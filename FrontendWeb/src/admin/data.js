import { badge, OK, PEND, KO, NEUT, delta } from '../lib/style';

export const metrics = [
  { label: 'VOLUME 24 H', value: '842 M XOF', delta: '+ 14,2 % vs hier', up: true },
  { label: 'SOLDE GLOBAL', value: '6,4 Md XOF', delta: '+ 2,1 % ce mois', up: true },
  { label: 'CROISSANCE USERS', value: '+3 940 / sem.', delta: '184 320 comptes', up: true },
  { label: 'ENCOURS PRÊTS', value: '1,86 Md XOF', delta: '64 % de la capacité', up: true },
  { label: 'TAUX DE DÉFAUT', value: '2,8 %', delta: '− 0,4 pt vs T2', up: true },
];

export const chart = [42, 55, 38, 61, 72, 49, 66, 80, 58, 74, 88, 63, 91, 100].map((h, i) => ({
  day: String(22 + i > 31 ? 22 + i - 31 : 22 + i),
  h,
  last: i === 13,
}));

export const alerts = [
  { title: 'Fraude suspectée · TX-99C41A', detail: '450 000 XOF · Boutique Sika · en attente de double validation', dot: '#B3262F' },
  { title: '27 prêts en retard', detail: 'Encours à risque : 41 M XOF · relances automatiques envoyées', dot: '#F5B301' },
  { title: 'KYC expirés · 12 comptes', detail: 'Pièces d’identité arrivées à échéance, limites réduites', dot: '#F5B301' },
  { title: 'Chargeback seuil dépassé', detail: 'Admin Koffi M. : 4 chargebacks cette semaine, alerte déclenchée', dot: '#B3262F' },
];

export const quickLinks = [
  { id: 'users', label: 'Utilisateurs' },
  { id: 'credit', label: 'Crédit' },
  { id: 'finance', label: 'Finance' },
  { id: 'disputes', label: 'Litiges' },
  { id: 'audit', label: 'Audit' },
];

const tierStyle = badge('#EEF2FA', '#5C6B8E');

export const users = [
  { id: 1, initials: 'AK', name: 'Aïcha Kodjo', phone: '+228 90 12 34 56', tier: 'TIER_2', score: 78, age: '14 mois', state: 'Actif', vaults: 3, loan: '100 000 XOF' },
  { id: 2, initials: 'KS', name: 'Kossi Sodji', phone: '+228 91 44 22 10', tier: 'TIER_1', score: 41, age: '3 mois', state: 'Actif', vaults: 1, loan: 'Aucun' },
  { id: 3, initials: 'MB', name: 'Mariam Bello', phone: '+225 07 88 12 33', tier: 'TIER_3', score: 92, age: '22 mois', state: 'Actif', vaults: 5, loan: '250 000 XOF' },
  { id: 4, initials: 'YT', name: 'Yao Tchalla', phone: '+228 92 10 55 09', tier: 'TIER_0', score: 12, age: '1 mois', state: 'Gelé', vaults: 0, loan: 'Aucun' },
  { id: 5, initials: 'FA', name: 'Fatou Adé', phone: '+225 05 60 41 27', tier: 'TIER_2', score: 66, age: '9 mois', state: 'Litige', vaults: 2, loan: '60 000 XOF' },
  { id: 6, initials: 'JN', name: 'Jean-Marc N’Guessan', phone: '+225 01 22 90 44', tier: 'TIER_3', score: 88, age: '18 mois', state: 'Actif', vaults: 4, loan: '180 000 XOF' },
].map((u) => ({
  ...u,
  tierStyle,
  stateStyle: u.state === 'Actif' ? OK : u.state === 'Gelé' ? KO : PEND,
}));

export const userFilterNames = ['Tous', 'TIER_2', 'TIER_3', 'Litige'];

export const kycQueue = [
  { name: 'Yao Tchalla', meta: 'TIER_1 → TIER_2 · pièce reçue il y a 2 h' },
  { name: 'Ama Domingo', meta: 'TIER_2 → TIER_3 · pièce reçue il y a 5 h' },
  { name: 'Ibrahim Sy', meta: 'TIER_1 → TIER_2 · pièce reçue hier' },
];

export const tierConfig = [
  { name: 'TIER 1', min: '30', max: '50 000 XOF', rate: '10 %/mois' },
  { name: 'TIER 2', min: '55', max: '150 000 XOF', rate: '8 %/mois' },
  { name: 'TIER 3', min: '75', max: '300 000 XOF', rate: '7 %/mois' },
  { name: 'TIER 4', min: '90', max: '750 000 XOF', rate: '6 %/mois' },
];

export const defaults = [
  { name: 'Koffi Danho', amount: '80 000 XOF', late: 12 },
  { name: 'Abla Mensah', amount: '45 000 XOF', late: 5 },
  { name: 'Rachid Konaté', amount: '120 000 XOF', late: 21 },
  { name: 'Nadia Ouattara', amount: '60 000 XOF', late: 3 },
];

export const liquidity = [
  { label: 'BLOQUÉ EN VAULTS', value: '2,1 Md XOF', note: '38 % de la liquidité totale' },
  { label: 'PRÊTÉ AUX UTILISATEURS', value: '1,86 Md XOF', note: '47 % de la liquidité totale' },
  { label: 'DISPONIBLE', value: '590 M XOF', note: '15 % · réserve de sécurité' },
];

export const revenue = [
  { label: 'Commissions transactions', value: '184,2 M XOF', pct: 59, color: '#0F2A6B' },
  { label: 'Intérêts microcrédit', value: '96,8 M XOF', pct: 31, color: '#3E5BAE' },
  { label: 'Spread épargne (Vaults)', value: '31,4 M XOF', pct: 10, color: '#F5B301' },
];

export const financeOperators = [
  { name: 'Moov Money', state: 'Réconcilié', stateStyle: OK },
  { name: 'Orange Money', state: 'Réconcilié', stateStyle: OK },
  { name: 'MTN Mobile Money', state: 'Écart détecté', stateStyle: PEND },
];

export const disputes = [
  { ref: 'TX-99C41A', tag: 'Fraude suspectée', tagStyle: KO, amount: '450 000 XOF', title: 'Paiement marchand contesté · Boutique Sika', meta: 'Ouvert il y a 2 h · TIER_2 · Lomé' },
  { ref: 'TX-77B08D', tag: 'Double débit', tagStyle: PEND, amount: '60 000 XOF', title: 'Cash-out exécuté deux fois · Agent 214', meta: 'Ouvert hier · TIER_3 · Kara' },
  { ref: 'TX-31F55E', tag: 'Litige P2P', tagStyle: NEUT, amount: '25 000 XOF', title: 'Destinataire erroné déclaré par l’expéditeur', meta: 'Ouvert il y a 3 j · TIER_1 · Sokodé' },
];

export const feeConfig = [
  { tier: 'TIER_0', p2p: '2,5 %', merchant: '2 %', cashout: '1,5 %' },
  { tier: 'TIER_1', p2p: '2 %', merchant: '1,5 %', cashout: '1,2 %' },
  { tier: 'TIER_2', p2p: '1,5 %', merchant: '1 %', cashout: '1 %' },
  { tier: 'TIER_3', p2p: '1 %', merchant: '0,8 %', cashout: '0,8 %' },
];

export const merchants = [
  { name: 'Boutique Adjo', cat: 'Commerce général', state: 'Actif', stateStyle: OK, action: 'Désactiver' },
  { name: 'Pharmacie du Port', cat: 'Santé', state: 'Actif', stateStyle: OK, action: 'Désactiver' },
  { name: 'Sika Motors', cat: 'Automobile', state: 'Suspendu', stateStyle: KO, action: 'Réactiver' },
  { name: 'École Les Palmiers', cat: 'Éducation', state: 'En attente', stateStyle: PEND, action: 'Approuver' },
];

export const audit = [
  { admin: 'Sena A.', action: 'Déblocage compte #4021', diff: 'Gelé → Actif', time: '04 sept. 09:12' },
  { admin: 'Koffi M.', action: 'Chargeback TX-88A21', diff: '45 000 → 0 XOF', time: '03 sept. 17:40' },
  { admin: 'Aya D.', action: 'Validation KYC #3390', diff: 'TIER_1 → TIER_2', time: '03 sept. 15:02' },
  { admin: 'Sena A.', action: 'Modif. taux TIER_3', diff: '8 % → 7 %/mois', time: '02 sept. 11:20' },
  { admin: 'Koffi M.', action: 'Fermeture coffre #712', diff: 'Actif → Clos', time: '01 sept. 08:55' },
  { admin: 'Aya D.', action: 'Rejet KYC #3388', diff: 'Pièce illisible', time: '31 août 16:10' },
];

export const reports = [
  { name: 'Rapport mensuel AML', period: 'Août 2026' },
  { name: 'Déclarations de soupçon', period: 'T3 2026' },
  { name: 'Seuils de transactions suspectes', period: 'Août 2026' },
];

export const admins = [
  { initials: 'SA', name: 'Sena Amétépé', role: 'Super-admin', roleStyle: badge('#0A1F5C', '#F5B301'), scope: 'Accès total · configuration produit' },
  { initials: 'KM', name: 'Koffi Messan', role: 'Agent conformité', roleStyle: NEUT, scope: 'KYC, litiges, chargebacks (2e validation)' },
  { initials: 'AD', name: 'Aya Djobo', role: 'Analyste crédit', roleStyle: NEUT, scope: 'Scoring, paliers de prêt, défauts' },
  { initials: 'PL', name: 'Prisca Lawson', role: 'Support', roleStyle: NEUT, scope: 'Tickets, consultation comptes (lecture seule)' },
];

export const tickets = [
  { ref: '#8821', subject: 'Transfert non reçu', user: 'Kossi Sodji', state: 'Ouvert', stateStyle: KO },
  { ref: '#8819', subject: 'Blocage injustifié du compte', user: 'Yao Tchalla', state: 'En cours', stateStyle: PEND },
  { ref: '#8814', subject: 'Question sur le taux de crédit', user: 'Mariam Bello', state: 'Résolu', stateStyle: OK },
  { ref: '#8802', subject: 'Coffre non débloqué', user: 'Fatou Adé', state: 'En cours', stateStyle: PEND },
  { ref: '#8795', subject: 'Demande de duplicata reçu', user: 'Aïcha Kodjo', state: 'Résolu', stateStyle: OK },
];

export const manualActions = [
  { action: 'Consultation historique de transactions', by: 'Prisca L.', time: '04 sept. 08:30' },
  { action: 'Blocage temporaire du compte', by: 'Koffi M.', time: '02 sept. 14:12' },
  { action: 'Ouverture litige TX-99C41A', by: 'Système', time: '02 sept. 12:05' },
  { action: 'Note interne ajoutée au dossier', by: 'Koffi M.', time: '02 sept. 14:15' },
];

export { badge, delta };
