// Contract:
//   list()                        -> Promise<ClientUser[]>
//   getById(id)                   -> Promise<ClientUser|null>
//   unblock(id)                   -> Promise<ClientUser>
//   forceCloseVault(id)           -> Promise<ClientUser>
//   getKycQueue()                 -> Promise<KycSubmission[]>
//   approveKyc(submissionId)      -> Promise<void>
//   rejectKyc(submissionId, why)  -> Promise<void>   (reason is mandatory)
//   getKycDocumentFile(id)        -> Promise<Blob>   (the piece under review)
//
// Tier/state filtering is done client-side (see hooks/useUsers.js) against
// the full list, matching the single combined filter control in the UI.

import { httpClient } from '../api/httpClient';
import { endpoints } from '../api/endpoints';
import { delay, clone } from './mockUtils';

let usersSeed = [
  { id: 1, initials: 'AK', name: 'Aïcha Kodjo', phone: '+228 90 12 34 56', tier: 'TIER_2', score: 78, age: '14 mois', state: 'Actif', vaults: 3, loan: '100 000 XOF' },
  { id: 2, initials: 'KS', name: 'Kossi Sodji', phone: '+228 91 44 22 10', tier: 'TIER_1', score: 41, age: '3 mois', state: 'Actif', vaults: 1, loan: 'Aucun' },
  { id: 3, initials: 'MB', name: 'Mariam Bello', phone: '+225 07 88 12 33', tier: 'TIER_3', score: 92, age: '22 mois', state: 'Actif', vaults: 5, loan: '250 000 XOF' },
  { id: 4, initials: 'YT', name: 'Yao Tchalla', phone: '+228 92 10 55 09', tier: 'TIER_0', score: 12, age: '1 mois', state: 'Gelé', vaults: 0, loan: 'Aucun' },
  { id: 5, initials: 'FA', name: 'Fatou Adé', phone: '+225 05 60 41 27', tier: 'TIER_2', score: 66, age: '9 mois', state: 'Litige', vaults: 2, loan: '60 000 XOF' },
  { id: 6, initials: 'JN', name: 'Jean-Marc N’Guessan', phone: '+225 01 22 90 44', tier: 'TIER_3', score: 88, age: '18 mois', state: 'Actif', vaults: 4, loan: '180 000 XOF' },
];

let kycQueueSeed = [
  { id: 'kyc-1', name: 'Yao Tchalla', fromTier: 'TIER_1', toTier: 'TIER_2', receivedAt: 'Il y a 2 h', documentType: "Carte d'identité", contentType: 'image/jpeg', fileName: 'cni-recto.jpg' },
  { id: 'kyc-2', name: 'Ama Domingo', fromTier: 'TIER_2', toTier: 'TIER_3', receivedAt: 'Il y a 5 h', documentType: 'Justificatif de domicile', contentType: 'application/pdf', fileName: 'facture.pdf' },
  { id: 'kyc-3', name: 'Ibrahim Sy', fromTier: 'TIER_1', toTier: 'TIER_2', receivedAt: 'Hier', documentType: 'Passeport', contentType: 'image/png', fileName: 'passeport.png' },
];

export class MockUserRepository {
  async list() {
    await delay();
    return clone(usersSeed);
  }

  async getById(id) {
    await delay(120);
    const found = usersSeed.find((u) => u.id === Number(id));
    return found ? clone(found) : null;
  }

  async unblock(id) {
    await delay();
    const user = usersSeed.find((u) => u.id === Number(id));
    if (!user) throw new Error('Utilisateur introuvable.');
    user.state = 'Actif';
    return clone(user);
  }

  async forceCloseVault(id) {
    await delay();
    const user = usersSeed.find((u) => u.id === Number(id));
    if (!user) throw new Error('Utilisateur introuvable.');
    user.vaults = Math.max(0, user.vaults - 1);
    return clone(user);
  }

  async getKycQueue() {
    await delay();
    return clone(kycQueueSeed);
  }

  async approveKyc(submissionId) {
    await delay();
    const item = kycQueueSeed.find((k) => k.id === submissionId);
    if (item) {
      const user = usersSeed.find((u) => u.name === item.name);
      if (user) user.tier = item.toTier;
    }
    kycQueueSeed = kycQueueSeed.filter((k) => k.id !== submissionId);
  }

  async rejectKyc(submissionId, reason) {
    await delay();
    if (!reason || !reason.trim()) throw new Error('Un motif de rejet est obligatoire.');
    kycQueueSeed = kycQueueSeed.filter((k) => k.id !== submissionId);
  }

  /** No file behind the seeded queue; the viewer says so rather than showing a broken image. */
  async getKycDocumentFile() {
    await delay(150);
    throw new Error("Aperçu indisponible sur les données de démonstration.");
  }
}

export class HttpUserRepository {
  list() {
    return httpClient.get(endpoints.users.list());
  }

  getById(id) {
    return httpClient.get(endpoints.users.detail(id));
  }

  unblock(id) {
    return httpClient.post(endpoints.users.unblock(id), {});
  }

  forceCloseVault(id) {
    return httpClient.post(endpoints.users.forceCloseVault(id), {});
  }

  getKycQueue() {
    return httpClient.get(endpoints.users.kycQueue());
  }

  approveKyc(submissionId) {
    return httpClient.post(endpoints.users.approveKyc(submissionId), {});
  }

  rejectKyc(submissionId, reason) {
    return httpClient.post(endpoints.users.rejectKyc(submissionId), { reason });
  }

  getKycDocumentFile(documentId) {
    return httpClient.blob(endpoints.users.kycDocumentFile(documentId));
  }
}
