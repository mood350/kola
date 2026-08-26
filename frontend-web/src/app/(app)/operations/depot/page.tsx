"use client";

import { useState } from "react";
import { useSession } from "@/lib/session";
import { transactionApi } from "@/lib/services";
import { newIdempotencyKey } from "@/lib/api";
import { describeActionError } from "@/lib/use-resource";
import { Alert, Button, Card, Field, Input } from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { AmountField, parseAmount } from "@/components/money/amount-field";
import { NoWalletNotice, WalletPicker } from "@/components/money/wallet-picker";
import { OperationResult } from "@/components/money/operation-result";
import type { Transaction } from "@/lib/types";

/**
 * Dépôt sur un wallet.
 *
 * LA CLÉ D'IDEMPOTENCE EST CRÉÉE AVEC L'INTENTION, pas à l'envoi. Elle est
 * posée dans l'état au montage et ne change qu'après un succès : si la réponse
 * se perd (réseau coupé au mauvais moment) et que l'utilisateur relance, le
 * backend reconnaît la même clé et ne crédite pas deux fois. Générer la clé
 * dans le gestionnaire d'envoi produirait deux clés pour deux tentatives — donc
 * deux dépôts.
 */
export default function DepositPage() {
  const { activeWallet, reloadWallets } = useSession();

  const [amount, setAmount] = useState("");
  const [reference, setReference] = useState("");
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<Transaction | null>(null);

  if (!activeWallet) {
    return (
      <>
        <PageHeader title="Déposer" backHref="/" backLabel="Accueil" />
        <NoWalletNotice />
      </>
    );
  }

  const value = parseAmount(amount, { min: 1 });

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (value === null) return;

    setError(null);
    setSubmitting(true);
    try {
      const transaction = await transactionApi.deposit({
        walletId: activeWallet.id,
        amount: value,
        externalReference: reference.trim() || undefined,
        idempotencyKey,
      });
      setResult(transaction);
      /* Le solde affiché partout ailleurs doit refléter ce dépôt immédiatement :
         sans ce rappel, l'accueil montrerait l'ancien montant. */
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
    /* Nouvelle intention, donc nouvelle clé : réutiliser la précédente ferait
       ignorer ce second dépôt par la déduplication du backend. */
    setIdempotencyKey(newIdempotencyKey());
  };

  return (
    <>
      <PageHeader
        title="Déposer de l'argent"
        description="Créditez votre compte Kola."
        backHref="/"
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

            <WalletPicker label="Compte à créditer" />

            <AmountField
              value={amount}
              onChange={setAmount}
              quickAmounts={[1000, 5000, 10_000, 25_000]}
            />

            <Field
              label="Référence externe"
              htmlFor="reference"
              hint="Facultatif — le numéro du reçu de votre agent ou de votre virement."
            >
              <Input
                id="reference"
                value={reference}
                onChange={(event) => setReference(event.target.value)}
                placeholder="Ex : MM-2026-0842"
              />
            </Field>

            <Button
              type="submit"
              size="lg"
              full
              loading={submitting}
              disabled={value === null}
            >
              Déposer
            </Button>
          </form>
        </Card>
      )}
    </>
  );
}
