"use client";

import Link from "next/link";
import { Suspense, useCallback, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useSession } from "@/lib/session";
import { beneficiaryApi, transactionApi } from "@/lib/services";
import { newIdempotencyKey } from "@/lib/api";
import { describeActionError, useResource } from "@/lib/use-resource";
import { formatAmount } from "@/lib/format";
import { NETWORK_COLOR, NETWORK_LABEL } from "@/lib/labels";
import { cn } from "@/lib/cn";
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  LoadError,
  SkeletonList,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { AmountField, parseAmount } from "@/components/money/amount-field";
import { NoWalletNotice, WalletPicker } from "@/components/money/wallet-picker";
import { OperationResult } from "@/components/money/operation-result";
import { UsersIcon } from "@/components/ui/icons";
import type { Beneficiary, Transaction } from "@/lib/types";

/**
 * Transfert vers un bénéficiaire enregistré.
 *
 * ON N'ENVOIE PAS À UN NUMÉRO SAISI À LA VOLÉE : le backend n'accepte qu'un
 * `beneficiaryId` (`TransferRequest`), et c'est une protection, pas une
 * limitation d'API. Un numéro doit d'abord être enregistré — donc relu, nommé,
 * confirmé — avant de pouvoir recevoir de l'argent. Le chiffre de trop se
 * corrige à l'ajout du bénéficiaire, pas au moment où la somme part.
 */
export default function TransferPage() {
  return (
    <Suspense fallback={<SkeletonList rows={3} />}>
      <TransferForm />
    </Suspense>
  );
}

function TransferForm() {
  const { activeWallet, reloadWallets } = useSession();
  const preselected = useSearchParams().get("beneficiaire");

  const loadBeneficiaries = useCallback(
    (signal: AbortSignal) => beneficiaryApi.list(signal),
    []
  );
  const beneficiaries = useResource(loadBeneficiaries);

  const [selectedId, setSelectedId] = useState<number | null>(
    preselected ? Number(preselected) : null
  );
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<Transaction | null>(null);

  if (!activeWallet) {
    return (
      <>
        <PageHeader title="Envoyer" backHref="/" backLabel="Accueil" />
        <NoWalletNotice />
      </>
    );
  }

  const available = activeWallet.availableBalance;
  const value = parseAmount(amount, { min: 1, max: available });
  const selected =
    beneficiaries.data?.find((item) => item.id === selectedId) ?? null;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (value === null || !selected) return;

    setError(null);
    setSubmitting(true);
    try {
      const transaction = await transactionApi.transfer({
        sourceWalletId: activeWallet.id,
        beneficiaryId: selected.id,
        amount: value,
        description: description.trim() || undefined,
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
    setDescription("");
    setIdempotencyKey(newIdempotencyKey());
  };

  return (
    <>
      <PageHeader
        title="Envoyer de l'argent"
        description={`Disponible : ${formatAmount(available)}`}
        backHref="/"
        backLabel="Accueil"
      />

      {result ? (
        <OperationResult
          transaction={result}
          onRestart={restart}
          restartLabel="Nouvel envoi"
        />
      ) : (
        <Card>
          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            {error ? <Alert tone="danger">{error}</Alert> : null}

            <WalletPicker label="Compte à débiter" />

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-ink-800">Destinataire</span>
                <Link
                  href="/beneficiaires"
                  className="text-sm font-semibold text-kola-600 hover:underline"
                >
                  Gérer
                </Link>
              </div>

              {beneficiaries.loading ? (
                <SkeletonList rows={2} />
              ) : beneficiaries.error ? (
                <LoadError message={beneficiaries.error} onRetry={beneficiaries.reload} />
              ) : beneficiaries.data && beneficiaries.data.length > 0 ? (
                <ul className="space-y-2">
                  {beneficiaries.data.map((beneficiary) => (
                    <li key={beneficiary.id}>
                      <BeneficiaryChoice
                        beneficiary={beneficiary}
                        checked={selectedId === beneficiary.id}
                        onSelect={() => setSelectedId(beneficiary.id)}
                      />
                    </li>
                  ))}
                </ul>
              ) : (
                <EmptyState
                  icon={<UsersIcon />}
                  title="Aucun bénéficiaire"
                  description="Enregistrez d'abord un destinataire : c'est ce qui permet de vérifier le numéro avant qu'un montant ne parte."
                  action={
                    <Link
                      href="/beneficiaires"
                      className="inline-flex h-11 items-center justify-center rounded-full bg-kola-600 px-5 text-sm font-semibold text-white hover:bg-kola-700"
                    >
                      Ajouter un bénéficiaire
                    </Link>
                  }
                />
              )}
            </div>

            <AmountField
              value={amount}
              onChange={setAmount}
              max={available}
              quickAmounts={[1000, 5000, 10_000].filter((quick) => quick <= available)}
            />

            <Field label="Motif" htmlFor="description" hint="Facultatif — visible sur le reçu.">
              <Input
                id="description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Ex : loyer, scolarité…"
                maxLength={140}
              />
            </Field>

            <Button
              type="submit"
              size="lg"
              full
              loading={submitting}
              disabled={value === null || !selected}
            >
              {selected && value !== null
                ? `Envoyer ${formatAmount(value)} à ${selected.alias}`
                : "Envoyer"}
            </Button>
          </form>
        </Card>
      )}
    </>
  );
}

/**
 * Choix d'un bénéficiaire.
 *
 * Un vrai `<input type="radio">` sous une carte cliquable : la sélection reste
 * navigable au clavier et annoncée comme un groupe de choix. Le style visuel ne
 * remplace pas la sémantique — il l'habille.
 */
function BeneficiaryChoice({
  beneficiary,
  checked,
  onSelect,
}: {
  beneficiary: Beneficiary;
  checked: boolean;
  onSelect: () => void;
}) {
  const color = NETWORK_COLOR[beneficiary.network];

  return (
    <label
      className={cn(
        "flex cursor-pointer items-center gap-3 rounded-field border p-3 transition-colors",
        checked
          ? "border-kola-500 bg-kola-50"
          : "border-line-strong bg-white hover:bg-ink-50"
      )}
    >
      <input
        type="radio"
        name="beneficiary"
        className="sr-only"
        checked={checked}
        onChange={onSelect}
      />
      <span
        className="flex size-10 shrink-0 items-center justify-center rounded-full text-sm font-bold"
        style={{
          backgroundColor: color ? `${color}26` : undefined,
          color: color ?? undefined,
        }}
      >
        {beneficiary.alias.slice(0, 2).toUpperCase()}
      </span>
      <span className="min-w-0 flex-1">
        <span className="block truncate text-sm font-semibold text-ink-900">
          {beneficiary.alias}
        </span>
        <span className="block truncate text-xs text-ink-500">
          {beneficiary.phoneNumber} · {NETWORK_LABEL[beneficiary.network]}
        </span>
      </span>
      <span
        className={cn(
          "size-4 shrink-0 rounded-full border-2",
          checked ? "border-kola-600 bg-kola-600 ring-2 ring-white ring-inset" : "border-ink-300"
        )}
      />
    </label>
  );
}
