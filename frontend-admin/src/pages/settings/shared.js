/** Lecture des saisies de l'écran Paramètres : un nombre, ou NaN quand la saisie n'est pas acceptable. */

/** Ce que le serveur affiche, et accepte en retour, pour un palier sans plafond (stocké à null). */
export const NO_CEILING = 'Aucun plafond';

/** « 1,5 », « 1.5 », « 12 » → nombre entre 0 et 100, une décimale au plus (c'est la précision que le serveur affiche). */
export function readPercent(text) {
  const value = String(text).trim().replace(',', '.');
  if (!/^\d{1,3}(\.\d)?$/.test(value)) return NaN;
  const number = Number(value);
  return number <= 100 ? number : NaN;
}

/** Une valeur du serveur (« 1,5 % », « 8,0 %/mois ») → nombre. */
export const percentFromServer = (text) => readPercent(String(text).replace(/[^\d.,]/g, ''));

/** Nombre → texte de saisie, virgule décimale. */
export const percentInput = (number) => String(number).replace('.', ',');

/** Nombre → texte affiché : « 1,5 % ». */
export const percentLabel = (number) =>
  `${number.toLocaleString('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} %`;

/** Score minimal : entier de 0 à 100. */
export function readScore(text) {
  const value = String(text).trim();
  if (!/^\d{1,3}$/.test(value)) return NaN;
  const number = Number(value);
  return number <= 100 ? number : NaN;
}

/** Plafond saisi : vide → null (aucun plafond), sinon un montant entier strictement positif (espaces tolérés). */
export function readCeiling(text) {
  const digits = String(text).replace(/\s/g, '');
  if (digits === '') return null;
  if (!/^\d+$/.test(digits)) return NaN;
  const number = Number(digits);
  return number > 0 ? number : NaN;
}

/** Plafond renvoyé par le serveur (« 150 000 XOF » ou « Aucun plafond ») → nombre, ou null. */
export function ceilingFromServer(text) {
  if (String(text).trim().toLowerCase() === NO_CEILING.toLowerCase()) return null;
  return Number(String(text).replace(/\D/g, ''));
}

/** « TIER 3 » → « Palier 3 ». */
export const tierName = (name) => `Palier ${String(name).replace(/\D/g, '')}`;
