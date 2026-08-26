"use client";

import { use, useCallback, useState } from "react";
import { useSession } from "@/lib/session";
import { creditApi } from "@/lib/services";
import { describeActionError, useResource } from "@/lib/use-resource";
import {
  formatAmount,
  formatCountdown,
  formatDate,
  formatMonthlyRate,
} from "@/lib/format";
import {
  CREDIT_TIER_LABEL,
  LOAN_STATUS_LABEL,
  LOAN_STATUS_TONE,
} from "@/lib/labels";
import {
  Alert,
  Badge,
  Button,
  Card,
  DetailRow,
  LoadError,
  Modal,
  Skeleton,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";

/**
 * Détail d'un prêt.
 *
 * LE REMBOURSEMENT EST INTÉGRAL ET EN UNE FOIS : le backend débite le wallet du
 * total dû et clôt le prêt (`LoanService.repay`) — il n'existe pas de
 * remboursement partiel. La confirmation annonce donc le montant exact qui va
 * quitter le compte, sans laisser croire à un échelonnement possible.
 *
 * Un prêt en DÉFAUT reste remboursable, et c'est délibéré côté backend :
 * n'autoriser que l'état « décaissé » rendrait une créance en défaut
 * littéralement impossible à régulariser.
 */
export default function LoanDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const loanId = Number(id);
  const { reloadWallets, activeWallet } = useSession();

  const load = useCallback(
    (signal: AbortSignal) => creditApi.loan(loanId, signal),
    [loanId]
  );
  const resource = useResource(load);
  const loan = resource.data;

  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const repay = async () => {
    setError(null);
    setSubmitting(true);
    try {
      await creditApi.repay(loanId);
      setConfirming(false);
      resource.reload();
      await reloadWallets();
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setSubmitting(false);
    }
  };

  const repayable = loan?.status === "DISBURSED" || loan?.status === "DEFAULTED";
  const shortfall =
    loan && activeWallet ? loan.totalRepayment - activeWallet.availableBalance : 0;

  return (
    <>
      <PageHeader title="Détail du prêt" backHref="/credit" backLabel="Crédit" />

      {resource.loading ? (
        <Card className="space-y-4">
          <Skeleton className="h-8 w-40" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-2/3" />
        </Card>
      ) : resource.error ? (
        <LoadError message={resource.error} onRetry={resource.reload} />
      ) : loan ? (
        <div className="space-y-5">
          {error ? <Alert tone="danger">{error}</Alert> : null}

          <Card>
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-sm text-ink-500">Montant emprunté</p>
                <p className="tabular mt-1 font-display text-3xl font-semibold text-ink-950">
                  {formatAmount(loan.requestedAmount)}
                </p>
              </div>
              <Badge tone={LOAN_STATUS_TONE[loan.status]}>
                {LOAN_STATUS_LABEL[loan.status]}
              </Badge>
            </div>

            <dl className="mt-5">
              <DetailRow label="À rembourser" value={formatAmount(loan.totalRepayment)} />
              <DetailRow label="Taux" value={formatMonthlyRate(loan.monthlyRate)} />
              <DetailRow label="Durée" value={`${loan.durationMonths} mois`} />
              {loan.dueDate ? (
                <DetailRow
                  label="Échéance"
                  value={`${formatDate(loan.dueDate)} (${formatCountdown(loan.dueDate)})`}
                />
              ) : null}
              {loan.purpose ? <DetailRow label="Motif" value={loan.purpose} /> : null}
              {/* Le score au moment de la demande est conservé par le backend :
                  c'est lui qui a déterminé le montant et le taux, et il explique
                  des conditions qui paraîtraient arbitraires des mois plus tard,
                  quand le score aura changé. */}
              <DetailRow
                label="Score à la demande"
                value={`${loan.creditScoreAtRequest}/100 · ${CREDIT_TIER_LABEL[loan.tierAtRequest]}`}
              />
            </dl>

            {loan.rejectionReason ? (
              <Alert tone="danger" title="Motif du refus">
                {loan.rejectionReason}
              </Alert>
            ) : null}
          </Card>

          {repayable ? (
            <>
              {loan.status === "DEFAULTED" ? (
                <Alert tone="danger" title="Prêt en défaut">
                  L&apos;échéance est dépassée. Le remboursement reste possible et
                  reste la seule façon de régulariser la situation.
                </Alert>
              ) : null}

              {shortfall > 0 ? (
                <Alert tone="warning">
                  Il manque {formatAmount(shortfall)} sur votre solde disponible
                  pour rembourser ce prêt en une fois.
                </Alert>
              ) : null}

              <Button size="lg" full onClick={() => setConfirming(true)}>
                Rembourser {formatAmount(loan.totalRepayment)}
              </Button>
            </>
          ) : loan.status === "REPAID" ? (
            <Alert tone="positive" title="Prêt remboursé">
              Ce prêt est soldé. Le remboursement est pris en compte dans le
              calcul de votre score de crédit.
            </Alert>
          ) : null}
        </div>
      ) : null}

      <Modal
        open={confirming}
        onClose={() => setConfirming(false)}
        title="Confirmer le remboursement"
      >
        <p className="text-sm text-ink-600">
          {loan ? formatAmount(loan.totalRepayment) : ""} seront débités de votre
          compte en une seule fois, et le prêt sera soldé.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setConfirming(false)}>
            Annuler
          </Button>
          <Button loading={submitting} onClick={() => void repay()}>
            Rembourser
          </Button>
        </div>
      </Modal>
    </>
  );
}
