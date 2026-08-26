/**
 * Pays proposés à l'inscription et à l'ajout d'un bénéficiaire.
 *
 * Le backend attend DEUX valeurs distinctes et ne les déduit pas l'une de
 * l'autre : `countryCode` au format ISO 3166-1 alpha-2 (« TG ») et
 * `phoneNumber` au format international complet (« +22890000000 »), chacun
 * validé par sa propre expression régulière. Cette table est ce qui permet à
 * l'interface de proposer un pays et de pré-remplir l'indicatif correspondant,
 * sans laisser l'utilisateur composer lui-même deux champs qui doivent
 * s'accorder.
 *
 * La liste couvre l'Afrique de l'Ouest — le marché de Kola (devise XOF) — plus
 * le Nigeria et le Ghana, où les réseaux acceptés par
 * `MobileNetwork` (OPay, PalmPay, MTN MoMo, Vodafone Cash, AirtelTigo) sont
 * établis. En ajouter un ici suffit : aucun autre fichier ne les énumère.
 */

export type Country = {
  /** ISO 3166-1 alpha-2, envoyé tel quel dans `countryCode`. */
  code: string;
  name: string;
  /** Indicatif international, préfixe du champ `phoneNumber`. */
  dial: string;
};

export const COUNTRIES: Country[] = [
  { code: "TG", name: "Togo", dial: "+228" },
  { code: "BJ", name: "Bénin", dial: "+229" },
  { code: "BF", name: "Burkina Faso", dial: "+226" },
  { code: "CI", name: "Côte d'Ivoire", dial: "+225" },
  { code: "GH", name: "Ghana", dial: "+233" },
  { code: "GW", name: "Guinée-Bissau", dial: "+245" },
  { code: "ML", name: "Mali", dial: "+223" },
  { code: "NE", name: "Niger", dial: "+227" },
  { code: "NG", name: "Nigeria", dial: "+234" },
  { code: "SN", name: "Sénégal", dial: "+221" },
];

export const DEFAULT_COUNTRY = "TG";

export function dialCodeFor(countryCode: string): string {
  return COUNTRIES.find((country) => country.code === countryCode)?.dial ?? "+";
}

/**
 * Format accepté par le backend : `^\+[1-9]\d{6,14}$`.
 *
 * Reproduit ici pour refuser le numéro AVANT l'envoi. Le backend le refuserait
 * de toute façon — mais après un aller-retour réseau, et avec un message
 * générique de validation, là où l'interface peut le dire sous le champ concerné.
 */
export const PHONE_PATTERN = /^\+[1-9]\d{6,14}$/;

export function isValidPhone(value: string): boolean {
  return PHONE_PATTERN.test(value.replace(/[\s.-]/g, ""));
}

/** Retire espaces et séparateurs : le backend n'accepte que des chiffres après le « + ». */
export function normalizePhone(value: string): string {
  return value.replace(/[\s.-]/g, "");
}

/**
 * Règles de mot de passe, identiques à celles de `RegistrationRequest` et
 * `ResetPasswordRequest` : 8 caractères minimum, une majuscule, une minuscule,
 * un chiffre. Les vérifier ici sert à guider la saisie, pas à autoriser quoi
 * que ce soit — le backend reste seul juge.
 */
export const PASSWORD_RULES = [
  { label: "8 caractères minimum", test: (value: string) => value.length >= 8 },
  { label: "une minuscule", test: (value: string) => /[a-z]/.test(value) },
  { label: "une majuscule", test: (value: string) => /[A-Z]/.test(value) },
  { label: "un chiffre", test: (value: string) => /\d/.test(value) },
];

export function isValidPassword(value: string): boolean {
  return PASSWORD_RULES.every((rule) => rule.test(value));
}
