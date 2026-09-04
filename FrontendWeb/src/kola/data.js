import { badge, OK, PEND, KO, NEUT, fmt } from '../lib/style';

export const wallets = [
  { code: 'XOF', short: '1 259 400', sym: 'F', note: 'Portefeuille principal · Togo' },
  { code: 'GHS', short: '2 480', sym: '₵', note: 'Ghana · via partenaire MTN' },
  { code: 'NGN', short: '145 000', sym: '₦', note: 'Nigeria · Naira' },
  { code: 'USD', short: '320', sym: '$', note: 'Réserve de change' },
];

export const actions = [
  { icon: '↗', label: 'Transférer', sub: 'P2P instantané · 1,5 %', go: 'wallet' },
  { icon: '▣', label: 'Payer un marchand', sub: 'Scan QR code', go: 'wallet' },
  { icon: '◆', label: 'Épargner', sub: 'Coffre-fort verrouillé', go: 'vaults' },
  { icon: '◔', label: 'Demander un crédit', sub: 'Pré-approuvé 300 000', go: 'credit' },
];

const amt = (neg) => ({ fontSize: 14, fontWeight: 800, whiteSpace: 'nowrap', color: neg ? '#0A1F5C' : '#0E8A5F' });

export const txns = [
  { icon: '↙', label: 'Dépôt Moov Money', meta: 'Aujourd’hui · 14:32 · gratuit', amount: '+ 150 000 XOF', amtStyle: amt(false) },
  { icon: '▣', label: 'Marchand · Boutique Adjo', meta: 'Aujourd’hui · 11:04 · frais 250', amount: '− 18 500 XOF', amtStyle: amt(true) },
  { icon: '↗', label: 'Transfert → Kossi A.', meta: 'Hier · 19:47 · frais 1,5 %', amount: '− 45 000 XOF', amtStyle: amt(true) },
  { icon: '◆', label: 'Virement programmé · Camion', meta: 'Hier · 00:01 · automatique', amount: '− 10 000 XOF', amtStyle: amt(true) },
  { icon: '◔', label: 'Décaissement prêt T2', meta: '2 sept. · 09:12 · 8 %/mois', amount: '+ 100 000 XOF', amtStyle: amt(false) },
];

export const upcoming = [
  { label: 'Remboursement prêt', amount: '108 000 XOF', date: '2 oct. 2026 · prélèvement auto' },
  { label: 'Virement → Scolarité', amount: '25 000 XOF', date: '5 sept. 2026 · mensuel' },
  { label: 'Virement → Réparation camion', amount: '10 000 XOF', date: '5 sept. 2026 · mensuel' },
];

const txnRows = [
  { icon: '↙', label: 'Dépôt Moov Money', meta: '04 sept. · 14:32', type: 'Cash-in', status: 'Réussi', st: OK, amount: '+ 150 000 XOF', neg: false, f: 'Dépôts' },
  { icon: '▣', label: 'Boutique Adjo · marchand', meta: '04 sept. · 11:04', type: 'Marchand', status: 'Réussi', st: OK, amount: '− 18 500 XOF', neg: true, f: 'Marchands' },
  { icon: '↗', label: 'Transfert → Kossi A.', meta: '03 sept. · 19:47', type: 'P2P', status: 'Réussi', st: OK, amount: '− 45 000 XOF', neg: true, f: 'Transferts' },
  { icon: '◆', label: 'Virement programmé · Camion', meta: '03 sept. · 00:01', type: 'Épargne', status: 'Réussi', st: OK, amount: '− 10 000 XOF', neg: true, f: 'Épargne' },
  { icon: '↗', label: 'Retrait cash-out · Agent 214', meta: '02 sept. · 16:20', type: 'Cash-out', status: 'En cours', st: PEND, amount: '− 60 000 XOF', neg: true, f: 'Transferts' },
  { icon: '◔', label: 'Décaissement prêt T2', meta: '02 sept. · 09:12', type: 'Crédit', status: 'Réussi', st: OK, amount: '+ 100 000 XOF', neg: false, f: 'Dépôts' },
  { icon: '↗', label: 'Transfert → +233 55 20 11', meta: '01 sept. · 08:55', type: 'P2P externe', status: 'Échoué', st: KO, amount: '− 30 000 XOF', neg: true, f: 'Transferts' },
];

export function allTxns(filter) {
  return txnRows
    .filter((r) => filter === 'Tous' || r.f === filter)
    .map((r) => ({ icon: r.icon, label: r.label, meta: r.meta, type: r.type, status: r.status, statusStyle: r.st, amount: r.amount, amtStyle: amt(r.neg) }));
}

export const filterNames = ['Tous', 'Transferts', 'Marchands', 'Dépôts', 'Épargne'];

export const quickAmounts = ['10 000', '25 000', '50 000', '100 000'];

export const fxRates = [
  { code: 'GHS', value: '0,0093' },
  { code: 'NGN', value: '2,41' },
  { code: 'USD', value: '0,0017' },
];

export const tierLimits = [
  { tier: 'TIER_0', limit: '50 000 XOF / jour' },
  { tier: 'TIER_1', limit: '250 000 XOF / jour' },
  { tier: 'TIER_2', limit: '1 000 000 XOF / jour' },
  { tier: 'TIER_3', limit: 'Illimité' },
];

export const operators = [
  { name: 'Moov Money', mode: 'API directe' },
  { name: 'Orange Money', mode: 'API directe' },
  { name: 'MTN MoMo', mode: 'Agrégateur' },
  { name: 'Wave', mode: 'Agrégateur' },
];

const dot = (on) => ({
  width: 38, height: 38, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center',
  fontSize: 14, fontWeight: 800, background: on ? '#F5B301' : '#fff', color: on ? '#0A1F5C' : '#8894B2',
  border: on ? '4px solid #fff' : '4px solid #EDF1F8',
});

export const kyc = [
  { name: 'TIER_0', req: 'Numéro de téléphone vérifié', state: 'Validé', mark: '✓', badgeStyle: { ...badge('#E6F5EE', '#0E8A5F'), marginTop: 11 }, dotStyle: dot(true) },
  { name: 'TIER_1', req: 'Email confirmé', state: 'Validé', mark: '✓', badgeStyle: { ...badge('#E6F5EE', '#0E8A5F'), marginTop: 11 }, dotStyle: dot(true) },
  { name: 'TIER_2', req: 'Pièce d’identité télévesée', state: 'Palier actuel', mark: '●', badgeStyle: { ...badge('#FFF4DA', '#96690A'), marginTop: 11 }, dotStyle: { ...dot(true), boxShadow: '0 0 0 7px rgba(245,179,1,.22)' } },
  { name: 'TIER_3', req: 'Identité validée par un agent', state: 'À compléter', mark: '3', badgeStyle: { ...badge('#EEF2FA', '#8894B2'), marginTop: 11 }, dotStyle: dot(false) },
];

export const kycLimits = [
  { name: 'TIER_0', send: '50 000 XOF', credit: 'Non', cur: false },
  { name: 'TIER_1', send: '200 000 XOF', credit: 'Non', cur: false },
  { name: 'TIER_2', send: '1 000 000 XOF', credit: 'Jusqu’à 300 000', cur: true },
  { name: 'TIER_3', send: 'Illimité', credit: 'Jusqu’à 750 000', cur: false },
].map((l) => ({
  ...l,
  rowStyle: {
    display: 'grid', gridTemplateColumns: '1.2fr 1fr 1fr', gap: 8, alignItems: 'center',
    padding: '14px 10px', borderRadius: 14, borderBottom: '1px solid #F4F7FC',
    background: l.cur ? '#FFFBF0' : 'transparent',
  },
}));

const docIcon = (c) => ({
  width: 34, height: 34, flex: '0 0 34px', borderRadius: 12, display: 'flex', alignItems: 'center',
  justifyContent: 'center', fontSize: 14, fontWeight: 800, background: 'rgba(255,255,255,.12)', color: c,
});

export const kycDocs = [
  { name: 'Carte d’identité (recto/verso)', note: 'Reçue le 28 août 2026', action: 'Voir', mark: '✓', iconStyle: docIcon('#0E8A5F') },
  { name: 'Selfie de vérification', note: 'En attente de votre envoi', action: 'Envoyer', mark: '↑', iconStyle: docIcon('#F5B301') },
  { name: 'Justificatif d’activité', note: 'Optionnel · accélère TIER_3', action: 'Ajouter', mark: '+', iconStyle: docIcon('rgba(255,255,255,.5)') },
];

export const metrics = [
  { label: 'VOLUME 24 H', value: '842 M XOF', delta: '+ 14,2 % vs hier', up: true },
  { label: 'SOLDE GLOBAL', value: '6,4 Md XOF', delta: '+ 2,1 % ce mois', up: true },
  { label: 'UTILISATEURS ACTIFS', value: '184 320', delta: '+ 3 940 cette semaine', up: true },
  { label: 'TAUX DE DÉFAUT', value: '2,8 %', delta: '− 0,4 pt vs T2', up: true },
];

export const chart = [42, 55, 38, 61, 72, 49, 66, 80, 58, 74, 88, 63, 91, 100].map((h, i) => ({
  day: String(22 + i > 31 ? 22 + i - 31 : 22 + i),
  h,
  last: i === 13,
}));

export const disputes = [
  { ref: 'TX-99C41A', tag: 'Fraude suspectée', tagStyle: badge('#FDEBEC', '#B3262F'), amount: '450 000 XOF', title: 'Paiement marchand contesté · Boutique Sika', meta: 'Ouvert il y a 2 h · TIER_2 · Lomé' },
  { ref: 'TX-77B08D', tag: 'Double débit', tagStyle: badge('#FFF4DA', '#96690A'), amount: '60 000 XOF', title: 'Cash-out exécuté deux fois · Agent 214', meta: 'Ouvert hier · TIER_3 · Kara' },
  { ref: 'TX-31F55E', tag: 'Litige P2P', tagStyle: badge('#EEF2FA', '#5C6B8E'), amount: '25 000 XOF', title: 'Destinataire erroné déclaré par l’expéditeur', meta: 'Ouvert il y a 3 j · TIER_1 · Sokodé' },
];

export const accounts = [
  { initials: 'AK', name: 'Aïcha Kodjo', meta: 'TIER_2 · score 78 · 3 coffres', state: 'Actif', stateStyle: OK, action: 'Gérer' },
  { initials: 'KS', name: 'Kossi Sodji', meta: 'TIER_1 · score 41 · 1 coffre', state: 'Actif', stateStyle: OK, action: 'Gérer' },
  { initials: 'MB', name: 'Mariam Bello', meta: 'TIER_3 · score 92 · 5 coffres', state: 'Actif', stateStyle: OK, action: 'Gérer' },
  { initials: 'YT', name: 'Yao Tchalla', meta: 'TIER_0 · score 12 · 0 coffre', state: 'Gelé', stateStyle: KO, action: 'Débloquer' },
  { initials: 'FA', name: 'Fatou Adé', meta: 'TIER_2 · score 66 · coffre bloqué', state: 'Litige', stateStyle: PEND, action: 'Forcer' },
];

export const factors = [
  { name: 'Régularité des dépôts', value: '22/25', detail: '6 approvisionnements ce mois', pct: 88 },
  { name: 'Discipline d’épargne', value: '24/25', detail: '3 virements programmés honorés', pct: 96 },
  { name: 'Volume & diversité', value: '18/25', detail: '42 paiements marchands, 11 P2P', pct: 72 },
  { name: 'Stabilité du solde', value: '14/25', detail: '2 soldes proches de zéro', pct: 56 },
];

export const tierDefs = [
  { name: 'TIER 1', max: '50 000 XOF', rate: '10 %/mois', min: 30 },
  { name: 'TIER 2', max: '150 000 XOF', rate: '8 %/mois', min: 55 },
  { name: 'TIER 3', max: '300 000 XOF', rate: '7 %/mois', min: 75 },
  { name: 'TIER 4', max: '750 000 XOF', rate: '6 %/mois', min: 90 },
];

export const schedule = [
  { label: 'Prêt décaissé', date: '2 sept. 2026 · 09:12', amount: '+ 100 000', status: 'Reçu', statusStyle: OK, dot: '#0E8A5F' },
  { label: 'Rappel J-7', date: '25 sept. 2026', amount: '—', status: 'Planifié', statusStyle: NEUT, dot: '#C3CCDE' },
  { label: 'Échéance unique', date: '2 oct. 2026 · 00:01', amount: '108 000', status: 'À venir', statusStyle: PEND, dot: '#F5B301', glow: true },
  { label: 'Clôture & score +5', date: '2 oct. 2026', amount: '—', status: 'Bonus', statusStyle: OK, dot: '#C3CCDE' },
];

export const vaults = [
  { icon: '◆', name: 'Réparation camion', rule: '10 000 XOF · le 5 de chaque mois', saved: '210 000', goal: '300 000 XOF', pct: 70, tag: 'Virement actif', tagStyle: badge('#FFF4DA', '#96690A') },
  { icon: '◎', name: 'Scolarité des enfants', rule: '25 000 XOF · le 5 de chaque mois', saved: '225 000', goal: '450 000 XOF', pct: 50, tag: 'Virement actif', tagStyle: badge('#FFF4DA', '#96690A') },
  { icon: '▣', name: 'Fonds d’urgence', rule: 'Versements manuels', saved: '50 000', goal: '150 000 XOF', pct: 33, tag: 'Manuel', tagStyle: badge('#EEF2FA', '#5C6B8E') },
];

export { fmt };
