"use client";

import { useCallback, useState } from "react";
import { useSession } from "@/lib/session";
import { paymentApi, transactionApi } from "@/lib/services";
import { newIdempotencyKey } from "@/lib/api";
import { describeActionError, useResource } from "@/lib/use-resource";
import { formatAmount } from "@/lib/format";
import { Alert, Button, Card, Field, Input, SkeletonList } from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { AmountField, parseAmount } from "@/components/money/amount-field";
import { NoWalletNotice, WalletPicker } from "@/components/money/wallet-picker";
import { OperationResult } from "@/components/money/operation-result";
import { OperatorPicker } from "@/components/money/operator-picker";
import { ModeSwitch } from "@/components/money/mode-switch";
import { isValidPhone, normalizePhone } from "@/lib/countries";
import type { Transaction } from "@/lib/types";

/**
 * Retrait : vers Mobile Money, ou en mode test.
 *
 * ═══ LE DÉBIT EST IMMÉDIAT DANS LES DEUX MODES ═══
 *
 * Et c'est l'inverse du dépôt. Un dépôt en attente ne crédite rien : au pire on
 * attend son argent. Un retrait qui ne débiterait pas tout de suite laisserait
 * la somme dépensable pendant tout le traitement — le temps de l'envoyer une
 * seconde fois ailleurs. Le portefeuille afficherait alors un solde qui
 * n'existe plus.
 *
 * En Mobile Money, l'écriture reste donc EN ATTENTE, débitée, jusqu'à ce que
 * l'opérateur confirme. Un échec recrédite le montant ET les frais.
 *
 * ⚠️ Le mode test débite sans rien envoyer nulle part : il est réservé au
 * développement et doit disparaître avant la production, comme son homologue
 * du dépôt.
 */
export default function WithdrawPage() {
  const { activeWallet, reloadWallets } = useSession();

  const loadMethods = useCallback(
    (signal: AbortSignal) => paymentApi.methods(undefined, signal),
    []
  );
  const methods = useResource(loadMethods);

  const [mode, setMode] = useState<"mobile" | "test">("mobile");
  const [amount, setAmount] = useState("");
  const [operator, setOperator] = useState<string | null>(null);
  const [phone, setPhone] = useState("");
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<Transaction | null>(null);

  if (!activeWallet) {
    return (
      <>
        <PageHeader title="Retirer" backHref="/mon-compte" backLabel="Accueil" />
        <NoWalletNotice />
      </>
    );
  }

  const available = activeWallet.availableBalance;
  const value = parseAmount(amount, { min: 1, max: available });
  const phoneOk = isValidPhone(phone);
  const canSubmit =
    value !== null && (mode === "test" || (operator !== null && phoneOk));

  const operators = methods.data?.available ?? [];
  /* Deux verrous distincts, et les confondre trompe : `providerEnabled` dit que
     le prestataire est configuré, `withdrawalEnabled` que les VERSEMENTS sont
     ouverts sur le compte marchand. Encaisser sans pouvoir verser est un état
     normal chez FedaPay, pas une panne. */
  const providerEnabled =
    (methods.data?.providerEnabled ?? false) &&
    (methods.data?.withdrawalEnabled ?? false);
  const withdrawalClosed =
    (methods.data?.providerEnabled ?? false) &&
    !(methods.data?.withdrawalEnabled ?? false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!canSubmit || value === null) return;

    setError(null);
    setSubmitting(true);
    try {
      const transaction =
        mode === "mobile"
          ? await transactionApi.withdrawByMobileMoney({
              walletId: activeWallet.id,
              amount: value,
              mode: operator!,
              phoneNumber: normalizePhone(phone),
              idempotencyKey,
            })
          : await transactionApi.withdraw({
              walletId: activeWallet.id,
              amount: value,
              idempotencyKey,
            });

      setResult(transaction);
      await reloadWallets();
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setSubmitting(false);
    }
  };

  const restart = () => {
    setResult(null);
    setAmount("");
    setIdempotencyKey(newIdempotencyKey());
  };

  return (
    <>
      <PageHeader
        title="Retirer de l'argent"
        description={`Disponible : ${formatAmount(available)}`}
        backHref="/mon-compte"
        backLabel="Accueil"
      />

      {result ? (
        <OperationResult
          transaction={result}
          onRestart={restart}
          restartLabel="Nouveau retrait"
        />
      ) : (
        <Card>
          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            {error ? <Alert tone="danger">{error}</Alert> : null}

            <ModeSwitch
              value={mode}
              onChange={setMode}
              realLabel="Mobile Money"
              realHint="Versement réel sur votre compte opérateur"
              testLabel="Test"
              testHint="Débit fictif, aucun versement"
            />

            <WalletPicker label="Compte à débiter" />

            <AmountField
              value={amount}
              onChange={setAmount}
              max={available}
              quickAmounts={[1000, 5000, 10_000].filter((quick) => quick <= available)}
              helper={`Vous pouvez retirer jusqu'à ${formatAmount(available)}.`}
            />

            {mode === "mobile" ? (
              methods.loading ? (
                <SkeletonList rows={1} />
              ) : (
                <>
                  {withdrawalClosed ? (
                    <Alert tone="warning" title="Retrait Mobile Money indisponible">
                      Les versements ne sont pas encore ouverts sur ce compte
                      marchand. Vos dépôts et vos virements fonctionnent
                      normalement ; le retrait sera disponible dès l&apos;accord
                      du prestataire.
                    </Alert>
                  ) : !providerEnabled ? (
                    <Alert tone="warning" title="Versement réel indisponible">
                      Le prestataire de paiement n&apos;est pas configuré sur ce
                      serveur. Utilisez le mode test, ou renseignez les clés
                      FedaPay côté backend.
                    </Alert>
                  ) : null}

                  <OperatorPicker
                    methods={operators}
                    selected={operator}
                    onSelect={setOperator}
                    disabled={!providerEnabled}
                  />

                  <Field
                    label="Numéro Mobile Money"
                    htmlFor="phone"
                    error={
                      phone !== "" && !phoneOk
                        ? "Numéro attendu au format international, ex : +22890000000"
                        : null
                    }
                    hint="Le numéro qui recevra l'argent. Vérifiez-le : un versement parti ne se rappelle pas."
                  >
                    <Input
                      id="phone"
                      type="tel"
                      inputMode="tel"
                      value={phone}
                      invalid={phone !== "" && !phoneOk}
                      onChange={(event) => setPhone(event.target.value)}
                      placeholder="+22890000000"
                      disabled={!providerEnabled}
                    />
                  </Field>

                  <Alert tone="info">
                    Le montant quitte votre compte dès l&apos;envoi. Si le
                    versement échoue, le montant et les frais vous sont
                    recrédités automatiquement.
                  </Alert>
                </>
              )
            ) : (
              <Alert tone="info">
                Aucun argent n&apos;est envoyé : le compte est simplement débité.
                Réservé au développement et aux démonstrations.
              </Alert>
            )}

            {activeWallet.lockedBalance > 0 ? (
              <Alert tone="info">
                {formatAmount(activeWallet.lockedBalance)} sont bloqués dans vos
                coffres et ne sont pas retirables.
              </Alert>
            ) : null}

            <Button
              type="submit"
              size="lg"
              full
              loading={submitting}
              disabled={!canSubmit || (mode === "mobile" && !providerEnabled)}
            >
              {mode === "mobile" ? "Retirer" : "Débiter (test)"}
            </Button>
          </form>
        </Card>
      )}
    </>
  );
}
