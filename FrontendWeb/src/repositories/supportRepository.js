// Contract:
//   getTickets()        -> Promise<SupportTicket[]>
//   getManualActions()  -> Promise<ManualAction[]>
//   takeCharge(ref)      -> Promise<SupportTicket>   open -> in_progress
//   resolve(ref)         -> Promise<SupportTicket>   -> resolved

import { httpClient } from '../api/httpClient';
import { endpoints } from '../api/endpoints';
import { delay, clone } from './mockUtils';

let ticketsSeed = [
  { ref: '#8821', subject: 'Transfert non reçu', userName: 'Kossi Sodji', status: 'open' },
  { ref: '#8819', subject: 'Blocage injustifié du compte', userName: 'Yao Tchalla', status: 'in_progress' },
  { ref: '#8814', subject: 'Question sur le taux de crédit', userName: 'Mariam Bello', status: 'resolved' },
  { ref: '#8802', subject: 'Coffre non débloqué', userName: 'Fatou Adé', status: 'in_progress' },
  { ref: '#8795', subject: 'Demande de duplicata reçu', userName: 'Aïcha Kodjo', status: 'resolved' },
];

let manualActionsSeed = [
  { id: 'ma-1', action: 'Consultation historique de transactions', by: 'Prisca L.', time: '04 sept. 08:30' },
  { id: 'ma-2', action: 'Blocage temporaire du compte', by: 'Koffi M.', time: '02 sept. 14:12' },
  { id: 'ma-3', action: 'Ouverture litige TX-99C41A', by: 'Système', time: '02 sept. 12:05' },
  { id: 'ma-4', action: 'Note interne ajoutée au dossier', by: 'Koffi M.', time: '02 sept. 14:15' },
];

const logManualAction = (action) => {
  manualActionsSeed = [
    { id: `ma-${Date.now()}`, action, by: 'Vous', time: "à l'instant" },
    ...manualActionsSeed,
  ];
};

export class MockSupportRepository {
  async getTickets() {
    await delay();
    return clone(ticketsSeed);
  }

  async getManualActions() {
    await delay();
    return clone(manualActionsSeed);
  }

  async takeCharge(ref) {
    await delay();
    const ticket = ticketsSeed.find((t) => t.ref === ref);
    if (!ticket) throw new Error('Ticket introuvable.');
    ticket.status = 'in_progress';
    logManualAction(`Ticket ${ref} pris en charge`);
    return clone(ticket);
  }

  async resolve(ref) {
    await delay();
    const ticket = ticketsSeed.find((t) => t.ref === ref);
    if (!ticket) throw new Error('Ticket introuvable.');
    ticket.status = 'resolved';
    logManualAction(`Ticket ${ref} marqué résolu`);
    return clone(ticket);
  }
}

export class HttpSupportRepository {
  getTickets() {
    return httpClient.get(endpoints.support.tickets());
  }

  getManualActions() {
    return httpClient.get(endpoints.support.manualActions());
  }

  takeCharge(ref) {
    return httpClient.post(endpoints.support.takeCharge(ref), {});
  }

  resolve(ref) {
    return httpClient.post(endpoints.support.resolve(ref), {});
  }
}
