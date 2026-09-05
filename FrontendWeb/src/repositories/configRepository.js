// Contract:
//   getFees()                     -> Promise<FeeConfig[]>
//   updateFees(fees)               -> Promise<FeeConfig[]>
//   getMerchants()                 -> Promise<Merchant[]>
//   updateMerchantStatus(id, status) -> Promise<Merchant>   status: 'active'|'suspended'|'pending'

import { httpClient } from '../api/httpClient';
import { endpoints } from '../api/endpoints';
import { delay, clone } from './mockUtils';

let feesSeed = [
  { tier: 'TIER_0', p2p: '2,5 %', merchant: '2 %', cashout: '1,5 %' },
  { tier: 'TIER_1', p2p: '2 %', merchant: '1,5 %', cashout: '1,2 %' },
  { tier: 'TIER_2', p2p: '1,5 %', merchant: '1 %', cashout: '1 %' },
  { tier: 'TIER_3', p2p: '1 %', merchant: '0,8 %', cashout: '0,8 %' },
];

let merchantsSeed = [
  { id: 'mer-1', name: 'Boutique Adjo', category: 'Commerce général', status: 'active' },
  { id: 'mer-2', name: 'Pharmacie du Port', category: 'Santé', status: 'active' },
  { id: 'mer-3', name: 'Sika Motors', category: 'Automobile', status: 'suspended' },
  { id: 'mer-4', name: 'École Les Palmiers', category: 'Éducation', status: 'pending' },
];

export class MockConfigRepository {
  async getFees() {
    await delay();
    return clone(feesSeed);
  }

  async updateFees(fees) {
    await delay();
    feesSeed = clone(fees);
    return clone(feesSeed);
  }

  async getMerchants() {
    await delay();
    return clone(merchantsSeed);
  }

  async updateMerchantStatus(id, status) {
    await delay();
    const merchant = merchantsSeed.find((m) => m.id === id);
    if (!merchant) throw new Error('Marchand introuvable.');
    merchant.status = status;
    return clone(merchant);
  }
}

export class HttpConfigRepository {
  getFees() {
    return httpClient.get(endpoints.config.fees());
  }

  updateFees(fees) {
    return httpClient.put(endpoints.config.fees(), { fees });
  }

  getMerchants() {
    return httpClient.get(endpoints.config.merchants());
  }

  updateMerchantStatus(id, status) {
    return httpClient.patch(endpoints.config.updateMerchantStatus(id), { status });
  }
}
