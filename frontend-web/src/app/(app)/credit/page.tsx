"use client";

import Link from "next/link";
import { useCallback, useState } from "react";
import { creditApi } from "@/lib/services";
import { describeActionError, useResource } from "@/lib/use-resource";
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
  Button,
  Card,
  EmptyState,
  LoadError,
  ProgressBar,
  SectionHeading,
  Skeleton,
  SkeletonList,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { ScoreGauge } from "@/components/credit/score-gauge";
import { ChevronRightIcon, CreditIcon, RefreshIcon } from "@/components/ui/icons";

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

  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = async () => {
    setError(null);
    setRefreshing(true);
    try {
      await creditApi.refreshScore();
      score.reload();
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setRefreshing(false);
    }
  };

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
        action={
          <Button
            variant="secondary"
            size="sm"
            icon={<RefreshIcon />}
            loading={refreshing}
            onClick={() => void refresh()}
          >
            Recalculer
          </Button>
        }
      />

      <div className="space-y-8">
        {error ? <Alert tone="danger">{error}</Alert> : null}

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

            <div className="mt-6 border-t border-line pt-5">
              <h2 className="mb-4 font-display text-sm font-semibold tracking-wide text-ink-500 uppercase">
                Ce qui compose votre score
              </h2>
              <ul className="space-y-4">
                {breakdown.details.map((rule) => (
                  <li key={rule.rule}>
                    <div className="flex items-baseline justify-between gap-3">
                      <p className="text-sm font-medium text-ink-900">{rule.label}</p>
                      <p className="tabular shrink-0 text-sm text-ink-600">
                        {rule.points} / {rule.maxPoints}
                      </p>
                    </div>
                    <ProgressBar
                      ratio={rule.maxPoints > 0 ? rule.points / rule.maxPoints : 0}
                      tone={
                        rule.points >= rule.maxPoints * 0.75
                          ? "positive"
                          : rule.points >= rule.maxPoints * 0.4
                            ? "warning"
                            : "danger"
                      }
                      className="mt-1.5"
                    />
                    {/* Explication renvoyée par le backend, affichée telle
                        quelle : la reformuler ici la ferait diverger du calcul
                        qu'elle décrit. */}
                    <p className="mt-1 text-xs text-ink-500">{rule.explanation}</p>
                  </li>
                ))}
              </ul>
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
