// Contract:
//   getMetrics()                  -> Promise<Metric[]>
//   getTransactionVolume(period)  -> Promise<ChartPoint[]>   period: '14d' | '30d'
//   getAlerts()                   -> Promise<Alert[]>
//   getLoanBookSummary()          -> Promise<LoanBookSummary>

import { httpClient } from '../api/httpClient';
import { endpoints } from '../api/endpoints';
import { delay, clone } from './mockUtils';

const METRICS_SEED = [
  { label: 'VOLUME 24 H', value: '842 M XOF', delta: '+ 14,2 % vs hier', up: true },
  { label: 'SOLDE GLOBAL', value: '6,4 Md XOF', delta: '+ 2,1 % ce mois', up: true },
  { label: 'CROISSANCE USERS', value: '+3 940 / sem.', delta: '184 320 comptes', up: true },
  { label: 'ENCOURS PRÊTS', value: '1,86 Md XOF', delta: '64 % de la capacité', up: true },
  { label: 'TAUX DE DÉFAUT', value: '2,8 %', delta: '− 0,4 pt vs T2', up: true },
];

// Bar heights are percentages of the tallest bar, as the real endpoint returns them.
const chartSeed = (heights) =>
  heights.map((h, i) => ({
    day: String(22 + i > 31 ? ((22 + i - 1) % 31) + 1 : 22 + i),
    h,
    last: i === heights.length - 1,
  }));

const CHART_14D_SEED = chartSeed([42, 55, 38, 61, 72, 49, 66, 80, 58, 74, 88, 63, 91, 100]);
const CHART_30D_SEED = chartSeed([
  31, 44, 29, 52, 47, 60, 35, 58, 71, 40, 66, 54, 78, 45,
  42, 55, 38, 61, 72, 49, 66, 80, 58, 74, 88, 63, 91, 100, 69, 83,
]);

const ALERTS_SEED = [
  { title: 'Fraude suspectée · TX-99C41A', detail: '450 000 XOF · Boutique Sika · en attente de double validation', severity: 'critical' },
  { title: '27 prêts en retard', detail: 'Encours à risque : 41 M XOF · relances automatiques envoyées', severity: 'warning' },
  { title: 'KYC expirés · 12 comptes', detail: 'Pièces d’identité arrivées à échéance, limites réduites', severity: 'warning' },
  { title: 'Chargeback seuil dépassé', detail: 'Admin Koffi M. : 4 chargebacks cette semaine, alerte déclenchée', severity: 'critical' },
];

const LOAN_BOOK_SUMMARY_SEED = {
  outstanding: '1,86 Md XOF',
  allocatedPct: 64,
  defaultRate: '2,8 %',
  defaultRateNote: "Seuil d'alerte fixé à 5 % · sous contrôle",
  userGrowth: '+ 3 940 / sem.',
  activeUsersTotal: '184 320 comptes actifs au total',
};

export class MockDashboardRepository {
  async getMetrics() {
    await delay();
    return clone(METRICS_SEED);
  }

  async getTransactionVolume(period = '14d') {
    await delay();
    return clone(period === '30d' ? CHART_30D_SEED : CHART_14D_SEED);
  }

  async getAlerts() {
    await delay();
    return clone(ALERTS_SEED);
  }

  async getLoanBookSummary() {
    await delay();
    return clone(LOAN_BOOK_SUMMARY_SEED);
  }
}

export class HttpDashboardRepository {
  getMetrics() {
    return httpClient.get(endpoints.dashboard.metrics());
  }

  getTransactionVolume(period = '14d') {
    return httpClient.get(endpoints.dashboard.transactionVolume(), { params: { period } });
  }

  getAlerts() {
    return httpClient.get(endpoints.dashboard.alerts());
  }

  getLoanBookSummary() {
    return httpClient.get(endpoints.dashboard.loanBookSummary());
  }
}
