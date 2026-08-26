"use client";

import { use, useCallback, useState } from "react";
import Link from "next/link";
import { ApiError, apiFetch } from "@/lib/api";
import { useResource } from "@/lib/use-resource";
import {
  Card,
  DataItem,
  ErrorBlock,
  LoadingBlock,
  PageHeading,
} from "@/components/kola/shell";
import { AmlStatusBadge, RiskBadge } from "@/components/kola/status";
import Button from "@/components/ui/button/Button";
import Label from "@/components/form/Label";
import { formatAmount, formatDateTime } from "@/lib/format";
import {
  AML_STATUSES,
  AML_STATUS_LABELS,
  type AmlAlert,
  type AmlAlertStatus,
} from "@/lib/types";

const CONTROL =
  "h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90";

/**
 * Fiche d'alerte et traitement par l'analyste.
 *
 * ═══ CE QUE FAIT L'ACTION DE REVUE ═══
 *
 * Elle change le statut et enregistre l'analyste (`reviewedBy`, renseigné côté
 * serveur à partir du jeton — jamais depuis le client). Le passage à « soupçon
 * confirmé » est l'issue lourde : elle engage une transmission à la cellule de
 * renseignement financier. L'écran le dit explicitement avant validation, parce
 * que rien dans une liste déroulante ne distingue autrement cette option des
 * trois autres.
 */
export default function AlertDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);

  const load = useCallback(
    (signal: AbortSignal) =>
      apiFetch<AmlAlert>(`/admin/aml/alerts/${id}`, { signal }),
    [id]
  );

  const alert = useResource(load);

  if (alert.error) {
    return (
      <>
        <PageHeading
          title="Alerte"
          backHref="/conformite"
          backLabel="Retour aux alertes"
        />
        <Card>
          <ErrorBlock message={alert.error} onRetry={alert.reload} />
        </Card>
      </>
    );
  }

  if (!alert.data) {
    return (
      <>
        <PageHeading
          title="Alerte"
          backHref="/conformite"
          backLabel="Retour aux alertes"
        />
        <Card>
          <LoadingBlock label="Chargement de l'alerte…" />
        </Card>
      </>
    );
  }

  const data = alert.data;

  return (
    <>
      <PageHeading
        title={`Alerte ${data.id}`}
        description={`Détectée le ${formatDateTime(data.createdAt)}`}
        backHref="/conformite"
        backLabel="Retour aux alertes"
        actions={
          <>
            <RiskBadge level={data.riskLevel} score={data.riskScore} />
            <AmlStatusBadge status={data.status} />
          </>
        }
      />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)]">
        <div className="space-y-6">
          <Card title="Client concerné">
            <dl className="grid gap-5 p-5 sm:grid-cols-2">
              <DataItem label="Nom">
                {data.userId ? (
                  <Link
                    href={`/utilisateurs/${data.userId}`}
                    className="font-medium text-brand-500 hover:text-brand-600"
                  >
                    {data.userFullName ?? "Voir la fiche"}
                  </Link>
                ) : (
                  (data.userFullName ?? "—")
                )}
              </DataItem>
              <DataItem label="E-mail">{data.userEmail ?? "—"}</DataItem>
            </dl>
          </Card>

          <Card title="Transaction déclenchante">
            <dl className="grid gap-5 p-5 sm:grid-cols-3">
              <DataItem label="Référence" mono>
                {data.transactionReference ?? "—"}
              </DataItem>
              <DataItem label="Type">{data.transactionType ?? "—"}</DataItem>
              <DataItem label="Montant">
                {formatAmount(data.transactionAmount)}
              </DataItem>
            </dl>
          </Card>

          <Card
            title="Règles déclenchées"
            description="Produites par le moteur de surveillance au moment de l'opération."
          >
            <TriggeredRules raw={data.triggeredRulesJson} />
          </Card>

          {data.reviewNotes || data.reviewedBy ? (
            <Card title="Dernier traitement">
              <dl className="grid gap-5 p-5 sm:grid-cols-2">
                <DataItem label="Analyste">{data.reviewedBy ?? "—"}</DataItem>
                <DataItem label="Notes">{data.reviewNotes ?? "—"}</DataItem>
              </dl>
            </Card>
          ) : null}
        </div>

        <ReviewPanel alert={data} onDone={alert.reload} />
      </div>
    </>
  );
}

/* ---------------------------------------------------------------------------
   Règles déclenchées
   ------------------------------------------------------------------------ */

/**
 * `triggeredRulesJson` est une CHAÎNE stockée telle quelle par le backend, pas
 * un objet : rien ne garantit qu'elle soit du JSON valide, ni quelle forme elle
 * prend. On tente donc l'analyse et on retombe sur le texte brut en cas
 * d'échec — plutôt que de laisser une exception vider la page. Perdre la mise en
 * forme est acceptable ; perdre l'information ne l'est pas, c'est elle qui
 * justifie l'alerte.
 */
function TriggeredRules({ raw }: { raw: string | null }) {
  if (!raw) {
    return (
      <p className="px-5 py-6 text-sm text-gray-500 dark:text-gray-400">
        Aucune règle enregistrée pour cette alerte.
      </p>
    );
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return (
      <pre className="overflow-x-auto px-5 py-4 font-mono text-theme-xs whitespace-pre-wrap text-gray-700 dark:text-gray-300">
        {raw}
      </pre>
    );
  }

  if (Array.isArray(parsed)) {
    return (
      <ul className="divide-y divide-gray-100 dark:divide-gray-800">
        {parsed.map((entry, index) => (
          <li
            key={index}
            className="px-5 py-3 text-sm text-gray-700 dark:text-gray-300"
          >
            {typeof entry === "string" ? entry : JSON.stringify(entry)}
          </li>
        ))}
      </ul>
    );
  }

  return (
    <pre className="overflow-x-auto px-5 py-4 font-mono text-theme-xs whitespace-pre-wrap text-gray-700 dark:text-gray-300">
      {JSON.stringify(parsed, null, 2)}
    </pre>
  );
}

/* ---------------------------------------------------------------------------
   Traitement
   ------------------------------------------------------------------------ */

function ReviewPanel({
  alert,
  onDone,
}: {
  alert: AmlAlert;
  onDone: () => void;
}) {
  const [status, setStatus] = useState<AmlAlertStatus>(alert.status);
  const [notes, setNotes] = useState(alert.reviewNotes ?? "");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [saved, setSaved] = useState(false);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setSaved(false);

    try {
      await apiFetch(`/admin/aml/alerts/${alert.id}/review`, {
        method: "POST",
        body: { status, notes },
      });
      setSaved(true);
      onDone();
    } catch (caught) {
      const fieldErrors =
        caught instanceof ApiError ? Object.values(caught.details) : [];
      setError(
        fieldErrors.length > 0
          ? fieldErrors.join(" ")
          : caught instanceof ApiError
            ? caught.message
            : "Impossible de joindre le serveur."
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card
      title="Traiter l'alerte"
      description="Votre identité est enregistrée avec la décision."
      className="h-fit xl:sticky xl:top-24"
    >
      <form onSubmit={submit} className="space-y-5 p-5">
        <div>
          <Label htmlFor="decision">Décision</Label>
          <select
            id="decision"
            className={CONTROL}
            value={status}
            onChange={(event) => setStatus(event.target.value as AmlAlertStatus)}
          >
            {AML_STATUSES.map((option) => (
              <option key={option} value={option}>
                {AML_STATUS_LABELS[option]}
              </option>
            ))}
          </select>
        </div>

        {/* L'avertissement n'apparaît que sur l'option qui engage réellement une
            démarche extérieure. Affiché en permanence, il deviendrait un élément
            de décor qu'on cesse de lire. */}
        {status === "CONFIRMED" ? (
          <p className="rounded-lg border border-warning-300 bg-warning-50 px-4 py-3 text-theme-xs leading-relaxed text-warning-700 dark:border-warning-500/40 dark:bg-warning-500/10 dark:text-warning-400">
            Confirmer le soupçon engage une déclaration à la cellule de
            renseignement financier (CENTIF). Le client ne doit en aucun cas en
            être informé.
          </p>
        ) : null}

        <div>
          <Label htmlFor="notes">Notes d&apos;analyse</Label>
          <textarea
            id="notes"
            rows={6}
            maxLength={2000}
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            className="w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90"
          />
          <p className="mt-1.5 text-theme-xs text-gray-500 dark:text-gray-400">
            Ce qui a motivé la décision : éléments vérifiés, pièces obtenues,
            contexte.
          </p>
        </div>

        {error ? (
          <p
            role="alert"
            className="rounded-lg border border-error-300 bg-error-50 px-4 py-3 text-sm text-error-700 dark:border-error-500/40 dark:bg-error-500/10 dark:text-error-400"
          >
            {error}
          </p>
        ) : null}

        {saved && !error ? (
          <p
            role="status"
            className="rounded-lg border border-success-300 bg-success-50 px-4 py-3 text-sm text-success-700 dark:border-success-500/40 dark:bg-success-500/10 dark:text-success-500"
          >
            Décision enregistrée.
          </p>
        ) : null}

        <Button type="submit" className="w-full" disabled={submitting}>
          {submitting ? "Enregistrement…" : "Enregistrer la décision"}
        </Button>
      </form>
    </Card>
  );
}
