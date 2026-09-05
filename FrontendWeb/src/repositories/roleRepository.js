// Contract:
//   getAdmins()                             -> Promise<AdminAccount[]>
//   updatePermissions(id, { role, scope })  -> Promise<AdminAccount>

import { httpClient } from '../api/httpClient';
import { endpoints } from '../api/endpoints';
import { delay, clone } from './mockUtils';

let adminsSeed = [
  { id: 'adm-1', initials: 'SA', name: 'Sena Amétépé', role: 'Super-admin', scope: 'Accès total · configuration produit' },
  { id: 'adm-2', initials: 'KM', name: 'Koffi Messan', role: 'Agent conformité', scope: 'KYC, litiges, chargebacks (2e validation)' },
  { id: 'adm-3', initials: 'AD', name: 'Aya Djobo', role: 'Analyste crédit', scope: 'Scoring, paliers de prêt, défauts' },
  { id: 'adm-4', initials: 'PL', name: 'Prisca Lawson', role: 'Support', scope: 'Tickets, consultation comptes (lecture seule)' },
];

export class MockRoleRepository {
  async getAdmins() {
    await delay();
    return clone(adminsSeed);
  }

  async updatePermissions(id, { role, scope }) {
    await delay();
    const admin = adminsSeed.find((a) => a.id === id);
    if (!admin) throw new Error('Admin introuvable.');
    if (role) admin.role = role;
    if (scope != null) admin.scope = scope;
    return clone(admin);
  }
}

export class HttpRoleRepository {
  getAdmins() {
    return httpClient.get(endpoints.roles.admins());
  }

  updatePermissions(id, { role, scope }) {
    return httpClient.patch(endpoints.roles.updatePermissions(id), { role, scope });
  }
}
