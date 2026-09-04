// Contract:
//   list()                    -> Promise<Dispute[]>
//   getDetail(ref)            -> Promise<DisputeDetail>
//   chargeback(ref)           -> Promise<Dispute>
//   reject(ref)               -> Promise<Dispute>
//   validateChargeback(ref)   -> Promise<DisputeDetail>

import { httpClient } from '../api/httpClient';
import { endpoints } from '../api/endpoints';
import { delay, clone } from './mockUtils';

let disputesSeed = [
  { ref: 'TX-99C41A', tag: 'fraud', tagLabel: 'Fraude suspectée', amount: '450 000 XOF', title: 'Paiement marchand contesté · Boutique Sika', meta: 'Ouvert il y a 2 h · TIER_2 · Lomé', status: 'chargeback_pending' },
  { ref: 'TX-77B08D', tag: 'double_debit', tagLabel: 'Double débit', amount: '60 000 XOF', title: 'Cash-out exécuté deux fois · Agent 214', meta: 'Ouvert hier · TIER_3 · Kara', status: 'open' },
  { ref: 'TX-31F55E', tag: 'p2p', tagLabel: 'Litige P2P', amount: '25 000 XOF', title: 'Destinataire erroné déclaré par l’expéditeur', meta: 'Ouvert il y a 3 j · TIER_1 · Sokodé', status: 'open' },
];

let detailsSeed = {
  'TX-99C41A': {
    ref: 'TX-99C41A',
    debitedAccount: 'Boutique Sika',
    creditedAccount: 'Aïcha Kodjo',
    amount: '450 000 XOF',
    validationsRequired: 2,
    validationsDone: 1,
    lastValidationNote: "1re validation : Sena A. — en attente d'un 2e admin conformité",
  },
};

export class MockDisputeRepository {
  async list() {
    await delay();
    return clone(disputesSeed);
  }

  async getDetail(ref) {
    await delay(150);
    const detail = detailsSeed[ref];
    if (!detail) throw new Error('Aucun détail disponible pour ce litige.');
    return clone(detail);
  }

  async chargeback(ref) {
    await delay();
    const d = disputesSeed.find((x) => x.ref === ref);
    if (d) d.status = 'chargeback_pending';
    return d ? clone(d) : null;
  }

  async reject(ref) {
    await delay();
    const d = disputesSeed.find((x) => x.ref === ref);
    if (d) d.status = 'rejected';
    return d ? clone(d) : null;
  }

  async validateChargeback(ref) {
    await delay();
    const detail = detailsSeed[ref];
    if (!detail) throw new Error('Aucun détail disponible pour ce litige.');
    detail.validationsDone = Math.min(detail.validationsRequired, detail.validationsDone + 1);
    detail.lastValidationNote =
      detail.validationsDone >= detail.validationsRequired
        ? 'Validation complète — chargeback exécuté.'
        : detail.lastValidationNote;
    if (detail.validationsDone >= detail.validationsRequired) {
      const d = disputesSeed.find((x) => x.ref === ref);
      if (d) d.status = 'resolved';
    }
    return clone(detail);
  }
}

export class HttpDisputeRepository {
  list() {
    return httpClient.get(endpoints.disputes.list());
  }

  getDetail(ref) {
    return httpClient.get(endpoints.disputes.detail(ref));
  }

  chargeback(ref) {
    return httpClient.post(endpoints.disputes.chargeback(ref), {});
  }

  reject(ref) {
    return httpClient.post(endpoints.disputes.reject(ref), {});
  }

  validateChargeback(ref) {
    return httpClient.post(endpoints.disputes.validateChargeback(ref), {});
  }
}
