"use client";

import { useCallback, useState } from "react";
import { useSession } from "@/lib/session";
import { paymentApi, transactionApi } from "@/lib/services";
import { newIdempotencyKey } from "@/lib/api";
import { describeActionError, useResource } from "@/lib/use-resource";
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
 * Dépôt : par Mobile Money, ou en mode test.
 *
 * ═══ DEUX CHEMINS QUI N'ONT RIEN À VOIR ═══
 *
 * MOBILE MONEY appelle vraiment l'opérateur. Où le client valide dépend de ce
 * que le compte marchand autorise : une demande de débit sur son téléphone
 * (prélèvement direct), ou une page de paiement chez le prestataire — c'est
 * `directCharge` qui le dit, et l'écran l'annonce avant la saisie. Dans les
 * deux cas l'écriture naît EN ATTENTE et le portefeuille n'est crédité qu'à la
 * confirmation du prestataire, par webhook.
 *
 * TEST crédite immédiatement, sans qu'aucun argent n'existe. C'est le
 * comportement historique de `POST /transactions/deposit`, conservé pour
 * développer et démontrer sans compte marchand. Il est signalé comme tel à
 * l'écran : un mode qui fabrique de l'argent ne doit jamais pouvoir être pris
 * pour le mode normal.
 *
 * ⚠️ Cette route de test doit disparaître — ou être réservée aux
 * administrateurs — avant toute mise en production : en l'état, n'importe quel
 * utilisateur peut se créditer ce qu'il veut.
 *
 * ═══ LA CLÉ D'IDEMPOTENCE ═══
 *
 * Créée avec l'intention, pas à l'envoi, et renouvelée seulement après un
 * succès ou un changement d'intention. Si la réponse se perd et que
 * l'utilisateur relance, le backend reconnaît la même clé et ne crédite pas
 * deux fois.
 */
export default function DepositPage() {
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
  const [reference, setReference] = useState("");
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<Transaction | null>(null);

  if (!activeWallet) {
    return (
      <>
        <PageHeader title="Déposer" backHref="/mon-compte" backLabel="Accueil" />
        <NoWalletNotice />
      </>
    );
  }

  const value = parseAmount(amount, { min: 1 });
  const phoneOk = isValidPhone(phone);
  const canSubmit =
    value !== null && (mode === "test" || (operator !== null && phoneOk));

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!canSubmit || value === null) return;

    setError(null);
    setSubmitting(true);
    try {
      const transaction =
        mode === "mobile"
          ? await transactionApi.depositByMobileMoney({
              walletId: activeWallet.id,
              amount: value,
              mode: operator!,
              phoneNumber: normalizePhone(phone),
              idempotencyKey,
            })
          : await transactionApi.deposit({
              walletId: activeWallet.id,
              amount: value,
              externalReference: reference.trim() || undefined,
              idempotencyKey,
            });

      setResult(transaction);
      /* Le solde affiché partout ailleurs doit suivre : en mode test il a
         changé tout de suite, en Mobile Money il changera au webhook — dans les
         deux cas, relire est la seule façon d'être juste. */
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
    setReference("");
    /* Nouvelle intention, nouvelle clé : réutiliser la précédente ferait
       ignorer ce second dépôt par la déduplication du backend. */
    setIdempotencyKey(newIdempotencyKey());
  };

  const available = methods.data?.available ?? [];
  const providerEnabled = methods.data?.providerEnabled ?? false;
  const directCharge = methods.data?.directCharge ?? false;

  return (
    <>
      <PageHeader
        title="Déposer de l'argent"
        description="Rechargez votre compte Kola depuis votre Mobile Money."
        backHref="/mon-compte"
        backLabel="Accueil"
      />

      {result ? (
        <OperationResult
          transaction={result}
          onRestart={restart}
          restartLabel="Nouveau dépôt"
        />
      ) : (
        <Card>
          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            {error ? <Alert tone="danger">{error}</Alert> : null}

            <ModeSwitch
              value={mode}
              onChange={setMode}
              realLabel="Mobile Money"
              realHint="Débit réel sur votre compte opérateur"
              testLabel="Test"
              testHint="Crédit fictif, sans argent réel"
            />

            <WalletPicker label="Compte à créditer" />

            <AmountField
              value={amount}
              onChange={setAmount}
              quickAmounts={[1000, 5000, 10_000, 25_000]}
            />

            {mode === "mobile" ? (
              <>
                {methods.loading ? (
                  <SkeletonList rows={1} />
                ) : (
                  <>
                    {!providerEnabled ? (
                      <Alert tone="warning" title="Paiement réel indisponible">
                        Le prestataire de paiement n&apos;est pas configuré sur ce
                        serveur. Utilisez le mode test, ou renseignez les clés
                        FedaPay côté backend.
                      </Alert>
                    ) : null}

                    <OperatorPicker
                      methods={available}
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
                      hint={
                        directCharge
                          ? "Le numéro qui sera débité. Vous validerez la demande sur votre téléphone."
                          : "Le numéro qui sera débité. Vous terminerez le paiement sur la page sécurisée du prestataire."
                      }
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
                  </>
                )}
              </>
            ) : (
              <>
                <Alert tone="info">
                  Aucun argent réel n&apos;est déplacé : le compte est crédité
                  directement. Réservé au développement et aux démonstrations.
                </Alert>

                <Field
                  label="Référence externe"
                  htmlFor="reference"
                  hint="Facultatif — le numéro du reçu de votre agent."
                >
                  <Input
                    id="reference"
                    value={reference}
                    onChange={(event) => setReference(event.target.value)}
                    placeholder="Ex : MM-2026-0842"
                  />
                </Field>
              </>
            )}

            <Button
              type="submit"
              size="lg"
              full
              loading={submitting}
              disabled={!canSubmit || (mode === "mobile" && !providerEnabled)}
            >
              {mode === "mobile" ? "Envoyer la demande" : "Créditer (test)"}
            </Button>
          </form>
        </Card>
      )}
    </>
  );
}
