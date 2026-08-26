import Badge from "@/components/ui/badge/Badge";
import {
  AML_RISK_LABELS,
  AML_STATUS_LABELS,
  KYC_LABELS,
  LOAN_STATUS_LABELS,
  TIER_LABELS,
  type AdminUserSummary,
  type AmlAlertStatus,
  type AmlRiskLevel,
  type CreditTier,
  type KycLevel,
  type LoanStatus,
} from "@/lib/types";

/**
 * Traduction des états métier en pastilles TailAdmin.
 *
 * CENTRALISÉ POUR UNE RAISON PRÉCISE : le même statut doit avoir la même
 * couleur partout. Si « en défaut » est rouge dans la liste des prêts et ambre
 * sur la fiche du client, l'opérateur cesse de se fier à la couleur — et la
 * couleur ne sert alors plus à rien.
 *
 * Le libellé écrit accompagne systématiquement la teinte. Une console
 * financière affiche des statuts dont la confusion coûte cher : « en défaut » et
 * « remboursé » ne doivent pas se distinguer par la seule couleur, ni pour un
 * opérateur daltonien, ni sur un écran mal calibré.
 */

type BadgeColor = React.ComponentProps<typeof Badge>["color"];

/* ---------------------------------------------------------------------------
   Vérification d'identité
   ------------------------------------------------------------------------ */

/**
 * Progression, pas jugement. Le palier KYC n'est ni bon ni mauvais : c'est un
 * niveau atteint. La rampe va donc du neutre au vert, sans passer par le rouge —
 * un compte TIER_0 n'a rien fait de mal.
 */
const KYC_COLORS: Record<KycLevel, BadgeColor> = {
  TIER_0: "light",
  TIER_1: "light",
  TIER_2: "primary",
  TIER_3: "success",
};

export function KycBadge({ level }: { level: KycLevel }) {
  return (
    <Badge size="sm" color={KYC_COLORS[level]}>
      {level.replace("TIER_", "N")} · {KYC_LABELS[level]}
    </Badge>
  );
}

/* ---------------------------------------------------------------------------
   État du compte
   ------------------------------------------------------------------------ */

/**
 * LA DISTINCTION QUI COMPTE ICI : un verrou AUTOMATIQUE (cinq mots de passe
 * erronés) porte un `lockedAt` et se lève seul au bout de trente minutes ; un
 * verrou ADMINISTRATIF a un `lockedAt` nul et ne se lève que par une action
 * humaine (cf. `AdminUserService.lock()`). Les confondre ferait attendre un
 * déblocage qui ne viendra jamais — ou intervenir sur un compte qui se serait
 * débloqué tout seul.
 */
export function AccountStateBadge({ user }: { user: AdminUserSummary }) {
  if (user.accountLocked) {
    return user.lockedAt === null ? (
      <Badge size="sm" color="error">
        Verrouillé (administratif)
      </Badge>
    ) : (
      <Badge size="sm" color="warning">
        Verrouillé (temporaire)
      </Badge>
    );
  }
  if (!user.enabled) {
    return (
      <Badge size="sm" color="warning">
        Non activé
      </Badge>
    );
  }
  return (
    <Badge size="sm" color="success">
      Actif
    </Badge>
  );
}

/* ---------------------------------------------------------------------------
   Prêts
   ------------------------------------------------------------------------ */

const LOAN_COLORS: Record<LoanStatus, BadgeColor> = {
  PENDING: "light",
  APPROVED: "primary",
  REJECTED: "light",
  DISBURSED: "info",
  REPAID: "success",
  DEFAULTED: "error",
};

export function LoanStatusBadge({ status }: { status: LoanStatus }) {
  return (
    <Badge size="sm" color={LOAN_COLORS[status]}>
      {LOAN_STATUS_LABELS[status]}
    </Badge>
  );
}

/**
 * Palier de crédit — magnitude ordonnée, pas catégorie. Seul INELIGIBLE sort de
 * la rampe : il ne désigne pas un niveau, mais l'absence de niveau.
 */
const TIER_COLORS: Record<CreditTier, BadgeColor> = {
  INELIGIBLE: "light",
  BASIC: "info",
  STANDARD: "primary",
  PREMIUM: "primary",
  ELITE: "success",
};

export function TierBadge({ tier }: { tier: CreditTier }) {
  return (
    <Badge size="sm" color={TIER_COLORS[tier]}>
      {TIER_LABELS[tier]}
    </Badge>
  );
}

/* ---------------------------------------------------------------------------
   Conformité LAB-FT
   ------------------------------------------------------------------------ */

/**
 * OPEN est en ambre et non en neutre : une alerte ouverte est un travail en
 * attente, pas un état de repos. CONFIRMED est en rouge — le soupçon est retenu
 * et doit être transmis à la cellule de renseignement financier (CENTIF dans
 * l'espace UEMOA), c'est l'issue la plus lourde du cycle.
 */
const AML_STATUS_COLORS: Record<AmlAlertStatus, BadgeColor> = {
  OPEN: "warning",
  REVIEWING: "primary",
  CLEARED: "success",
  CONFIRMED: "error",
};

export function AmlStatusBadge({ status }: { status: AmlAlertStatus }) {
  return (
    <Badge size="sm" color={AML_STATUS_COLORS[status]}>
      {AML_STATUS_LABELS[status]}
    </Badge>
  );
}

/** La seule échelle de ce fichier où le rouge signifie vraiment « danger ». */
const AML_RISK_COLORS: Record<AmlRiskLevel, BadgeColor> = {
  LOW: "light",
  MEDIUM: "info",
  HIGH: "warning",
  CRITICAL: "error",
};

export function RiskBadge({
  level,
  score,
}: {
  level: AmlRiskLevel;
  score?: number;
}) {
  return (
    <Badge size="sm" color={AML_RISK_COLORS[level]}>
      {AML_RISK_LABELS[level]}
      {score !== undefined ? ` · ${score}` : ""}
    </Badge>
  );
}
