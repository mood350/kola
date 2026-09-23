"use client";

import Link from "next/link";
import { useSession } from "@/lib/session";
import { formatDate, initials } from "@/lib/format";
import { KYC_LABEL, KYC_TONE } from "@/lib/labels";
import { Avatar } from "@/components/ui/avatar";
import {
  Alert,
  Button,
  Card,
  DetailRow,
  DotBadge,
  Skeleton,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import {
  ChevronRightIcon,
  ClockIcon,
  EditIcon,
  KeyIcon,
  LogoutIcon,
  ShieldIcon,
  UsersIcon,
  WalletIcon,
} from "@/components/ui/icons";

/**
 * Profil.
 *
 * Deux blocs : l'identité (ce que le compte est) et les réglages (ce qu'on
 * peut y faire). Les entrées reprennent celles du mobile, à l'identique —
 * informations, mot de passe, bénéficiaires, virements programmés — pour que
 * chercher un réglage au même endroit fonctionne d'un appareil à l'autre.
 *
 * La dernière connexion connue est affichée sans commentaire rassurant : c'est
 * la seule information de cette page qui peut révéler un accès qu'on n'a pas
 * fait soi-même, et l'habiller de « tout va bien » lui retirerait sa fonction.
 */

const MENU = [
  {
    href: "/profil/modifier",
    label: "Modifier mes informations",
    description: "Nom, téléphone, avatar",
    icon: EditIcon,
  },
  {
    href: "/profil/mot-de-passe",
    label: "Changer mon mot de passe",
    description: "Sécurité du compte",
    icon: KeyIcon,
  },
  {
    href: "/beneficiaires",
    label: "Mes bénéficiaires",
    description: "Ajouter ou retirer un destinataire",
    icon: UsersIcon,
  },
  {
    href: "/virements-programmes",
    label: "Virements programmés",
    description: "Automatiser mon épargne",
    icon: ClockIcon,
  },
  {
    href: "/comptes",
    label: "Mes comptes",
    description: "Soldes et devises",
    icon: WalletIcon,
  },
];

export default function ProfilePage() {
  const { user, logout } = useSession();

  return (
    <>
      <PageHeader title="Profil" />

      {!user ? (
        <Card className="space-y-3">
          <Skeleton className="h-14 w-14 rounded-full" />
          <Skeleton className="h-5 w-1/2" />
          <Skeleton className="h-4 w-2/3" />
        </Card>
      ) : (
        <div className="space-y-5">
          <Card>
            <div className="flex items-center gap-4">
              <Avatar
                avatarId={user.avatar}
                initials={initials(user.firstName, user.lastName)}
                size={64}
              />
              <div className="min-w-0 flex-1">
                <p className="truncate font-display text-xl font-semibold text-ink-950">
                  {user.firstName} {user.lastName}
                </p>
                <p className="truncate text-sm text-ink-500">{user.email}</p>
                <div className="mt-2">
                  <DotBadge tone={KYC_TONE[user.kycLevel]}>
                    {KYC_LABEL[user.kycLevel]}
                  </DotBadge>
                </div>
              </div>
            </div>

            <dl className="mt-5">
              <DetailRow label="Téléphone" value={user.phoneNumber} />
              <DetailRow label="Pays" value={user.countryCode} />
              {user.createdAt ? (
                <DetailRow label="Client depuis" value={formatDate(user.createdAt)} />
              ) : null}
            </dl>
          </Card>

          {user.kycLevel === "TIER_0" ? (
            <Alert tone="warning" title="Vérification à compléter">
              Votre compte est au niveau 0 : les plafonds d&apos;opération sont
              les plus bas, et le niveau de vérification pèse dans le calcul de
              votre score de crédit. La vérification se fait auprès du service
              client.
            </Alert>
          ) : null}

          <nav className="space-y-2" aria-label="Réglages">
            {MENU.map((item) => (
              <Link key={item.href} href={item.href} className="block">
                <Card className="flex items-center gap-3 py-4 transition-colors hover:bg-ink-50">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-ink-100 text-lg text-ink-600">
                    <item.icon />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-ink-900">{item.label}</p>
                    <p className="truncate text-xs text-ink-500">{item.description}</p>
                  </div>
                  <ChevronRightIcon className="shrink-0 text-ink-400" />
                </Card>
              </Link>
            ))}
          </nav>

          <Card>
            <h2 className="flex items-center gap-2 font-display text-sm font-semibold tracking-wide text-ink-500 uppercase">
              <ShieldIcon className="text-base" />
              Dernière connexion connue
            </h2>
            <dl className="mt-3">
              <DetailRow label="Adresse IP" value={user.lastKnownIp ?? "—"} mono />
              <DetailRow
                label="Appareil"
                value={
                  <span className="line-clamp-2 break-all">
                    {user.lastKnownUserAgent ?? "—"}
                  </span>
                }
              />
            </dl>
            <p className="mt-3 text-xs text-ink-500">
              Si vous ne reconnaissez pas cet appareil, changez votre mot de
              passe et contactez le service client.
            </p>
          </Card>

          <Button
            variant="secondary"
            full
            size="lg"
            icon={<LogoutIcon />}
            onClick={() => void logout()}
          >
            Se déconnecter
          </Button>
        </div>
      )}
    </>
  );
}
