"use client";

import { useCallback, useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "@/lib/session";
import { creditApi } from "@/lib/services";
import { describeActionError, useResource } from "@/lib/use-resource";
import { formatAmount, formatMonthlyRate } from "@/lib/format";
import { CREDIT_TIER_LABEL } from "@/lib/labels";
import {
  Alert,
  Button,
  Card,
  DetailRow,
  Field,
  Input,
  LoadError,
  Select,
  Skeleton,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { AmountField, parseAmount } from "@/components/money/amount-field";
import { NoWalletNotice, WalletPicker } from "@/components/money/wallet-picker";

/** Montant minimum imposé par `LoanApplicationRequest` (`@DecimalMin("1000")`). */
const MIN_LOAN = 1000;

/**
 * Demande de prêt.
 *
 * ═══ LE PLAFOND VIENT DE LA CAPACITÉ, PAS DU SCORE ═══
 *
 * C'est le point de cet écran. Le score fixe la confiance — donc le taux, et un
 * plafond absolu de palier. Le MONTANT, lui, est calculé sur les flux réels des
 * 90 derniers jours (`GET /credit/capacity`). Deux personnes à 80 points
 * n'obtiennent donc pas la même somme si l'une encaisse 20 000 XOF par mois et
 * l'autre 300 000.
 *
 * Borner le champ sur `maxLoanAmount` du score — comme le faisait cet écran —
 * laissait demander des montants que le backend refuse ensuite, et donnait à
 * l'utilisateur un chiffre qui n'était jamais le sien.
 *
 * ═══ LE MONTANT DÉPEND DE LA DURÉE ═══
 *
 * Le prêt se rembourse en UNE FOIS à l'échéance : plus la durée est longue,
 * plus la somme est accumulable, donc plus le plafond monte. Changer la durée
 * change donc le maximum — et le champ se réajuste sous les yeux de
 * l'utilisateur plutôt que de le laisser dépasser.
 *
 * Le simulateur applique exactement la formule du backend — `montant × (1 +
 * taux mensuel × durée)`, un intérêt simple (`LoanService`). Une estimation
 * calculée autrement afficherait un total différent de celui qui sera dû.
 */
export default function LoanApplicationPage() {
  const router = useRouter();
  const { activeWallet, reloadWallets } = useSession();

  const loadCapacity = useCallback(
    (signal: AbortSignal) => creditApi.capacity(signal),
    []
  );
  const capacity = useResource(loadCapacity);

  const [amount, setAmount] = useState("");
  const [duration, setDuration] = useState("3");
  const [purpose, setPurpose] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (!activeWallet) {
    return (
      <>
        <PageHeader title="Demander un prêt" backHref="/credit" backLabel="Crédit" />
        <NoWalletNotice />
      </>
    );
  }

  const data = capacity.data;
  const months = Number(duration);
  const maxAmount = data ? (data.maxAmountByDuration[String(months)] ?? 0) : 0;
  const value = parseAmount(amount, { min: MIN_LOAN, max: maxAmount });

  const totalRepayment =
    value !== null && data ? value * (1 + data.monthlyRate * months) : null;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (value === null) return;

    setError(null);
    setSubmitting(true);
    try {
      const loan = await creditApi.apply({
        walletId: activeWallet.id,
        requestedAmount: value,
        durationMonths: months,
        purpose: purpose.trim() || undefined,
      });
      /* Le décaissement crédite le wallet dans la même transaction : le solde
         doit être relu avant d'afficher l'écran suivant. */
      await reloadWallets();
      router.replace(`/credit/prets/${loan.id}`);
    } catch (caught) {
      setError(describeActionError(caught));
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Demander un prêt"
        description="Le montant dépend de vos entrées et sorties réelles."
        backHref="/credit"
        backLabel="Crédit"
      />

      {capacity.loading ? (
        <Card className="space-y-3">
          <Skeleton className="h-5 w-1/2" />
          <Skeleton className="h-12 w-full" />
        </Card>
      ) : capacity.error ? (
        <LoadError message={capacity.error} onRetry={capacity.reload} />
      ) : !data || data.tier === "INELIGIBLE" ? (
        <Alert tone="warning" title="Prêt indisponible">
          Votre score actuel ne permet pas encore d&apos;emprunter. Le détail des
          règles sur la page Crédit indique précisément sur quoi agir.
        </Alert>
      ) : maxAmount < MIN_LOAN ? (
        <Alert tone="warning" title="Capacité insuffisante">
          {data.limitingFactorLabel} ne permet pas encore un prêt d&apos;au moins{" "}
          {formatAmount(MIN_LOAN)}. Utilisez votre compte régulièrement — dépôts
          entrants et dépenses maîtrisées — puis revenez.
        </Alert>
      ) : (
        <div className="space-y-5">
          {/* ---- Ce que le calcul a retenu ---- */}
          <Card>
            <h2 className="font-display text-sm font-semibold tracking-wide text-ink-500 uppercase">
              Votre capacité de remboursement
            </h2>
            <p className="tabular mt-2 font-display text-3xl font-semibold text-ink-950">
              {formatAmount(maxAmount)}
            </p>
            <p className="mt-1 text-sm text-ink-500">
              sur {months} mois · {data.limitingFactorLabel}
            </p>

            {/* Les chiffres qui ont servi au calcul : l'utilisateur reconnaît
                les siens, et voit sur quoi agir. Un plafond sans explication se
                vit comme un refus arbitraire. */}
            <dl className="mt-5">
              <DetailRow label="Entrées mensuelles" value={formatAmount(data.monthlyInflow)} />
              <DetailRow label="Sorties mensuelles" value={formatAmount(data.monthlyOutflow)} />
              <DetailRow
                label="Disponible chaque mois"
                value={formatAmount(data.monthlyDisposable)}
              />
              <DetailRow
                label="Régularité des revenus"
                value={`${data.activeMonths} mois actif(s) sur 3`}
              />
              <DetailRow
                label="Palier de confiance"
                value={`${CREDIT_TIER_LABEL[data.tier]} · ${formatMonthlyRate(data.monthlyRate)}`}
              />
            </dl>

            {data.limitingFactor === "GRADUATION" ? (
              <Alert tone="info">
                Vos premiers prêts sont plafonnés à{" "}
                {formatAmount(data.graduationCeiling)}. Ce plafond double à chaque
                prêt remboursé à l&apos;heure.
              </Alert>
            ) : null}
          </Card>

          {/* ---- La demande ---- */}
          <Card>
            <form onSubmit={handleSubmit} className="space-y-5" noValidate>
              {error ? <Alert tone="danger">{error}</Alert> : null}

              <WalletPicker label="Compte à créditer" />

              <Field label="Durée de remboursement" htmlFor="duration">
                <Select
                  id="duration"
                  value={duration}
                  onChange={(event) => setDuration(event.target.value)}
                >
                  {/* 1 à 12 mois : bornes de `LoanApplicationRequest`. Le
                      maximum empruntable est affiché pour chaque durée, parce
                      que c'est précisément ce qui varie. */}
                  {Array.from({ length: 12 }, (_, index) => index + 1).map((month) => (
                    <option key={month} value={month}>
                      {month} mois — jusqu&apos;à{" "}
                      {formatAmount(data.maxAmountByDuration[String(month)] ?? 0)}
                    </option>
                  ))}
                </Select>
              </Field>

              <AmountField
                value={amount}
                onChange={setAmount}
                min={MIN_LOAN}
                max={maxAmount}
                label="Montant demandé"
                helper={`Entre ${formatAmount(MIN_LOAN)} et ${formatAmount(maxAmount)} sur ${months} mois.`}
              />

              <Field label="Motif" htmlFor="purpose" hint="Facultatif.">
                <Input
                  id="purpose"
                  value={purpose}
                  onChange={(event) => setPurpose(event.target.value)}
                  placeholder="Ex : stock de marchandise"
                  maxLength={140}
                />
              </Field>

              {totalRepayment !== null ? (
                <div className="rounded-field border border-line-strong p-4">
                  <p className="text-sm text-ink-500">À rembourser au total</p>
                  <p className="tabular mt-1 font-display text-2xl font-semibold text-ink-950">
                    {formatAmount(totalRepayment)}
                  </p>
                  <p className="mt-1 text-xs text-ink-500">
                    {formatAmount(value ?? 0)} empruntés +{" "}
                    {formatAmount(totalRepayment - (value ?? 0))} d&apos;intérêts sur{" "}
                    {months} mois, dus en une fois à l&apos;échéance.
                  </p>
                </div>
              ) : null}

              {/* Le message dépend du montant : au-delà du seuil d'examen, le
                  versement n'est PAS immédiat. Promettre l'inverse ferait
                  attendre un virement qui ne vient pas, et transformerait une
                  procédure normale en incident au support. */}
              {value !== null && value > data.manualReviewThreshold ? (
                <Alert tone="warning" title="Demande soumise à examen">
                  Au-delà de {formatAmount(data.manualReviewThreshold)}, votre
                  demande est examinée par notre équipe avant versement. Vous
                  serez notifié de la décision ; aucun montant n&apos;est versé
                  d&apos;ici là.
                </Alert>
              ) : (
                <Alert tone="info">
                  Jusqu&apos;à {formatAmount(data.manualReviewThreshold)}, le prêt
                  est accordé et versé sur votre compte immédiatement.
                </Alert>
              )}

              <Button
                type="submit"
                size="lg"
                full
                loading={submitting}
                disabled={value === null}
              >
                Demander {value !== null ? formatAmount(value) : "le prêt"}
              </Button>
            </form>
          </Card>
        </div>
      )}
    </>
  );
}
