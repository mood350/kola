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
import { LoanStatusBadge, TierBadge } from "@/components/kola/status";
import Button from "@/components/ui/button/Button";
import { Modal } from "@/components/ui/modal";
import Label from "@/components/form/Label";
import {
  formatAmount,
  formatDate,
  formatDateTime,
  formatMonthlyRate,
} from "@/lib/format";
import type { AdminLoanSummary } from "@/lib/types";

/**
 * Fiche d'un prêt.
 *
 * Le point de cet écran est de rendre le dossier RECONSTITUABLE : combien a été
 * demandé, sur la base de quel score, à quel taux, et où en est le
 * remboursement. Le score au dépôt est un instantané figé dans le prêt — il ne
 * suit pas le score courant de l'emprunteur, sans quoi on ne pourrait plus
 * justifier après coup la décision prise ce jour-là.
 *
 * ═══ LA DÉCISION D'EXAMEN ═══
 *
 * Au-delà de 200 000 XOF, un prêt n'est plus accordé automatiquement : il
 * arrive ici en attente, et l'argent N'A PAS ÉTÉ VERSÉ. Les deux boutons ne
 * s'affichent donc que sur un dossier en attente — sur tout autre statut, la
 * décision est déjà prise et l'API la refuserait.
 *
 * Accorder décaisse immédiatement : c'est irréversible, d'où la confirmation.
 */
export default function LoanDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);

  const load = useCallback(
    (signal: AbortSignal) =>
      apiFetch<AdminLoanSummary>(`/admin/loans/${id}`, { signal }),
    [id]
  );

  const loan = useResource(load);
  const [decision, setDecision] = useState<"approve" | "reject" | null>(null);

  if (loan.error) {
    return (
      <>
        <PageHeading title="Dossier de prêt" backHref="/prets" backLabel="Retour aux prêts" />
        <Card>
          <ErrorBlock message={loan.error} onRetry={loan.reload} />
        </Card>
      </>
    );
  }

  if (!loan.data) {
    return (
      <>
        <PageHeading title="Dossier de prêt" backHref="/prets" backLabel="Retour aux prêts" />
        <Card>
          <LoadingBlock label="Chargement du dossier…" />
        </Card>
      </>
    );
  }

  const data = loan.data;

  return (
    <>
      <PageHeading
        title={formatAmount(data.requestedAmount)}
        description={`Demandé le ${formatDate(data.createdAt)}${data.purpose ? ` · ${data.purpose}` : ""}`}
        backHref="/prets"
        backLabel="Retour aux prêts"
        actions={
          <div className="flex items-center gap-3">
            <LoanStatusBadge status={data.status} />
            {/* Seul un dossier en attente se tranche : afficher ces boutons
                ailleurs proposerait une action que l'API refuse. */}
            {data.status === "PENDING" ? (
              <>
                <Button size="sm" variant="outline" onClick={() => setDecision("reject")}>
                  Refuser
                </Button>
                <Button size="sm" onClick={() => setDecision("approve")}>
                  Accorder et débourser
                </Button>
              </>
            ) : null}
          </div>
        }
      />

      <div className="space-y-6">
        <Card title="Conditions du prêt">
          <dl className="grid gap-5 p-5 sm:grid-cols-2 lg:grid-cols-4">
            <DataItem label="Principal">
              {formatAmount(data.requestedAmount)}
            </DataItem>
            <DataItem label="Total à rembourser">
              {formatAmount(data.totalRepayment)}
              {/* Le coût du crédit n'est écrit nulle part dans le modèle : il se
                  déduit, et l'afficher évite de le calculer de tête à chaque
                  dossier. */}
              <span className="mt-0.5 block text-theme-xs text-gray-500 dark:text-gray-400">
                dont {formatAmount(data.totalRepayment - data.requestedAmount)}{" "}
                d&apos;intérêts
              </span>
            </DataItem>
            <DataItem label="Taux">{formatMonthlyRate(data.monthlyRate)}</DataItem>
            <DataItem label="Durée">{data.durationMonths} mois</DataItem>
            <DataItem label="Échéance">{formatDate(data.dueDate)}</DataItem>
            <DataItem label="Score au dépôt">
              <span className="flex items-center gap-2">
                {data.creditScoreAtRequest} / 100
                {data.tierAtRequest ? <TierBadge tier={data.tierAtRequest} /> : null}
              </span>
            </DataItem>
            <DataItem label="Motif déclaré">
              {data.purpose ?? "Non renseigné"}
            </DataItem>
            <DataItem label="Passage en défaut">
              {data.defaultedAt ? formatDateTime(data.defaultedAt) : "Jamais"}
            </DataItem>
          </dl>

          {data.rejectionReason ? (
            <div className="border-t border-gray-100 px-5 py-4 dark:border-gray-800">
              <DataItem label="Motif du refus">{data.rejectionReason}</DataItem>
            </div>
          ) : null}
        </Card>

        <Card title="Emprunteur">
          <dl className="grid gap-5 p-5 sm:grid-cols-3">
            <DataItem label="Nom">
              {data.borrowerId ? (
                <Link
                  href={`/utilisateurs/${data.borrowerId}`}
                  className="font-medium text-brand-500 hover:text-brand-600"
                >
                  {data.borrowerFullName ?? "Voir la fiche"}
                </Link>
              ) : (
                (data.borrowerFullName ?? "—")
              )}
            </DataItem>
            <DataItem label="E-mail">{data.borrowerEmail ?? "—"}</DataItem>
            <DataItem label="Identifiant" mono>
              {data.borrowerId ?? "—"}
            </DataItem>
          </dl>
        </Card>
      </div>

      {decision ? (
        <DecisionDialog
          kind={decision}
          loan={data}
          onClose={() => setDecision(null)}
          onDone={() => {
            setDecision(null);
            loan.reload();
          }}
        />
      ) : null}
    </>
  );
}

/**
 * Confirmation d'une décision de crédit.
 *
 * Le motif n'est demandé qu'au refus : le backend l'exige (10 à 500
 * caractères) et le transmet tel quel à l'emprunteur. Un accord, lui, se
 * justifie par le dossier — c'est le montant qui est repris dans le journal.
 */
function DecisionDialog({
  kind,
  loan,
  onClose,
  onDone,
}: {
  kind: "approve" | "reject";
  loan: AdminLoanSummary;
  onClose: () => void;
  onDone: () => void;
}) {
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      if (kind === "approve") {
        await apiFetch(`/admin/loans/${loan.id}/approve`, { method: "POST" });
      } else {
        await apiFetch(`/admin/loans/${loan.id}/reject`, {
          method: "POST",
          body: { reason },
        });
      }
      onDone();
    } catch (caught) {
      if (caught instanceof ApiError) {
        const fieldErrors = Object.values(caught.details);
        setError(fieldErrors.length > 0 ? fieldErrors.join(" ") : caught.message);
      } else {
        setError("Impossible de joindre le serveur.");
      }
      setSubmitting(false);
    }
  }

  return (
    <Modal isOpen onClose={onClose} className="m-4 max-w-md p-6 lg:p-8">
      <h2 className="text-lg font-semibold text-gray-800 dark:text-white/90">
        {kind === "approve" ? "Accorder ce prêt" : "Refuser ce prêt"}
      </h2>
      <p className="mt-2 text-sm leading-relaxed text-gray-500 dark:text-gray-400">
        {kind === "approve"
          ? `${formatAmount(loan.requestedAmount)} seront versés immédiatement sur le portefeuille de l'emprunteur, et l'échéance courra à partir d'aujourd'hui. Cette opération est irréversible.`
          : "Le motif sera transmis tel quel à l'emprunteur et consigné dans le journal avec votre identité."}
      </p>

      <form onSubmit={submit} className="mt-6 space-y-5">
        {kind === "reject" ? (
          <div>
            <Label htmlFor="motif">Motif du refus</Label>
            <textarea
              id="motif"
              rows={3}
              required
              minLength={10}
              maxLength={500}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              className="w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90"
            />
            <p className="mt-1.5 text-theme-xs text-gray-500 dark:text-gray-400">
              Dix caractères minimum.
            </p>
          </div>
        ) : null}

        {error ? (
          <p
            role="alert"
            className="rounded-lg border border-error-300 bg-error-50 px-4 py-3 text-sm text-error-700 dark:border-error-500/40 dark:bg-error-500/10 dark:text-error-400"
          >
            {error}
          </p>
        ) : null}

        <div className="flex justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onClose}
            disabled={submitting}
          >
            Annuler
          </Button>
          <Button type="submit" size="sm" disabled={submitting}>
            {submitting ? "En cours…" : kind === "approve" ? "Accorder et débourser" : "Refuser"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
