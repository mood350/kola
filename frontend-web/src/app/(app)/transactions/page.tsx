"use client";

import { useCallback, useState } from "react";
import { useSession } from "@/lib/session";
import { transactionApi } from "@/lib/services";
import { useResource } from "@/lib/use-resource";
import { formatCount } from "@/lib/format";
import {
  Button,
  EmptyState,
  LoadError,
  SkeletonList,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { NoWalletNotice, WalletPicker } from "@/components/money/wallet-picker";
import { TransactionRow } from "@/components/transactions/transaction-row";
import { ListIcon } from "@/components/ui/icons";

const PAGE_SIZE = 20;

/**
 * Historique des transactions d'un wallet.
 *
 * PAGINATION PAR PAGES, PAS PAR ACCUMULATION. Un bouton « charger plus » qui
 * empile les résultats rend impossible de revenir en arrière autrement qu'en
 * faisant défiler, et le nombre total d'opérations disparaît. Ici, chaque page
 * remplace la précédente, le rang est affiché, et `useResource` garde la page
 * courante à l'écran pendant le chargement de la suivante — la liste ne
 * clignote donc pas à chaque navigation.
 */
export default function TransactionsPage() {
  const { activeWallet } = useSession();
  const walletId = activeWallet?.id ?? null;
  const [page, setPage] = useState(0);

  const load = useCallback(
    (signal: AbortSignal) =>
      walletId === null
        ? Promise.resolve(null)
        : transactionApi.history(walletId, page, PAGE_SIZE, signal),
    [walletId, page]
  );
  const history = useResource(load);

  if (!activeWallet) {
    return (
      <>
        <PageHeader title="Transactions" />
        <NoWalletNotice />
      </>
    );
  }

  const data = history.data;

  return (
    <>
      <PageHeader
        title="Transactions"
        description={
          data
            ? `${formatCount(data.totalElements)} opération(s) sur ce compte`
            : undefined
        }
      />

      <div className="mb-4">
        {/* Changer de compte remet à la première page : rester en page 4 sur un
            compte qui n'en a qu'une afficherait une liste vide sans explication. */}
        <WalletPicker label="Compte" onWalletChange={() => setPage(0)} />
      </div>

      {history.loading && !data ? (
        <SkeletonList rows={6} />
      ) : history.error ? (
        <LoadError message={history.error} onRetry={history.reload} />
      ) : data && data.content.length > 0 ? (
        <>
          <div className="space-y-2" aria-busy={history.loading}>
            {data.content.map((transaction) => (
              <TransactionRow key={transaction.id} transaction={transaction} />
            ))}
          </div>

          {data.totalPages > 1 ? (
            <nav
              className="mt-6 flex items-center justify-between gap-3"
              aria-label="Pagination"
            >
              <Button
                variant="secondary"
                size="sm"
                disabled={data.number === 0 || history.loading}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                Précédent
              </Button>
              <span className="text-sm text-ink-500">
                Page {data.number + 1} sur {data.totalPages}
              </span>
              <Button
                variant="secondary"
                size="sm"
                disabled={data.last || history.loading}
                onClick={() => setPage((current) => current + 1)}
              >
                Suivant
              </Button>
            </nav>
          ) : null}
        </>
      ) : (
        <EmptyState
          icon={<ListIcon />}
          title="Aucune opération"
          description="Dès votre premier dépôt ou transfert, l'historique s'affichera ici."
        />
      )}
    </>
  );
}
