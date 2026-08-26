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
import { AmlStatusBadge, RiskBadge } from "@/components/kola/status";
import Label from "@/components/form/Label";
import { formatAmount, formatCount, formatDateTime } from "@/lib/format";
import {
  AML_RISK_LABELS,
  AML_RISK_LEVELS,
  AML_STATUSES,
  AML_STATUS_LABELS,
  type AmlAlert,
  type AmlOverview,
  type Page as PageResult,
} from "@/lib/types";

const CONTROL =
  "h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90";

/**
 * Console de conformité LAB-FT.
 *
 * ═══ CE QUE CET ÉCRAN N'EST PAS ═══
 *
 * Ce n'est pas un tableau de bord de plus. Les alertes listées ici désignent
 * nommément des clients soupçonnés, et leur traitement a des conséquences
 * réglementaires : une alerte confirmée doit être transmise à la cellule de
 * renseignement financier (CENTIF dans l'espace UEMOA). Le backend l'a placée
 * sous `/api/admin/**` précisément pour que ces informations ne puissent jamais
 * atteindre le client concerné — les lui divulguer constituerait un délit de
 * divulgation (« tipping off »).
 */
export default function CompliancePage() {
  const [status, setStatus] = useState("");
  const [riskLevel, setRiskLevel] = useState("");
  const [page, setPage] = useState(0);

  const loadOverview = useCallback(
    (signal: AbortSignal) =>
      apiFetch<AmlOverview>("/admin/aml/overview", { signal }),
    []
  );

  const loadAlerts = useCallback(
    (signal: AbortSignal) =>
      apiFetch<PageResult<AmlAlert>>(
        `/admin/aml/alerts${queryString({ status, riskLevel, page, size: 20 })}`,
        { signal }
      ),
    [status, riskLevel, page]
  );

  const overview = useResource(loadOverview);
  const alerts = useResource(loadAlerts);

  return (
    <>
      <PageHeading
        title="Conformité"
        description="Alertes de lutte anti-blanchiment produites par le moteur de règles, et leur traitement par l'analyste."
      />

      <div className="space-y-6">
        {/* ── Volumétrie ─────────────────────────────────────────────── */}
        <Card
          title="État du stock d'alertes"
          description="Les alertes ouvertes sont celles qui attendent une décision."
        >
          {overview.error ? (
            <ErrorBlock message={overview.error} onRetry={overview.reload} />
          ) : null}
          {overview.loading && !overview.data ? (
            <LoadingBlock label="Chargement…" />
          ) : null}

          {overview.data ? (
            <>
              <dl className="grid grid-cols-2 divide-gray-100 border-b border-gray-100 sm:grid-cols-3 lg:grid-cols-5 dark:divide-gray-800 dark:border-gray-800">
                <Bucket label="Total" value={overview.data.total} />
                {AML_STATUSES.map((key) => (
                  <Bucket
                    key={key}
                    label={AML_STATUS_LABELS[key]}
                    value={overview.data?.byStatus?.[key] ?? 0}
                    emphasis={key === "OPEN" || key === "CONFIRMED"}
                  />
                ))}
              </dl>

              <dl className="grid grid-cols-2 sm:grid-cols-4">
                {AML_RISK_LEVELS.map((key) => (
                  <Bucket
                    key={key}
                    label={`Risque ${AML_RISK_LABELS[key].toLowerCase()}`}
                    value={overview.data?.byRiskLevel?.[key] ?? 0}
                    emphasis={key === "CRITICAL"}
                  />
                ))}
              </dl>
            </>
          ) : null}
        </Card>

        {/* ── Liste ──────────────────────────────────────────────────── */}
        <Card>
          <div className="grid gap-4 border-b border-gray-100 p-5 sm:grid-cols-2 lg:max-w-2xl dark:border-gray-800">
            <div>
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
                {AML_STATUSES.map((option) => (
                  <option key={option} value={option}>
                    {AML_STATUS_LABELS[option]}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <Label htmlFor="risque">Niveau de risque</Label>
              <select
                id="risque"
                className={CONTROL}
                value={riskLevel}
                onChange={(event) => {
                  setRiskLevel(event.target.value);
                  setPage(0);
                }}
              >
                <option value="">Tous les niveaux</option>
                {AML_RISK_LEVELS.map((option) => (
                  <option key={option} value={option}>
                    {AML_RISK_LABELS[option]}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {alerts.error ? (
            <ErrorBlock message={alerts.error} onRetry={alerts.reload} />
          ) : null}

          {alerts.loading && !alerts.data ? (
            <LoadingBlock label="Chargement des alertes…" />
          ) : null}

          {alerts.data ? (
            alerts.data.content.length === 0 ? (
              <EmptyBlock
                title="Aucune alerte"
                hint={
                  status || riskLevel
                    ? "Aucune alerte ne correspond à ces filtres."
                    : "Le moteur de surveillance n'a déclenché aucune alerte."
                }
              />
            ) : (
              <>
                <div className={alerts.loading ? "opacity-60 transition-opacity" : undefined}>
                  <TableScroll>
                    <table className="w-full">
                      <thead>
                        <tr>
                          <Th>Client</Th>
                          <Th>Transaction</Th>
                          <Th align="right">Montant</Th>
                          <Th>Risque</Th>
                          <Th>Statut</Th>
                          <Th align="right">Détectée le</Th>
                        </tr>
                      </thead>
                      <tbody>
                        {alerts.data.content.map((alert) => (
                          <tr
                            key={alert.id}
                            className="transition-colors hover:bg-gray-50 dark:hover:bg-white/[0.02]"
                          >
                            <Td>
                              <Link
                                href={`/conformite/${alert.id}`}
                                className="font-medium text-gray-800 hover:text-brand-500 dark:text-white/90"
                              >
                                {alert.userFullName ?? `Alerte ${alert.id}`}
                              </Link>
                              <span className="block text-theme-xs text-gray-500 dark:text-gray-400">
                                {alert.userEmail}
                              </span>
                            </Td>
                            <Td>
                              <span className="font-mono text-theme-xs">
                                {alert.transactionReference ?? "—"}
                              </span>
                              <span className="block text-theme-xs text-gray-500 dark:text-gray-400">
                                {alert.transactionType ?? "—"}
                              </span>
                            </Td>
                            <Td align="right">
                              {formatAmount(alert.transactionAmount)}
                            </Td>
                            <Td>
                              <RiskBadge
                                level={alert.riskLevel}
                                score={alert.riskScore}
                              />
                            </Td>
                            <Td>
                              <AmlStatusBadge status={alert.status} />
                            </Td>
                            <Td align="right">{formatDateTime(alert.createdAt)}</Td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </TableScroll>
                </div>

                <Pagination
                  page={alerts.data.number}
                  totalPages={alerts.data.totalPages}
                  totalElements={alerts.data.totalElements}
                  onChange={setPage}
                />
              </>
            )
          ) : null}
        </Card>
      </div>
    </>
  );
}

/**
 * Case de volumétrie.
 *
 * `emphasis` distingue les deux compteurs qui appellent une action — alertes
 * ouvertes (à traiter) et soupçons confirmés (à transmettre) — des compteurs
 * purement descriptifs. Sans cette hiérarchie, neuf chiffres alignés se valent
 * tous, et aucun ne se remarque.
 */
function Bucket({
  label,
  value,
  emphasis = false,
}: {
  label: string;
  value: number;
  emphasis?: boolean;
}) {
  return (
    <div className="border-b border-gray-100 p-5 last:border-b-0 sm:border-b-0 dark:border-gray-800">
      <dt className="text-theme-xs font-medium tracking-wide text-gray-500 uppercase dark:text-gray-400">
        {label}
      </dt>
      <dd
        className={`mt-1.5 text-lg font-semibold ${
          emphasis && value > 0
            ? "text-error-600 dark:text-error-400"
            : "text-gray-800 dark:text-white/90"
        }`}
      >
        {formatCount(value)}
      </dd>
    </div>
  );
}
