"use client";

import { useState } from "react";
import { useSession } from "@/lib/session";
import { transactionApi } from "@/lib/services";
import { newIdempotencyKey } from "@/lib/api";
import { describeActionError } from "@/lib/use-resource";
import { formatAmount } from "@/lib/format";
import { Alert, Button, Card } from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { AmountField, parseAmount } from "@/components/money/amount-field";
import { NoWalletNotice, WalletPicker } from "@/components/money/wallet-picker";
import { OperationResult } from "@/components/money/operation-result";
import type { Transaction } from "@/lib/types";

/**
 * Retrait depuis un wallet.
 *
 * LE PLAFOND EST LE SOLDE **DISPONIBLE**, pas le solde total. L'écart entre les
 * deux est l'argent bloqué en coffres : proposer de retirer le total ferait
 * échouer l'opération côté serveur (`InsufficientFundsException`) avec un
 * message qui n'expliquerait pas d'où vient la différence. Ici, le champ refuse
 * le montant avant l'envoi et dit pourquoi.
 */
export default function WithdrawPage() {
  const { activeWallet, reloadWallets } = useSession();

  const [amount, setAmount] = useState("");
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<Transaction | null>(null);

  if (!activeWallet) {
    return (
      <>
        <PageHeader title="Retirer" backHref="/" backLabel="Accueil" />
        <NoWalletNotice />
      </>
    );
  }

  const available = activeWallet.availableBalance;
  const value = parseAmount(amount, { min: 1, max: available });

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (value === null) return;

    setError(null);
    setSubmitting(true);
    try {
      const transaction = await transactionApi.withdraw({
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
        backHref="/"
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

            <WalletPicker label="Compte à débiter" />

            <AmountField
              value={amount}
              onChange={setAmount}
              max={available}
              quickAmounts={[1000, 5000, 10_000].filter((quick) => quick <= available)}
              helper={`Vous pouvez retirer jusqu'à ${formatAmount(available)}.`}
            />

            {activeWallet.lockedBalance > 0 ? (
              <Alert tone="info">
                {formatAmount(activeWallet.lockedBalance)} sont bloqués dans vos
                coffres d&apos;épargne et ne sont pas retirables tant qu&apos;ils
                n&apos;ont pas été débloqués.
              </Alert>
            ) : null}

            <Button
              type="submit"
              size="lg"
              full
              loading={submitting}
              disabled={value === null}
            >
              Retirer
            </Button>
          </form>
        </Card>
      )}
    </>
  );
}
