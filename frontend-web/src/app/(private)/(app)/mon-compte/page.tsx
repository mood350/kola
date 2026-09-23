"use client";

import Link from "next/link";
import { useCallback } from "react";
import { useSession } from "@/lib/session";
import { useResource } from "@/lib/use-resource";
import { creditApi, transactionApi, vaultApi } from "@/lib/services";
import { formatAmount } from "@/lib/format";
import { CREDIT_TIER_LABEL, CREDIT_TIER_TONE } from "@/lib/labels";
import {
  Badge,
  Card,
  EmptyState,
  LoadError,
  SectionHeading,
  SkeletonList,
} from "@/components/ui/primitives";
import { NoWalletNotice, WalletBalanceCard, WalletPicker } from "@/components/money/wallet-picker";
import { TransactionRow } from "@/components/transactions/transaction-row";
import {
  ArrowDownIcon,
  ArrowUpIcon,
  ChevronRightIcon,
  CreditIcon,
  ListIcon,
  SendIcon,
  StoreIcon,
  VaultIcon,
} from "@/components/ui/icons";

/**
 * Accueil.
 *
 * L'ordre des blocs répond à la question qu'on se pose en ouvrant une
 * application d'argent, dans l'ordre où on se la pose : combien me reste-t-il,
 * que puis-je faire, et que s'est-il passé depuis la dernière fois. Les mêmes
 * quatre actions rapides que sur mobile — déposer, envoyer, payer, retirer —
 * dans le même ordre : la mémoire gestuelle d'un utilisateur qui passe du
 * téléphone au navigateur doit rester valable.
 */

const QUICK_ACTIONS = [
  { href: "/operations/depot", label: "Déposer", icon: ArrowDownIcon },
  { href: "/operations/envoi", label: "Envoyer", icon: SendIcon },
  { href: "/operations/paiement", label: "Payer", icon: StoreIcon },
  { href: "/operations/retrait", label: "Retirer", icon: ArrowUpIcon },
];

export default function HomePage() {
  const { user, activeWallet, wallets } = useSession();
  const walletId = activeWallet?.id ?? null;

  const loadRecent = useCallback(
    (signal: AbortSignal) =>
      walletId === null
        ? Promise.resolve(null)
        : transactionApi.history(walletId, 0, 5, signal),
    [walletId]
  );
  const recent = useResource(loadRecent);

  const loadVaults = useCallback(
    (signal: AbortSignal) => vaultApi.list(signal),
    []
  );
  const vaults = useResource(loadVaults);

  /* Le score est chargé ici comme ailleurs : le backend le sert depuis son
     cache de 30 jours et ne le recalcule que s'il a expiré
     (`CreditScoringService.getOrCompute`). L'afficher sur l'accueil ne coûte
     donc pas un recalcul à chaque visite. */
  const loadScore = useCallback((signal: AbortSignal) => creditApi.score(signal), []);
  const score = useResource(loadScore);

  const savedTotal = (vaults.data ?? [])
    .filter((vault) => vault.status === "ACTIVE")
    .reduce((total, vault) => total + vault.currentAmount, 0);

  return (
    <div className="space-y-8">
      <header>
        <p className="text-sm text-ink-500">Bonjour,</p>
        <h1 className="font-display text-2xl font-semibold tracking-tight text-ink-950">
          {user?.firstName ?? "—"}
        </h1>
      </header>

      {/* ---- Solde ---- */}
      {activeWallet ? (
        <section className="space-y-3">
          <WalletBalanceCard wallet={activeWallet} />
          {wallets.length > 1 ? <WalletPicker label="Compte affiché" /> : null}
        </section>
      ) : (
        <NoWalletNotice />
      )}

      {/* ---- Actions rapides ---- */}
      {activeWallet ? (
        <section>
          <h2 className="sr-only">Actions rapides</h2>
          <div className="grid grid-cols-4 gap-2 sm:gap-3">
            {QUICK_ACTIONS.map((action) => (
              <Link
                key={action.href}
                href={action.href}
                className="flex flex-col items-center gap-2 rounded-surface bg-surface px-2 py-4 text-center ring-1 ring-line transition-colors hover:bg-kola-50"
              >
                <span className="flex size-11 items-center justify-center rounded-full bg-kola-50 text-xl text-kola-600">
                  <action.icon />
                </span>
                <span className="text-xs font-semibold text-ink-800 sm:text-sm">
                  {action.label}
                </span>
              </Link>
            ))}
          </div>
        </section>
      ) : null}

      {/* ---- Épargne et crédit ---- */}
      <section className="grid gap-3 sm:grid-cols-2">
        <Link href="/coffres" className="group">
          <Card className="h-full transition-colors group-hover:bg-ink-50">
            <div className="flex items-center justify-between">
              <span className="flex items-center gap-2 text-sm font-medium text-ink-600">
                <VaultIcon className="text-lg text-kola-600" />
                Épargne bloquée
              </span>
              <ChevronRightIcon className="text-ink-400" />
            </div>
            <p className="tabular mt-2 font-display text-2xl font-semibold text-ink-950">
              {vaults.loading ? "…" : formatAmount(savedTotal)}
            </p>
            <p className="mt-1 text-xs text-ink-500">
              {vaults.data
                ? `${vaults.data.filter((vault) => vault.status === "ACTIVE").length} coffre(s) actif(s)`
                : "Coffres d'épargne"}
            </p>
          </Card>
        </Link>

        <Link href="/credit" className="group">
          <Card className="h-full transition-colors group-hover:bg-ink-50">
            <div className="flex items-center justify-between">
              <span className="flex items-center gap-2 text-sm font-medium text-ink-600">
                <CreditIcon className="text-lg text-kola-600" />
                Score de crédit
              </span>
              <ChevronRightIcon className="text-ink-400" />
            </div>
            <p className="tabular mt-2 font-display text-2xl font-semibold text-ink-950">
              {score.loading ? "…" : score.data ? `${score.data.totalScore} / 100` : "—"}
            </p>
            <p className="mt-1 text-xs">
              {score.data ? (
                <Badge tone={CREDIT_TIER_TONE[score.data.tier]}>
                  {CREDIT_TIER_LABEL[score.data.tier]}
                </Badge>
              ) : (
                <span className="text-ink-500">Non calculé</span>
              )}
            </p>
          </Card>
        </Link>
      </section>

      {/* ---- Dernières opérations ---- */}
      {activeWallet ? (
        <section>
          <SectionHeading
            title="Récent"
            action={
              <Link
                href="/transactions"
                className="text-sm font-semibold text-kola-600 hover:underline"
              >
                Tout voir
              </Link>
            }
          />

          {recent.loading ? (
            <SkeletonList rows={3} />
          ) : recent.error ? (
            <LoadError message={recent.error} onRetry={recent.reload} />
          ) : recent.data && recent.data.content.length > 0 ? (
            <div className="space-y-2">
              {recent.data.content.map((transaction) => (
                <TransactionRow key={transaction.id} transaction={transaction} />
              ))}
            </div>
          ) : (
            <EmptyState
              icon={<ListIcon />}
              title="Aucune opération"
              description="Vos dépôts, transferts et paiements apparaîtront ici."
            />
          )}
        </section>
      ) : null}
    </div>
  );
}
