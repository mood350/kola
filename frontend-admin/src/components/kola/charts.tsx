"use client";

import dynamic from "next/dynamic";
import type { ApexOptions } from "apexcharts";
import { formatAmount, formatCount, formatMonthLabel } from "@/lib/format";
import type { AdminMetric } from "@/lib/types";
import { EmptyBlock } from "@/components/kola/shell";

/**
 * Graphiques du tableau de bord (ApexCharts, comme le template).
 *
 * `ssr: false` est OBLIGATOIRE : ApexCharts touche `window` au moment de son
 * import. Rendu côté serveur, il échoue au build — ce n'est pas une
 * optimisation, c'est la seule façon de le charger.
 *
 * ═══ POURQUOI UNE SEULE TEINTE PARTOUT ═══
 *
 * Toutes ces séries sont uniques : on compare des MAGNITUDES entre catégories,
 * pas des séries qu'on suivrait dans le temps. L'encodage juste est donc
 * séquentiel — une teinte, celle de la marque. Colorier chaque barre
 * différemment ferait croire que la couleur porte une information, alors
 * qu'elle ne ferait que répéter le libellé déjà écrit à côté ; pire, elle
 * suivrait le RANG, et un filtre qui réordonne repeindrait des catégories
 * inchangées.
 *
 * Conséquence directe : aucune légende. Il n'y a qu'une série, le titre du
 * panneau dit déjà ce qui est mesuré.
 *
 * ═══ ÉCHELLES ANCRÉES À ZÉRO ═══
 *
 * Sur des volumes financiers, une base tronquée transforme une variation de
 * quelques pour cent en falaise. ApexCharts part de zéro par défaut sur les
 * barres ; on ne le contredit jamais ici.
 */
const ReactApexChart = dynamic(() => import("react-apexcharts"), {
  ssr: false,
});

/** Indigo Kola — même valeur que `--color-brand-500`. */
const BRAND = "#494fdf";

const BASE_CHART: ApexOptions["chart"] = {
  fontFamily: "Outfit, sans-serif",
  toolbar: { show: false },
  /* Les animations d'entrée d'ApexCharts rejouent à chaque rechargement de
     données. Sur une console qu'on filtre en continu, elles transforment un
     changement de filtre en spectacle de deux secondes. */
  animations: { enabled: false },
};

/* ---------------------------------------------------------------------------
   Volume mensuel
   ------------------------------------------------------------------------ */

/**
 * Six mois glissants, en colonnes.
 *
 * La colonne l'emporte sur la courbe à six points : une courbe suggérerait une
 * continuité entre les mois qui n'existe pas — chaque mois est un total, pas un
 * instant mesuré.
 *
 * L'infobulle affiche le montant en plus du nombre : la hauteur n'encode qu'une
 * des deux grandeurs, l'autre serait invisible sans elle.
 */
export function MonthlyTransactionsChart({ items }: { items: AdminMetric[] }) {
  if (items.length === 0) {
    return (
      <EmptyBlock title="Aucune transaction sur les six derniers mois." />
    );
  }

  const options: ApexOptions = {
    colors: [BRAND],
    chart: { ...BASE_CHART, type: "bar", height: 240 },
    plotOptions: {
      bar: {
        horizontal: false,
        columnWidth: "45%",
        /* Chapeau arrondi, pied carré sur la ligne de base : un arrondi des
           deux côtés ferait flotter les petites valeurs au-dessus de l'axe. */
        borderRadius: 4,
        borderRadiusApplication: "end",
      },
    },
    dataLabels: { enabled: false },
    legend: { show: false },
    grid: {
      yaxis: { lines: { show: true } },
      xaxis: { lines: { show: false } },
    },
    xaxis: {
      categories: items.map((item) => formatMonthLabel(item.label)),
      axisBorder: { show: false },
      axisTicks: { show: false },
    },
    yaxis: {
      labels: { formatter: (value: number) => formatCount(value) },
    },
    tooltip: {
      /* Le montant du mois est repris depuis `items` par l'index du point :
         ApexCharts ne transporte qu'une valeur par série. */
      custom: ({ dataPointIndex }: { dataPointIndex: number }) => {
        const item = items[dataPointIndex];
        return `<div class="px-3 py-2">
          <div class="text-[10px] leading-4 text-gray-500">${formatMonthLabel(item.label)}</div>
          <div class="text-theme-xs font-medium">${formatCount(item.count)} opération${item.count > 1 ? "s" : ""}</div>
          <div class="text-theme-xs">${formatAmount(item.amount)}</div>
        </div>`;
      },
    },
  };

  return (
    <div className="px-4 pt-4 pb-2">
      <ReactApexChart
        options={options}
        series={[{ name: "Opérations", data: items.map((item) => item.count) }]}
        type="bar"
        height={240}
      />
    </div>
  );
}

/* ---------------------------------------------------------------------------
   Ventilations
   ------------------------------------------------------------------------ */

/**
 * Ventilation par catégorie, en barres horizontales.
 *
 * Horizontal et non vertical : les libellés sont des noms de pays, des types de
 * transaction, des paliers KYC. En colonnes, ils seraient inclinés ou tronqués.
 *
 * La hauteur suit le nombre de catégories : une hauteur fixe écraserait huit
 * lignes ou laisserait un vide sous deux.
 */
export function BreakdownChart({
  items,
  measure,
  formatLabel,
  emptyLabel = "Aucune donnée sur cette période.",
}: {
  items: AdminMetric[];
  /** Grandeur portée par la barre. L'autre reste dans l'infobulle. */
  measure: "count" | "amount";
  formatLabel?: (label: string) => string;
  emptyLabel?: string;
}) {
  if (items.length === 0) {
    return <EmptyBlock title={emptyLabel} />;
  }

  const values = items.map((item) =>
    measure === "count" ? item.count : item.amount
  );
  const height = Math.max(160, items.length * 44 + 40);

  const options: ApexOptions = {
    colors: [BRAND],
    chart: { ...BASE_CHART, type: "bar", height },
    plotOptions: {
      bar: {
        horizontal: true,
        barHeight: "58%",
        borderRadius: 4,
        borderRadiusApplication: "end",
      },
    },
    dataLabels: { enabled: false },
    legend: { show: false },
    grid: {
      yaxis: { lines: { show: false } },
      xaxis: { lines: { show: true } },
    },
    xaxis: {
      categories: items.map((item) =>
        formatLabel ? formatLabel(item.label) : item.label
      ),
      axisBorder: { show: false },
      axisTicks: { show: false },
      labels: {
        formatter: (value: string) =>
          measure === "count"
            ? formatCount(Number(value))
            : formatAmount(Number(value)),
      },
    },
    tooltip: {
      custom: ({ dataPointIndex }: { dataPointIndex: number }) => {
        const item = items[dataPointIndex];
        const label = formatLabel ? formatLabel(item.label) : item.label;
        return `<div class="px-3 py-2">
          <div class="text-[10px] leading-4 text-gray-500">${label}</div>
          <div class="text-theme-xs font-medium">${formatCount(item.count)}</div>
          <div class="text-theme-xs">${formatAmount(item.amount)}</div>
        </div>`;
      },
    },
  };

  return (
    <div className="px-2 pt-2 pb-1">
      <ReactApexChart
        options={options}
        series={[
          { name: measure === "count" ? "Nombre" : "Montant", data: values },
        ]}
        type="bar"
        height={height}
      />
    </div>
  );
}
