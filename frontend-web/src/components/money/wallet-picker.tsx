"use client";

import Link from "next/link";
import { useSyncExternalStore } from "react";
import { formatAmount } from "@/lib/format";
import { useSession } from "@/lib/session";
import { Card, EmptyState, Select } from "@/components/ui/primitives";
import { EyeIcon, EyeOffIcon, WalletIcon } from "@/components/ui/icons";
import type { Wallet } from "@/lib/types";

/**
 * Sélecteur du wallet sur lequel porte une opération.
 *
 * IL DISPARAÎT QUAND IL N'A RIEN À CHOISIR. Un compte n'a le plus souvent qu'un
 * seul wallet ; afficher une liste déroulante à une entrée ajoute une décision
 * là où il n'y en a aucune. Avec deux wallets ou plus, il devient au contraire
 * l'information la plus importante de l'écran — c'est lui qui dit quel solde va
 * bouger — et le solde disponible est rappelé à côté.
 */
export function WalletPicker({
  label = "Compte",
  onWalletChange,
}: {
  label?: string;
  /** Notifie l'écran d'un changement de compte — une liste paginée doit
      revenir à sa première page, sans quoi elle afficherait une page 4 qui
      n'existe pas sur le nouveau compte. */
  onWalletChange?: (walletId: number) => void;
}) {
  const { wallets, activeWallet, selectWallet } = useSession();

  if (!activeWallet) return null;

  if (wallets.length === 1) {
    return (
      <div className="flex items-center justify-between rounded-field bg-ink-50 px-4 py-3">
        <span className="flex items-center gap-2 text-sm text-ink-600">
          <WalletIcon className="text-lg" />
          {label} {activeWallet.currency}
        </span>
        <span className="tabular text-sm font-semibold text-ink-900">
          {formatAmount(activeWallet.availableBalance)}
        </span>
      </div>
    );
  }

  return (
    <div className="space-y-1.5">
      <label htmlFor="wallet" className="block text-sm font-medium text-ink-800">
        {label}
      </label>
      <Select
        id="wallet"
        value={activeWallet.id}
        onChange={(event) => {
          const id = Number(event.target.value);
          selectWallet(id);
          onWalletChange?.(id);
        }}
      >
        {wallets.map((wallet) => (
          <option key={wallet.id} value={wallet.id}>
            {wallet.currency} · {formatAmount(wallet.availableBalance)} disponible
            {wallet.active ? "" : " (inactif)"}
          </option>
        ))}
      </Select>
    </div>
  );
}

/**
 * Écran de repli quand le compte n'a aucun wallet.
 *
 * Le cas n'est pas théorique : un compte fraîchement activé peut n'en avoir
 * aucun. Une page d'opération vide, sans explication, laisserait croire à une
 * panne — alors qu'il manque une action, et une seule.
 */
export function NoWalletNotice() {
  return (
    <EmptyState
      icon={<WalletIcon />}
      title="Aucun compte disponible"
      description="Créez d'abord un compte Kola pour déposer, envoyer ou épargner de l'argent."
      action={
        <Link
          href="/comptes"
          className="inline-flex h-11 items-center justify-center rounded-full bg-kola-600 px-5 text-sm font-semibold text-white hover:bg-kola-700"
        >
          Créer un compte
        </Link>
      }
    />
  );
}

/* ═══ LE MASQUAGE DU SOLDE, MÉMORISÉ LOCALEMENT ═══

   Le choix survit au rechargement : quelqu'un qui consulte son compte dans un
   lieu public ne veut pas re-cliquer à chaque page. La valeur reste dans le
   navigateur, n'est jamais envoyée, et MASQUER N'EST PAS PROTÉGER — le montant
   transite et reste lisible dans l'onglet réseau. C'est un cache-œil contre les
   regards par-dessus l'épaule, pas un secret.

   Un petit magasin externe plutôt qu'un état local, pour deux raisons :
   `useSyncExternalStore` lit le navigateur sans provoquer la divergence
   d'hydratation qu'un état initialisé depuis `localStorage` créerait ; et les
   abonnés étant partagés, les deux cartes de solde (accueil et page Comptes)
   basculent ensemble au lieu de se contredire. */
const HIDDEN_KEY = "kola_web_hide_balance";

const listeners = new Set<() => void>();

function subscribeHidden(onChange: () => void) {
  listeners.add(onChange);
  return () => {
    listeners.delete(onChange);
  };
}

function readHidden(): boolean {
  try {
    return window.localStorage.getItem(HIDDEN_KEY) === "1";
  } catch {
    /* Navigation privée, stockage bloqué : on reste visible. */
    return false;
  }
}

/** Le serveur ne masque jamais : c'est ce qui fait coïncider HTML et hydratation. */
function readHiddenOnServer(): boolean {
  return false;
}

function writeHidden(next: boolean) {
  try {
    window.localStorage.setItem(HIDDEN_KEY, next ? "1" : "0");
  } catch {
    /* Le masquage vaut alors pour cette page seulement. */
  }
  listeners.forEach((notify) => notify());
}

/** Carte de solde réutilisée par l'accueil et la page des comptes. */
export function WalletBalanceCard({ wallet }: { wallet: Wallet }) {
  const hidden = useSyncExternalStore(subscribeHidden, readHidden, readHiddenOnServer);
  const toggle = () => writeHidden(!hidden);

  /* Une longueur fixe, sans rapport avec le montant : des points au nombre des
     chiffres laisseraient deviner l'ordre de grandeur. */
  const masque = "•••••••";

  return (
    <Card variant="brand">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-kola-100">Solde disponible · {wallet.currency}</p>
        <button
          type="button"
          onClick={toggle}
          aria-pressed={hidden}
          aria-label={hidden ? "Afficher le solde" : "Masquer le solde"}
          title={hidden ? "Afficher le solde" : "Masquer le solde"}
          className="-mr-1 inline-flex size-9 shrink-0 items-center justify-center rounded-full text-kola-100 transition-colors hover:bg-white/15 hover:text-white"
        >
          {hidden ? <EyeOffIcon className="text-lg" /> : <EyeIcon className="text-lg" />}
        </button>
      </div>
      <p className="tabular mt-1 font-display text-3xl font-semibold sm:text-4xl">
        {hidden ? masque : formatAmount(wallet.availableBalance)}
      </p>

      {/* Le solde bloqué n'est affiché que s'il existe : une ligne « 0 F
          bloqué » sur un compte sans coffre est du bruit. Quand il existe, en
          revanche, il explique l'écart entre le total et le disponible — sans
          quoi l'utilisateur croit à une erreur de calcul. */}
      {wallet.lockedBalance > 0 ? (
        <div className="mt-4 flex gap-6 border-t border-white/15 pt-3 text-sm">
          <div>
            <p className="text-kola-200">Total</p>
            <p className="tabular font-semibold">
              {hidden ? masque : formatAmount(wallet.balance)}
            </p>
          </div>
          <div>
            <p className="text-kola-200">Bloqué en coffres</p>
            <p className="tabular font-semibold">
              {hidden ? masque : formatAmount(wallet.lockedBalance)}
            </p>
          </div>
        </div>
      ) : null}

      {!wallet.active ? (
        <p className="mt-3 rounded-field bg-white/15 px-3 py-2 text-xs">
          Ce compte est inactif : aucune opération n&apos;est possible dessus.
        </p>
      ) : null}
    </Card>
  );
}
