"use client";

import { useCallback, useState } from "react";
import { useSession } from "@/lib/session";
import { merchantApi, transactionApi } from "@/lib/services";
import { newIdempotencyKey } from "@/lib/api";
import { describeActionError } from "@/lib/use-resource";
import { formatAmount } from "@/lib/format";
import { Alert, Button, Card, Field, Input, Spinner } from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { AmountField, parseAmount } from "@/components/money/amount-field";
import { NoWalletNotice, WalletPicker } from "@/components/money/wallet-picker";
import { OperationResult } from "@/components/money/operation-result";
import { MerchantScanner } from "@/components/money/merchant-scanner";
import { StoreIcon } from "@/components/ui/icons";
import type { Merchant, Transaction } from "@/lib/types";

/**
 * Paiement marchand.
 *
 * EN DEUX TEMPS, ET C'EST DÉLIBÉRÉ. Le code est d'abord résolu
 * (`GET /merchants/{code}`) pour afficher le NOM du marchand ; le champ montant
 * n'apparaît qu'ensuite. Payer un code brut reviendrait à envoyer de l'argent à
 * une chaîne de caractères — or c'est justement à ce moment qu'une faute de
 * frappe, ou un QR code substitué, doit se voir.
 *
 * La réponse du backend n'expose aucun solde marchand : juste de quoi
 * reconnaître qui l'on paie.
 */
export default function PayMerchantPage() {
  const { activeWallet, reloadWallets } = useSession();

  const [code, setCode] = useState("");
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [lookingUp, setLookingUp] = useState(false);
  const [amount, setAmount] = useState("");
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<Transaction | null>(null);

  const lookUp = useCallback(async (rawCode: string) => {
    const trimmed = rawCode.trim();
    if (!trimmed) return;

    setError(null);
    setLookingUp(true);
    try {
      setMerchant(await merchantApi.byCode(trimmed));
    } catch (caught) {
      setMerchant(null);
      setError(describeActionError(caught));
    } finally {
      setLookingUp(false);
    }
  }, []);

  if (!activeWallet) {
    return (
      <>
        <PageHeader title="Payer un marchand" backHref="/mon-compte" backLabel="Accueil" />
        <NoWalletNotice />
      </>
    );
  }

  const available = activeWallet.availableBalance;
  const value = parseAmount(amount, { min: 1, max: available });

  const handlePay = async (event: React.FormEvent) => {
    event.preventDefault();
    if (value === null || !merchant) return;

    setError(null);
    setSubmitting(true);
    try {
      const transaction = await transactionApi.payMerchant({
        sourceWalletId: activeWallet.id,
        merchantCode: merchant.merchantCode,
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
    setCode("");
    setMerchant(null);
    setIdempotencyKey(newIdempotencyKey());
  };

  return (
    <>
      <PageHeader
        title="Payer un marchand"
        description={`Disponible : ${formatAmount(available)}`}
        backHref="/mon-compte"
        backLabel="Accueil"
      />

      {result ? (
        <OperationResult
          transaction={result}
          onRestart={restart}
          restartLabel="Nouveau paiement"
        />
      ) : (
        <Card>
          <div className="space-y-5">
            {error ? <Alert tone="danger">{error}</Alert> : null}

            <WalletPicker label="Compte à débiter" />

            {merchant ? (
              <>
                <div className="flex items-center gap-3 rounded-field bg-positive-50 p-4">
                  <span className="flex size-11 items-center justify-center rounded-full bg-white text-xl text-positive-600">
                    <StoreIcon />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-semibold text-ink-900">{merchant.name}</p>
                    <p className="truncate text-xs text-ink-500">
                      {merchant.category ? `${merchant.category} · ` : ""}
                      {merchant.merchantCode}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setMerchant(null);
                      setAmount("");
                    }}
                    className="shrink-0 text-sm font-semibold text-kola-600 hover:underline"
                  >
                    Changer
                  </button>
                </div>

                <form onSubmit={handlePay} className="space-y-5" noValidate>
                  <AmountField
                    value={amount}
                    onChange={setAmount}
                    max={available}
                    label="Montant à payer"
                  />
                  <Button
                    type="submit"
                    size="lg"
                    full
                    loading={submitting}
                    disabled={value === null}
                  >
                    {value !== null
                      ? `Payer ${formatAmount(value)} à ${merchant.name}`
                      : "Payer"}
                  </Button>
                </form>
              </>
            ) : (
              <form
                onSubmit={(event) => {
                  event.preventDefault();
                  void lookUp(code);
                }}
                className="space-y-4"
                noValidate
              >
                <Field
                  label="Code du marchand"
                  htmlFor="merchant-code"
                  hint="Affiché sur le comptoir ou dans le QR code du commerçant."
                >
                  <Input
                    id="merchant-code"
                    value={code}
                    onChange={(event) => setCode(event.target.value.toUpperCase())}
                    placeholder="Ex : KOLA-M-00421"
                    autoComplete="off"
                  />
                </Field>

                <Button type="submit" size="lg" full disabled={!code.trim() || lookingUp}>
                  {lookingUp ? <Spinner className="size-4" /> : null}
                  Vérifier le marchand
                </Button>

                {/* Le scan n'apparaît que si le navigateur sait décoder un QR
                    code nativement — cf. `MerchantScanner`. */}
                <MerchantScanner
                  onDetected={(scanned) => {
                    setCode(scanned);
                    void lookUp(scanned);
                  }}
                />
              </form>
            )}
          </div>
        </Card>
      )}
    </>
  );
}
