// Contract:
//   getLog()          -> Promise<AuditLogEntry[]>
//   exportLog()       -> Promise<{ url: string }>
//   getReports()      -> Promise<ComplianceReport[]>
//   exportReport(id)  -> Promise<{ url: string }>

import { httpClient } from '../api/httpClient';
import { endpoints } from '../api/endpoints';
import { delay, clone } from './mockUtils';

const LOG_SEED = [
  { id: 'log-1', admin: 'Sena A.', action: 'Déblocage compte #4021', diff: 'Gelé → Actif', time: '04 sept. 09:12' },
  { id: 'log-2', admin: 'Koffi M.', action: 'Chargeback TX-88A21', diff: '45 000 → 0 XOF', time: '03 sept. 17:40' },
  { id: 'log-3', admin: 'Aya D.', action: 'Validation KYC #3390', diff: 'TIER_1 → TIER_2', time: '03 sept. 15:02' },
  { id: 'log-4', admin: 'Sena A.', action: 'Modif. taux TIER_3', diff: '8 % → 7 %/mois', time: '02 sept. 11:20' },
  { id: 'log-5', admin: 'Koffi M.', action: 'Fermeture coffre #712', diff: 'Actif → Clos', time: '01 sept. 08:55' },
  { id: 'log-6', admin: 'Aya D.', action: 'Rejet KYC #3388', diff: 'Pièce illisible', time: '31 août 16:10' },
];

const REPORTS_SEED = [
  { id: 'rep-1', name: 'Rapport mensuel AML', period: 'Août 2026' },
  { id: 'rep-2', name: 'Déclarations de soupçon', period: 'T3 2026' },
  { id: 'rep-3', name: 'Seuils de transactions suspectes', period: 'Août 2026' },
];

export class MockAuditRepository {
  async getLog() {
    await delay();
    return clone(LOG_SEED);
  }

  async exportLog() {
    await delay(300);
    return { url: null };
  }

  async getReports() {
    await delay();
    return clone(REPORTS_SEED);
  }

  async exportReport(_id) {
    await delay(300);
    return { url: null };
  }
}

export class HttpAuditRepository {
  getLog() {
    return httpClient.get(endpoints.audit.log());
  }

  exportLog() {
    return httpClient.post(endpoints.audit.exportLog(), {});
  }

  getReports() {
    return httpClient.get(endpoints.audit.reports());
  }

  exportReport(id) {
    return httpClient.post(endpoints.audit.exportReport(id), {});
  }
}
