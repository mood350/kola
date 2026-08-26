"use client";

import { useCallback } from "react";
import Link from "next/link";
import { apiFetch } from "@/lib/api";
import { useResource } from "@/lib/use-resource";
import {
  Card,
  ErrorBlock,
  LoadingBlock,
  PageHeading,
} from "@/components/kola/shell";
import { MetricCard } from "@/components/kola/metric-card";
import {
  BreakdownChart,
  MonthlyTransactionsChart,
} from "@/components/kola/charts";
import { formatAmount, formatCount } from "@/lib/format";
import { BoxIconLine, DollarLineIcon, GroupIcon, ShootingStarIcon } from "@/icons";
import {
  KYC_LABELS,
  LOAN_STATUS_LABELS,
  type AdminLoanOverview,
  type AdminOverview,
  type KycLevel,
  type LoanStatus,
} from "@/lib/types";

/**
 * Tableau de bord.
 *
 * ═══ ORDRE DE LECTURE ═══
 *
 * Du général au particulier, et de l'actionnable au descriptif :
 *
 *   1. quatre indicateurs de tête — la santé de la plateforme d'un coup d'œil ;
 *   2. le portefeuille de prêts, seule zone où de l'argent est engagé et non
 *      encore recouvré ;
 *   3. la chronologie des transactions, qui donne la tendance ;
 *   4. les ventilations, qui expliquent d'où viennent les chiffres du haut.
 *
 * Les deux ressources (`/admin/overview` et `/admin/loans/overview`) sont
 * chargées SÉPARÉMENT et affichent leurs états indépendamment : si l'une échoue,
 * l'autre reste lisible. Les fondre en un seul chargement ferait disparaître
 * tout le tableau de bord pour une panne partielle.
 */
export default function DashboardPage() {
  const loadOverview = useCallback(
    (signal: AbortSignal) =>
      apiFetch<AdminOverview>("/admin/overview", { signal }),
    []
  );
  const loadLoans = useCallback(
    (signal: AbortSignal) =>
      apiFetch<AdminLoanOverview>("/admin/loans/overview", { signal }),
    []
  );

  const overview = useResource(loadOverview);
  const loans = useResource(loadLoans);

  return (
    <>
      <PageHeading
        title="Tableau de bord"
        description="Vue d'ensemble : comptes, encaisses, transactions et encours de crédit."
      />

      {overview.error ? (
        <Card>
          <ErrorBlock message={overview.error} onRetry={overview.reload} />
        </Card>
      ) : null}

      {overview.loading && !overview.data ? (
        <Card>
          <LoadingBlock label="Chargement des indicateurs…" />
        </Card>
      ) : null}

      {overview.data ? (
        <div className="space-y-6">
          {/* ── Indicateurs de tête ──────────────────────────────────── */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4 md:gap-6">
            <MetricCard
              label="Utilisateurs actifs"
              value={formatCount(overview.data.activeUsers)}
              icon={<GroupIcon className="size-6" />}
              delta={overview.data.userGrowthRate}
              hint={`${formatCount(overview.data.totalUsers)} comptes au total, dont ${formatCount(overview.data.adminUsers)} administrateur${overview.data.adminUsers > 1 ? "s" : ""}`}
            />
            <MetricCard
              label="Volume de transactions"
              value={formatAmount(overview.data.totalTransactionVolume)}
              icon={<DollarLineIcon className="size-6" />}
              delta={overview.data.transactionVolumeGrowthRate}
              hint={`${formatAmount(overview.data.totalFees)} de frais perçus`}
            />
            <MetricCard
              label="Encaisse des portefeuilles"
              value={formatAmount(overview.data.activeWalletBalance)}
              icon={<BoxIconLine className="size-6" />}
              hint={`${formatCount(overview.data.activeWallets)} portefeuille${overview.data.activeWallets > 1 ? "s" : ""} actif${overview.data.activeWallets > 1 ? "s" : ""}`}
            />
            <MetricCard
              label="Épargne bloquée en coffres"
              value={formatAmount(overview.data.lockedVaultAmount)}
              icon={<ShootingStarIcon className="size-6" />}
              hint={`${formatCount(overview.data.activeVaults)} coffre${overview.data.activeVaults > 1 ? "s" : ""} actif${overview.data.activeVaults > 1 ? "s" : ""}`}
            />
          </div>

          {/* ── Encours de crédit ────────────────────────────────────── */}
          <Card
            title="Portefeuille de prêts"
            description="L'encours ne compte que les prêts approuvés, versés ou en défaut — l'argent réellement engagé."
            actions={
              <Link
                href="/prets"
                className="text-sm font-medium text-brand-500 hover:text-brand-600"
              >
                Voir les prêts
              </Link>
            }
          >
            {loans.error ? (
              <ErrorBlock message={loans.error} onRetry={loans.reload} />
            ) : null}
            {loans.loading && !loans.data ? (
              <LoadingBlock label="Chargement de l'encours…" />
            ) : null}
            {loans.data ? (
              <>
                <dl className="grid grid-cols-1 divide-y divide-gray-100 border-b border-gray-100 sm:grid-cols-3 sm:divide-x sm:divide-y-0 dark:divide-gray-800 dark:border-gray-800">
                  <Figure label="Prêts au total" value={formatCount(loans.data.total)} />
                  <Figure
                    label="Principal engagé"
                    value={formatAmount(loans.data.outstandingPrincipal)}
                  />
                  <Figure
                    label="Total à recouvrer"
                    value={formatAmount(loans.data.outstandingRepayment)}
                  />
                </dl>

                <BreakdownChart
                  items={loans.data.byStatus.map((bucket) => ({
                    label: bucket.status,
                    count: bucket.count,
                    amount: bucket.principal,
                  }))}
                  measure="count"
                  formatLabel={(label) =>
                    LOAN_STATUS_LABELS[label as LoanStatus] ?? label
                  }
                  emptyLabel="Aucun prêt enregistré."
                />
              </>
            ) : null}
          </Card>

          {/* ── Chronologie ──────────────────────────────────────────── */}
          <Card
            title="Transactions par mois"
            description="Nombre d'opérations sur les six derniers mois. Survolez une colonne pour le montant."
          >
            <MonthlyTransactionsChart items={overview.data.monthlyTransactions} />
          </Card>

          {/* ── Ventilations ─────────────────────────────────────────── */}
          <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
            <Card
              title="Transactions par type"
              description="Nombre d'opérations ; montant cumulé dans l'infobulle."
            >
              <BreakdownChart
                items={overview.data.transactionsByType}
                measure="count"
                formatLabel={humanizeType}
              />
            </Card>

            <Card
              title="Transactions par statut"
              description="Une part élevée d'opérations échouées mérite investigation."
            >
              <BreakdownChart
                items={overview.data.transactionsByStatus}
                measure="count"
                formatLabel={humanizeStatus}
              />
            </Card>

            <Card
              title="Utilisateurs par niveau de vérification"
              description="Le palier KYC commande les plafonds et pèse 15 points dans le score de crédit."
            >
              <BreakdownChart
                items={overview.data.usersByKycLevel}
                measure="count"
                formatLabel={(label) =>
                  KYC_LABELS[label as KycLevel]
                    ? `${label.replace("TIER_", "N")} · ${KYC_LABELS[label as KycLevel]}`
                    : label
                }
              />
            </Card>

            <Card
              title="Utilisateurs par pays"
              description="« Non renseigné » regroupe les comptes sans code pays."
            >
              <BreakdownChart
                items={overview.data.usersByCountry}
                measure="count"
                formatLabel={countryName}
              />
            </Card>

            <Card
              title="Encaisse par devise"
              description="Solde disponible et montant bloqué cumulés."
            >
              <BreakdownChart
                items={overview.data.walletsByCurrency}
                measure="amount"
              />
            </Card>

            <Card
              title="Transactions par pays"
              description="Volume d'opérations par pays d'origine du compte."
            >
              <BreakdownChart
                items={overview.data.transactionsByCountry}
                measure="count"
                formatLabel={countryName}
              />
            </Card>
          </div>
        </div>
      ) : null}
    </>
  );
}

function Figure({ label, value }: { label: string; value: string }) {
  return (
    <div className="p-5">
      <dt className="text-theme-xs font-medium tracking-wide text-gray-500 uppercase dark:text-gray-400">
        {label}
      </dt>
      <dd className="mt-1.5 text-lg font-semibold text-gray-800 dark:text-white/90">
        {value}
      </dd>
    </div>
  );
}

/* ---------------------------------------------------------------------------
   Traduction des libellés bruts
   ------------------------------------------------------------------------ */

/**
 * Les libellés viennent des enums Java, en majuscules anglaises. Les afficher
 * tels quels ferait lire « TRANSFER_OUT » à un opérateur francophone. Les tables
 * restent partielles à dessein : une valeur inconnue retombe sur le libellé brut
 * plutôt que de disparaître — mieux vaut « TRANSFER_OUT » qu'un vide.
 */

/** Recopié sur `transaction/TransactionType.java` — les onze valeurs. */
const TYPE_LABELS: Record<string, string> = {
  DEPOSIT: "Dépôt",
  WITHDRAWAL: "Retrait",
  TRANSFER_OUT: "Transfert émis",
  TRANSFER_IN: "Transfert reçu",
  VAULT_LOCK: "Blocage en coffre",
  VAULT_UNLOCK: "Déblocage de coffre",
  FEE: "Frais",
  SCHEDULED_TRANSFER: "Virement programmé",
  LOAN_DISBURSEMENT: "Versement de prêt",
  LOAN_REPAYMENT: "Remboursement de prêt",
  MERCHANT_PAYMENT: "Paiement marchand",
};

function humanizeType(label: string): string {
  return TYPE_LABELS[label] ?? label;
}

/** Recopié sur `transaction/TransactionStatus.java`. */
const TX_STATUS_LABELS: Record<string, string> = {
  PENDING: "En attente",
  SUCCESS: "Aboutie",
  FAILED: "Échouée",
  CANCELLED: "Annulée",
  REFUNDED: "Remboursée",
};

function humanizeStatus(label: string): string {
  return TX_STATUS_LABELS[label] ?? label;
}

/**
 * Les huit États de l'UEMOA, plus le repli « UN » produit par le backend pour un
 * compte sans code pays (`COALESCE(u.countryCode, 'UN')`).
 */
const COUNTRY_NAMES: Record<string, string> = {
  BJ: "Bénin",
  BF: "Burkina Faso",
  CI: "Côte d'Ivoire",
  GW: "Guinée-Bissau",
  ML: "Mali",
  NE: "Niger",
  SN: "Sénégal",
  TG: "Togo",
  UN: "Non renseigné",
};

function countryName(code: string): string {
  return COUNTRY_NAMES[code] ?? code;
}
