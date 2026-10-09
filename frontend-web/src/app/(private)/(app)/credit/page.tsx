"use client";

import Link from "next/link";
import { useCallback } from "react";
import { creditApi } from "@/lib/services";
import { useResource } from "@/lib/use-resource";
import { formatAmount, formatDate, formatMonthlyRate } from "@/lib/format";
import {
  CREDIT_TIER_LABEL,
  CREDIT_TIER_TONE,
  LOAN_STATUS_LABEL,
  LOAN_STATUS_TONE,
} from "@/lib/labels";
import {
  Alert,
  Badge,
  Card,
  EmptyState,
  LoadError,
  SectionHeading,
  Skeleton,
  SkeletonList,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { ScoreGauge } from "@/components/credit/score-gauge";
import { ChevronRightIcon, CreditIcon } from "@/components/ui/icons";

/**
 * Score de crédit et prêts.
 *
 * LE SCORE N'EST JAMAIS RECALCULÉ ICI. Le backend le produit à partir de sept
 * règles pondérées, le met en cache 30 jours et le rafraîchit de lui-même
 * (`CreditScoringService`). Cet écran ne fait que le montrer — et surtout,
 * montre les sept règles telles qu'elles arrivent, chacune avec son explication
 * déjà rédigée en français. C'est ce qui distingue un score utile d'un chiffre
 * arbitraire : on voit sur quoi agir pour le faire monter.
 */
export default function CreditPage() {
  const loadScore = useCallback((signal: AbortSignal) => creditApi.score(signal), []);
  const score = useResource(loadScore);

  const loadCapacity = useCallback(
    (signal: AbortSignal) => creditApi.capacity(signal),
    []
  );
  const capacity = useResource(loadCapacity);

  const loadLoans = useCallback((signal: AbortSignal) => creditApi.loans(signal), []);
  const loans = useResource(loadLoans);

  /* Plus de bouton « Recalculer » : `GET /credit/score` recalcule à chaque
     consultation, et une transaction rafraîchit le score dès qu'elle est
     validée. Demander à l'utilisateur d'actionner lui-même un recalcul, c'était
     lui faire porter l'existence d'un cache dont il n'a pas à connaître le
     fonctionnement — et lui laisser croire, tant qu'il n'appuyait pas, que la
     note affichée pouvait être fausse. */

  const breakdown = score.data;
  /* Un prêt en cours interdit d'en demander un second : le backend le refuse
     (`ActiveLoanExistsException`). Le dire avant, plutôt que de laisser
     remplir un formulaire voué à l'échec. */
  const openLoan = (loans.data ?? []).find(
    (loan) => loan.status === "PENDING" || loan.status === "APPROVED" || loan.status === "DISBURSED"
  );

  return (
    <>
      <PageHeader
        title="Crédit"
        description="Votre score de confiance et vos prêts."
      />

      <div className="space-y-8">
        {/* ---- Score ---- */}
        {score.loading ? (
          <Card className="space-y-4">
            <Skeleton className="mx-auto h-28 w-56" />
            <Skeleton className="h-4 w-2/3" />
          </Card>
        ) : score.error ? (
          <LoadError message={score.error} onRetry={score.reload} />
        ) : breakdown ? (
          <Card>
            <ScoreGauge score={breakdown.totalScore} tier={breakdown.tier} />

            <div className="mt-2 flex flex-col items-center gap-2">
              <Badge tone={CREDIT_TIER_TONE[breakdown.tier]}>
                Palier {CREDIT_TIER_LABEL[breakdown.tier]}
              </Badge>
              {/* Le score fixe le TAUX et un plafond de palier — pas le
                  montant. Afficher ici « empruntez jusqu'à 2 000 000 » parce
                  que le palier le permet donnait un chiffre que la capacité
                  réelle rendait presque toujours inatteignable. */}
              <p className="text-center text-sm text-ink-500">
                Taux de votre palier : {formatMonthlyRate(breakdown.monthlyRate)}
                {" · plafond "}
                {formatAmount(breakdown.maxLoanAmount)}
              </p>
              {breakdown.computedAt ? (
                <p className="text-xs text-ink-400">
                  Calculé le {formatDate(breakdown.computedAt)}
                  {breakdown.expiresAt
                    ? ` · valable jusqu'au ${formatDate(breakdown.expiresAt)}`
                    : ""}
                </p>
              ) : null}
            </div>

            {/* ═══ LE DÉTAIL DU BARÈME N'EST PLUS AFFICHÉ ═══

                Il l'était : chaque règle, ses points, son plafond et son
                explication. C'était utile — on voyait sur quoi agir — mais
                c'était aussi la notice d'utilisation du barème. Publier les
                poids apprend à les optimiser : enregistrer cinq bénéficiaires
                pour cinq points, déposer une fois par semaine plutôt qu'une
                fois par mois pour le même argent. Un score que l'on sait
                fabriquer ne mesure plus rien.

                Ce qui reste est ce qui engage Kola : la note, le palier, le
                taux. Ce qui disparaît est la recette.

                ⚠️ CETTE PAGE NE FAIT QUE MASQUER. Le tableau `details` continue
                d'arriver dans la réponse de `GET /credit/score` et reste
                lisible dans l'onglet réseau du navigateur. Pour que le barème
                cesse réellement d'être public, c'est le backend qui doit
                arrêter de l'envoyer. */}
            <div className="mt-6 border-t border-line pt-5">
              <h2 className="font-display text-sm font-semibold tracking-wide text-ink-500 uppercase">
                Comment il évolue
              </h2>
              <p className="mt-2 text-sm text-ink-500">
                Votre score est recalculé à partir de votre usage du compte :
                son ancienneté, la régularité de vos entrées, votre épargne, vos
                remboursements et votre niveau de vérification. Utilisez Kola
                régulièrement et il progressera.
              </p>
            </div>

          </Card>
        ) : null}

        {/* ---- Capacité : le montant, distinct de la solvabilité ---- */}
        {capacity.data && breakdown && breakdown.tier !== "INELIGIBLE" ? (
          <Card>
            <SectionHeading
              title="Ce que vous pouvez emprunter"
              description="Calculé sur vos entrées et sorties des 90 derniers jours — pas sur votre score."
            />
            <p className="tabular font-display text-3xl font-semibold text-ink-950">
              {formatAmount(capacity.data.maxAmountByDuration["6"] ?? 0)}
            </p>
            <p className="mt-1 text-sm text-ink-500">
              sur 6 mois · {capacity.data.limitingFactorLabel}
            </p>
            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              {[
                ["Entrées / mois", capacity.data.monthlyInflow],
                ["Sorties / mois", capacity.data.monthlyOutflow],
                ["Disponible / mois", capacity.data.monthlyDisposable],
              ].map(([label, amount]) => (
                <div key={label as string} className="rounded-field bg-ink-50 p-3">
                  <p className="text-xs text-ink-500">{label as string}</p>
                  <p className="tabular text-sm font-semibold text-ink-900">
                    {formatAmount(amount as number)}
                  </p>
                </div>
              ))}
            </div>
          </Card>
        ) : null}

        {/* ---- Prêts ---- */}
        <section>
          <SectionHeading
            title="Mes prêts"
            action={
              breakdown && breakdown.tier !== "INELIGIBLE" && !openLoan ? (
                <Link
                  href="/credit/demande"
                  className="text-sm font-semibold text-kola-600 hover:underline"
                >
                  Demander un prêt
                </Link>
              ) : null
            }
          />

          {openLoan ? (
            <Alert tone="info">
              Un prêt est en cours ({LOAN_STATUS_LABEL[openLoan.status].toLowerCase()}).
              Une nouvelle demande ne sera possible qu&apos;une fois celui-ci
              remboursé.
            </Alert>
          ) : null}

          <div className="mt-3">
            {loans.loading ? (
              <SkeletonList rows={2} />
            ) : loans.error ? (
              <LoadError message={loans.error} onRetry={loans.reload} />
            ) : loans.data && loans.data.length > 0 ? (
              <div className="space-y-2">
                {loans.data.map((loan) => (
                  <Link key={loan.id} href={`/credit/prets/${loan.id}`} className="block">
                    <Card className="transition-colors hover:bg-ink-50">
                      <div className="flex items-center gap-3">
                        <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-kola-50 text-lg text-kola-600">
                          <CreditIcon />
                        </span>
                        <div className="min-w-0 flex-1">
                          <p className="tabular font-semibold text-ink-900">
                            {formatAmount(loan.requestedAmount)}
                          </p>
                          <p className="truncate text-xs text-ink-500">
                            {loan.durationMonths} mois ·{" "}
                            {formatMonthlyRate(loan.monthlyRate)}
                            {loan.dueDate ? ` · échéance ${formatDate(loan.dueDate)}` : ""}
                          </p>
                        </div>
                        <Badge tone={LOAN_STATUS_TONE[loan.status]}>
                          {LOAN_STATUS_LABEL[loan.status]}
                        </Badge>
                        <ChevronRightIcon className="shrink-0 text-ink-400" />
                      </div>
                    </Card>
                  </Link>
                ))}
              </div>
            ) : (
              <EmptyState
                icon={<CreditIcon />}
                title="Aucun prêt"
                description={
                  breakdown && breakdown.tier === "INELIGIBLE"
                    ? "Votre score doit encore progresser avant qu'un prêt puisse être accordé. Les règles ci-dessus indiquent sur quoi agir."
                    : "Vous pouvez demander un prêt dans la limite de votre palier."
                }
                action={
                  breakdown && breakdown.tier !== "INELIGIBLE" ? (
                    <Link
                      href="/credit/demande"
                      className="inline-flex h-11 items-center justify-center rounded-full bg-kola-600 px-5 text-sm font-semibold text-white hover:bg-kola-700"
                    >
                      Demander un prêt
                    </Link>
                  ) : null
                }
              />
            )}
          </div>
        </section>
      </div>
    </>
  );
}
