// Contract:
//   getLiquidity()               -> Promise<LiquidityBucket[]>
//   getRevenue()                 -> Promise<{ lines: RevenueLine[], total: string }>
//   getOperatorReconciliation()  -> Promise<OperatorStatus[]>

import { httpClient } from '../api/httpClient';
import { endpoints } from '../api/endpoints';
import { delay, clone } from './mockUtils';

const LIQUIDITY_SEED = [
  { label: 'BLOQUÉ EN VAULTS', value: '2,1 Md XOF', note: '38 % de la liquidité totale', pct: 38 },
  { label: 'PRÊTÉ AUX UTILISATEURS', value: '1,86 Md XOF', note: '47 % de la liquidité totale', pct: 47 },
  { label: 'DISPONIBLE', value: '590 M XOF', note: '15 % · réserve de sécurité', pct: 15 },
];

const REVENUE_SEED = {
  lines: [
    { label: 'Commissions transactions', value: '184,2 M XOF', pct: 59 },
    { label: 'Intérêts microcrédit', value: '96,8 M XOF', pct: 31 },
    { label: 'Spread épargne (Vaults)', value: '31,4 M XOF', pct: 10 },
  ],
  total: '312,4 M XOF',
};

const OPERATORS_SEED = [
  { name: 'Moov Money', status: 'reconciled' },
  { name: 'Orange Money', status: 'reconciled' },
  { name: 'MTN Mobile Money', status: 'discrepancy' },
];

export class MockFinanceRepository {
  async getLiquidity() {
    await delay();
    return clone(LIQUIDITY_SEED);
  }

  async getRevenue() {
    await delay();
    return clone(REVENUE_SEED);
  }

  async getOperatorReconciliation() {
    await delay();
    return clone(OPERATORS_SEED);
  }
}

export class HttpFinanceRepository {
  getLiquidity() {
    return httpClient.get(endpoints.finance.liquidity());
  }

  getRevenue() {
    return httpClient.get(endpoints.finance.revenue());
  }

  getOperatorReconciliation() {
    return httpClient.get(endpoints.finance.operatorReconciliation());
  }
}
