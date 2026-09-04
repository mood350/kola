// Maps domain values (plain strings/enums coming from services) to their
// visual representation. Keeping this out of models/ and repositories/ means
// the data layer never carries styling, and a real backend response needs no
// UI-shaped fields (no `stateStyle`, no `dotColor`) to slot in cleanly.

import { badge, OK, PEND, KO, NEUT } from '../lib/style';

export function userStateStyle(state) {
  if (state === 'Actif') return OK;
  if (state === 'Gelé') return KO;
  return PEND; // Litige
}

export const tierBadgeStyle = NEUT;

export function operatorStatusView(status) {
  return status === 'reconciled'
    ? { label: 'Réconcilié', style: OK }
    : { label: 'Écart détecté', style: PEND };
}

export function disputeTagStyle(tag) {
  if (tag === 'fraud') return KO;
  if (tag === 'double_debit') return PEND;
  return NEUT; // p2p
}

const MERCHANT_STATUS_VIEW = {
  active: { label: 'Actif', style: OK, actionLabel: 'Désactiver' },
  suspended: { label: 'Suspendu', style: KO, actionLabel: 'Réactiver' },
  pending: { label: 'En attente', style: PEND, actionLabel: 'Approuver' },
};
export function merchantStatusView(status) {
  return MERCHANT_STATUS_VIEW[status] || MERCHANT_STATUS_VIEW.pending;
}

export function roleBadgeStyle(role) {
  return role === 'Super-admin' ? badge('#002353', '#FFCB05') : NEUT;
}

const TICKET_STATUS_VIEW = {
  open: { label: 'Ouvert', style: KO },
  in_progress: { label: 'En cours', style: PEND },
  resolved: { label: 'Résolu', style: OK },
};
export function ticketStatusView(status) {
  return TICKET_STATUS_VIEW[status] || TICKET_STATUS_VIEW.open;
}

export function alertDotColor(severity) {
  return severity === 'critical' ? '#BA1A1A' : '#FFCB05';
}
