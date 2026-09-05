// Contract:
//   getStats()               -> Promise<CreditStats>
//   getTierConfig()          -> Promise<TierConfig[]>
//   updateTierConfig(tiers)  -> Promise<TierConfig[]>
//   getDefaults()            -> Promise<LoanDefault[]>
//   remind(loanId)           -> Promise<void>

import { httpClient } from '../api/httpClient';
import { endpoints } from '../api/endpoints';
import { delay, clone } from './mockUtils';

const STATS_SEED = { outstandingTotal: '1,86 Md XOF', defaultRate: '2,8 %', lateLoans: 27 };

let tierConfigSeed = [
  { name: 'TIER 1', minScore: 30, maxAmount: '50 000 XOF', monthlyRate: '10 %/mois' },
  { name: 'TIER 2', minScore: 55, maxAmount: '150 000 XOF', monthlyRate: '8 %/mois' },
  { name: 'TIER 3', minScore: 75, maxAmount: '300 000 XOF', monthlyRate: '7 %/mois' },
  { name: 'TIER 4', minScore: 90, maxAmount: '750 000 XOF', monthlyRate: '6 %/mois' },
];

const defaultsSeed = [
  { id: 'loan-1', borrowerName: 'Koffi Danho', amount: '80 000 XOF', daysLate: 12 },
  { id: 'loan-2', borrowerName: 'Abla Mensah', amount: '45 000 XOF', daysLate: 5 },
  { id: 'loan-3', borrowerName: 'Rachid Konaté', amount: '120 000 XOF', daysLate: 21 },
  { id: 'loan-4', borrowerName: 'Nadia Ouattara', amount: '60 000 XOF', daysLate: 3 },
];

export class MockCreditRepository {
  async getStats() {
    await delay();
    return clone(STATS_SEED);
  }

  async getTierConfig() {
    await delay();
    return clone(tierConfigSeed);
  }

  async updateTierConfig(tiers) {
    await delay();
    tierConfigSeed = clone(tiers);
    return clone(tierConfigSeed);
  }

  async getDefaults() {
    await delay();
    return clone(defaultsSeed);
  }

  async remind(_loanId) {
    await delay(200);
  }
}

export class HttpCreditRepository {
  getStats() {
    return httpClient.get(endpoints.credit.stats());
  }

  getTierConfig() {
    return httpClient.get(endpoints.credit.tierConfig());
  }

  updateTierConfig(tiers) {
    return httpClient.put(endpoints.credit.tierConfig(), { tiers });
  }

  getDefaults() {
    return httpClient.get(endpoints.credit.defaults());
  }

  remind(loanId) {
    return httpClient.post(endpoints.credit.remind(loanId), {});
  }
}
