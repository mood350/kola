"use client";

import { useState } from "react";
import { useSession } from "@/lib/session";
import { walletApi } from "@/lib/services";
import { describeActionError } from "@/lib/use-resource";
import { formatAmount } from "@/lib/format";
import {
  Alert,
  Badge,
  Button,
  Card,
  DetailRow,
  Modal,
  SkeletonList,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { PlusIcon, WalletIcon } from "@/components/ui/icons";

/**
 * Comptes (wallets).
 *
 * Un compte par devise. Le sélecteur affiché en tête des écrans d'opération
 * puise dans cette liste ; ici, on voit les trois soldes que le backend
 * distingue — total, bloqué, disponible — et l'écart entre eux, qui est la
 * question la plus fréquemment posée à un support financier.
 *
 * UN COMPTE NE SE SUPPRIME PAS : la politique du backend est la désactivation
 * (`active = false`), parce que le grand livre continue de le référencer. Aucun
 * bouton ne promet donc le contraire.
 */
export default function WalletsPage() {
  const { wallets, activeWallet, selectWallet, reloadWallets, status } = useSession();

  const [creating, setCreating] = useState(false);
  const [currency, setCurrency] = useState("XOF");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const create = async () => {
    setError(null);
    setSubmitting(true);
    try {
      await walletApi.create(currency);
      setCreating(false);
      await reloadWallets();
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Mes comptes"
        description="Un compte par devise."
        action={
          <Button icon={<PlusIcon />} onClick={() => setCreating(true)}>
            Nouveau compte
          </Button>
        }
      />

      {error ? (
        <div className="mb-4">
          <Alert tone="danger" onDismiss={() => setError(null)}>
            {error}
          </Alert>
        </div>
      ) : null}

      {status === "loading" ? (
        <SkeletonList rows={2} />
      ) : wallets.length === 0 ? (
        <Card>
          <p className="text-sm text-ink-600">
            Vous n&apos;avez encore aucun compte. Créez-en un pour déposer de
            l&apos;argent, envoyer des transferts et ouvrir des coffres
            d&apos;épargne.
          </p>
          <Button className="mt-4" onClick={() => setCreating(true)}>
            Créer mon compte
          </Button>
        </Card>
      ) : (
        <ul className="space-y-3">
          {wallets.map((wallet) => {
            const current = wallet.id === activeWallet?.id;
            return (
              <li key={wallet.id}>
                <Card selected={current}>
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <span className="flex size-10 items-center justify-center rounded-full bg-kola-50 text-lg text-kola-600">
                        <WalletIcon />
                      </span>
                      <div>
                        <p className="font-display text-lg font-semibold text-ink-950">
                          Compte {wallet.currency}
                        </p>
                        <p className="text-xs text-ink-500">Compte #{wallet.id}</p>
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-2">
                      <Badge tone={wallet.active ? "positive" : "neutral"}>
                        {wallet.active ? "Actif" : "Inactif"}
                      </Badge>
                      {current ? <Badge tone="brand">Compte affiché</Badge> : null}
                    </div>
                  </div>

                  <dl className="mt-4">
                    <DetailRow label="Solde disponible" value={formatAmount(wallet.availableBalance)} />
                    <DetailRow label="Bloqué en coffres" value={formatAmount(wallet.lockedBalance)} />
                    <DetailRow label="Solde total" value={formatAmount(wallet.balance)} />
                  </dl>

                  {!current ? (
                    <Button
                      variant="secondary"
                      size="sm"
                      className="mt-4"
                      onClick={() => selectWallet(wallet.id)}
                    >
                      Utiliser ce compte
                    </Button>
                  ) : null}
                </Card>
              </li>
            );
          })}
        </ul>
      )}

      <Modal open={creating} onClose={() => setCreating(false)} title="Nouveau compte">
        <label htmlFor="currency" className="block text-sm font-medium text-ink-800">
          Devise
        </label>
        <select
          id="currency"
          value={currency}
          onChange={(event) => setCurrency(event.target.value)}
          className="mt-1.5 w-full rounded-field border border-line-strong bg-white px-3.5 py-2.5 text-ink-900 focus:border-kola-500 focus:ring-2 focus:ring-kola-100 focus:outline-none"
        >
          {/* Code ISO 4217 sur trois lettres — le backend valide le format
              (`CreateWalletRequest`). XOF est la devise du produit ; les autres
              sont proposées pour les comptes multi-devises. */}
          {["XOF", "XAF", "GHS", "NGN", "EUR", "USD"].map((code) => (
            <option key={code} value={code}>
              {code}
            </option>
          ))}
        </select>

        <p className="mt-3 text-sm text-ink-500">
          Un compte par devise. Vous choisirez celui à débiter au moment de
          chaque opération.
        </p>

        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setCreating(false)}>
            Annuler
          </Button>
          <Button loading={submitting} onClick={() => void create()}>
            Créer le compte
          </Button>
        </div>
      </Modal>
    </>
  );
}
