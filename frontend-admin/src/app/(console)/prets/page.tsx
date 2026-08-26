"use client";

import { useCallback, useState } from "react";
import Link from "next/link";
import { apiFetch, queryString } from "@/lib/api";
import { useResource } from "@/lib/use-resource";
import {
  Card,
  EmptyBlock,
  ErrorBlock,
  LoadingBlock,
  PageHeading,
  Pagination,
  TableScroll,
  Td,
  Th,
} from "@/components/kola/shell";
import { LoanStatusBadge, TierBadge } from "@/components/kola/status";
import Label from "@/components/form/Label";
import { formatAmount, formatDate, formatMonthlyRate } from "@/lib/format";
import {
  LOAN_STATUSES,
  LOAN_STATUS_LABELS,
  type AdminLoanSummary,
  type Page as PageResult,
} from "@/lib/types";

const CONTROL =
  "h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90";

/**
 * Portefeuille de prêts.
 *
 * ÉCRAN EN LECTURE SEULE, et c'est délibéré côté serveur : le cycle de vie d'un
 * prêt est piloté par `LoanService` — le déboursement crédite un portefeuille,
 * le remboursement le débite et met le score à jour, la mise en défaut est
 * prononcée par un batch sur la date d'échéance. Un bouton « passer en
 * remboursé » écrirait un statut sans qu'aucun franc n'ait bougé.
 */
export default function LoansPage() {
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);

  const load = useCallback(
    (signal: AbortSignal) =>
      apiFetch<PageResult<AdminLoanSummary>>(
        `/admin/loans${queryString({ status, page, size: 20 })}`,
        { signal }
      ),
    [status, page]
  );

  const loans = useResource(load);

  return (
    <>
      <PageHeading
        title="Prêts"
        description="Toutes les demandes de crédit, du dépôt du dossier au remboursement."
      />

      <Card>
        <div className="border-b border-gray-100 p-5 dark:border-gray-800">
          <div className="max-w-xs">
            <Label htmlFor="statut">Statut</Label>
            <select
              id="statut"
              className={CONTROL}
              value={status}
              onChange={(event) => {
                setStatus(event.target.value);
                setPage(0);
              }}
            >
              <option value="">Tous les statuts</option>
              {LOAN_STATUSES.map((option) => (
                <option key={option} value={option}>
                  {LOAN_STATUS_LABELS[option]}
                </option>
              ))}
            </select>
            <p className="mt-1.5 text-theme-xs text-gray-500 dark:text-gray-400">
              « En défaut » regroupe les échéances dépassées, prononcées par le
              batch quotidien.
            </p>
          </div>
        </div>

        {loans.error ? (
          <ErrorBlock message={loans.error} onRetry={loans.reload} />
        ) : null}

        {loans.loading && !loans.data ? (
          <LoadingBlock label="Chargement des prêts…" />
        ) : null}

        {loans.data ? (
          loans.data.content.length === 0 ? (
            <EmptyBlock
              title="Aucun prêt"
              hint={
                status
                  ? "Aucun dossier dans ce statut."
                  : "Aucune demande de crédit n'a encore été déposée."
              }
            />
          ) : (
            <>
              <div className={loans.loading ? "opacity-60 transition-opacity" : undefined}>
                <TableScroll>
                  <table className="w-full">
                    <thead>
                      <tr>
                        <Th>Emprunteur</Th>
                        <Th align="right">Montant</Th>
                        <Th align="right">À rembourser</Th>
                        <Th align="right">Taux</Th>
                        <Th>Score au dépôt</Th>
                        <Th align="right">Échéance</Th>
                        <Th>Statut</Th>
                      </tr>
                    </thead>
                    <tbody>
                      {loans.data.content.map((loan) => (
                        <tr
                          key={loan.id}
                          className="transition-colors hover:bg-gray-50 dark:hover:bg-white/[0.02]"
                        >
                          <Td>
                            <Link
                              href={`/prets/${loan.id}`}
                              className="font-medium text-gray-800 hover:text-brand-500 dark:text-white/90"
                            >
                              {loan.borrowerFullName ?? "Emprunteur inconnu"}
                            </Link>
                            <span className="block text-theme-xs text-gray-500 dark:text-gray-400">
                              {loan.borrowerEmail}
                            </span>
                          </Td>
                          <Td align="right">{formatAmount(loan.requestedAmount)}</Td>
                          <Td align="right">
                            {formatAmount(loan.totalRepayment)}
                            <span className="block text-theme-xs text-gray-500">
                              sur {loan.durationMonths} mois
                            </span>
                          </Td>
                          <Td align="right">{formatMonthlyRate(loan.monthlyRate)}</Td>
                          <Td>
                            <span className="flex items-center gap-2">
                              {loan.creditScoreAtRequest}
                              {loan.tierAtRequest ? (
                                <TierBadge tier={loan.tierAtRequest} />
                              ) : null}
                            </span>
                          </Td>
                          <Td align="right">{formatDate(loan.dueDate)}</Td>
                          <Td>
                            <LoanStatusBadge status={loan.status} />
                          </Td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </TableScroll>
              </div>

              <Pagination
                page={loans.data.number}
                totalPages={loans.data.totalPages}
                totalElements={loans.data.totalElements}
                onChange={setPage}
              />
            </>
          )
        ) : null}
      </Card>
    </>
  );
}
