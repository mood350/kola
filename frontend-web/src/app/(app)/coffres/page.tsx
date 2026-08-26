"use client";

import Link from "next/link";
import { useCallback, useState } from "react";
import { useSession } from "@/lib/session";
import { vaultApi } from "@/lib/services";
import { describeActionError, useResource } from "@/lib/use-resource";
import {
  formatAmount,
  formatCountdown,
  formatDate,
  progressRatio,
  toDateInputValue,
} from "@/lib/format";
import { VAULT_STATUS_LABEL, VAULT_STATUS_TONE } from "@/lib/labels";
import {
  Alert,
  Badge,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  LoadError,
  Modal,
  ProgressBar,
  SkeletonList,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { AmountField, parseAmount } from "@/components/money/amount-field";
import { NoWalletNotice, WalletPicker } from "@/components/money/wallet-picker";
import { ChevronRightIcon, PlusIcon, VaultIcon } from "@/components/ui/icons";
import type { Vault } from "@/lib/types";

/**
 * Coffres d'épargne.
 *
 * Un coffre BLOQUE une part du wallet jusqu'à une date choisie : l'argent reste
 * la propriété de l'utilisateur mais quitte le solde disponible. C'est la seule
 * chose qu'il faut avoir comprise avant d'en créer un — d'où le rappel dans le
 * formulaire, et non dans une aide qu'on n'ouvre jamais.
 *
 * Les coffres ne sont jamais supprimés (politique du backend, cf. `User.java`) :
 * ils passent de ACTIVE à UNLOCKED ou CLOSED. La liste montre donc aussi les
 * coffres terminés, plutôt que de les faire disparaître comme si l'épargne
 * n'avait pas existé.
 */
export default function VaultsPage() {
  const { activeWallet, reloadWallets } = useSession();
  const load = useCallback((signal: AbortSignal) => vaultApi.list(signal), []);
  const vaults = useResource(load);
  const [creating, setCreating] = useState(false);

  const active = (vaults.data ?? []).filter((vault) => vault.status === "ACTIVE");
  const archived = (vaults.data ?? []).filter((vault) => vault.status !== "ACTIVE");
  const totalSaved = active.reduce((total, vault) => total + vault.currentAmount, 0);

  return (
    <>
      <PageHeader
        title="Coffres d'épargne"
        description={
          active.length > 0
            ? `${formatAmount(totalSaved)} bloqués dans ${active.length} coffre(s)`
            : "Bloquez une somme jusqu'à la date de votre choix."
        }
        action={
          activeWallet ? (
            <Button icon={<PlusIcon />} onClick={() => setCreating(true)}>
              Nouveau coffre
            </Button>
          ) : null
        }
      />

      {!activeWallet ? (
        <NoWalletNotice />
      ) : vaults.loading ? (
        <SkeletonList rows={3} />
      ) : vaults.error ? (
        <LoadError message={vaults.error} onRetry={vaults.reload} />
      ) : vaults.data && vaults.data.length > 0 ? (
        <div className="space-y-6">
          <section className="space-y-3">
            {active.map((vault) => (
              <VaultCard key={vault.id} vault={vault} />
            ))}
            {active.length === 0 ? (
              <p className="text-sm text-ink-500">Aucun coffre actif pour le moment.</p>
            ) : null}
          </section>

          {archived.length > 0 ? (
            <section>
              <h2 className="mb-3 font-display text-sm font-semibold tracking-wide text-ink-500 uppercase">
                Coffres terminés
              </h2>
              <div className="space-y-3">
                {archived.map((vault) => (
                  <VaultCard key={vault.id} vault={vault} />
                ))}
              </div>
            </section>
          ) : null}
        </div>
      ) : (
        <EmptyState
          icon={<VaultIcon />}
          title="Aucun coffre"
          description="Un coffre met une somme de côté et la bloque jusqu'à la date que vous fixez — pour ne pas y toucher avant."
          action={<Button onClick={() => setCreating(true)}>Créer mon premier coffre</Button>}
        />
      )}

      <CreateVaultModal
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={() => {
          setCreating(false);
          vaults.reload();
          /* Un coffre prélève sur le wallet : le solde affiché ailleurs doit
             suivre immédiatement. */
          void reloadWallets();
        }}
      />
    </>
  );
}

function VaultCard({ vault }: { vault: Vault }) {
  const ratio = progressRatio(vault.currentAmount, vault.targetAmount);

  return (
    <Link href={`/coffres/${vault.id}`} className="block">
      <Card className="transition-colors hover:bg-ink-50">
        <div className="flex items-start gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-kola-50 text-lg text-kola-600">
            <VaultIcon />
          </span>

          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <p className="truncate font-semibold text-ink-900">{vault.name}</p>
              <Badge tone={VAULT_STATUS_TONE[vault.status]}>
                {VAULT_STATUS_LABEL[vault.status]}
              </Badge>
            </div>
            {vault.purpose ? (
              <p className="truncate text-sm text-ink-500">{vault.purpose}</p>
            ) : null}
          </div>

          <ChevronRightIcon className="mt-2 shrink-0 text-ink-400" />
        </div>

        <div className="mt-4">
          <div className="flex items-baseline justify-between gap-3">
            <p className="tabular font-display text-xl font-semibold text-ink-950">
              {formatAmount(vault.currentAmount)}
            </p>
            {vault.targetAmount ? (
              <p className="tabular text-sm text-ink-500">
                sur {formatAmount(vault.targetAmount)}
              </p>
            ) : null}
          </div>

          {ratio !== null ? (
            <ProgressBar
              ratio={ratio}
              tone={vault.status === "ACTIVE" ? "brand" : "positive"}
              className="mt-2"
            />
          ) : null}

          {vault.unlockDate ? (
            <p className="mt-2 text-xs text-ink-500">
              {vault.status === "ACTIVE"
                ? `Déblocage ${formatCountdown(vault.unlockDate)} · ${formatDate(vault.unlockDate)}`
                : `Échéance : ${formatDate(vault.unlockDate)}`}
            </p>
          ) : null}
        </div>
      </Card>
    </Link>
  );
}

/**
 * Création d'un coffre.
 *
 * La date de déblocage doit être dans le FUTUR (`@Future` côté backend) : le
 * champ est donc borné au lendemain. La refuser ici évite un aller-retour dont
 * le message d'erreur serait moins clair que la contrainte du champ lui-même.
 */
function CreateVaultModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}) {
  const { activeWallet } = useSession();

  const [name, setName] = useState("");
  const [purpose, setPurpose] = useState("");
  const [target, setTarget] = useState("");
  const [initial, setInitial] = useState("");
  const [unlockDate, setUnlockDate] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const minDate = toDateInputValue(tomorrow);

  const available = activeWallet?.availableBalance ?? 0;
  /* Un coffre peut naître vide (le backend accepte 0) et être alimenté plus
     tard : c'est le cas de celui qu'on ouvre avant d'avoir l'argent. */
  const initialAmount = parseAmount(initial, { min: 0, max: available }) ?? 0;
  const targetAmount = target ? parseAmount(target, { min: 1 }) : null;

  const reset = () => {
    setName("");
    setPurpose("");
    setTarget("");
    setInitial("");
    setUnlockDate("");
    setError(null);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!activeWallet || !name.trim()) return;

    setError(null);
    setSubmitting(true);
    try {
      await vaultApi.create({
        walletId: activeWallet.id,
        name: name.trim(),
        purpose: purpose.trim() || undefined,
        targetAmount: targetAmount ?? undefined,
        initialAmount,
        unlockDate: unlockDate || undefined,
      });
      reset();
      onCreated();
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Nouveau coffre">
      <form id="create-vault" onSubmit={handleSubmit} className="space-y-4" noValidate>
        {error ? <Alert tone="danger">{error}</Alert> : null}

        <WalletPicker label="Compte source" />

        <Field label="Nom du coffre" htmlFor="vault-name">
          <Input
            id="vault-name"
            required
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Ex : Rentrée scolaire"
          />
        </Field>

        <Field label="Objectif" htmlFor="vault-purpose" hint="Facultatif.">
          <Input
            id="vault-purpose"
            value={purpose}
            onChange={(event) => setPurpose(event.target.value)}
            placeholder="À quoi cette épargne servira-t-elle ?"
          />
        </Field>

        <AmountField
          id="vault-initial"
          label="Montant initial"
          value={initial}
          onChange={setInitial}
          min={0}
          max={available}
          helper={`Prélevé sur votre solde disponible (${formatAmount(available)}). Peut être nul.`}
        />

        <AmountField
          id="vault-target"
          label="Objectif d'épargne"
          value={target}
          onChange={setTarget}
          helper="Facultatif — sert à afficher votre progression."
        />

        <Field
          label="Date de déblocage"
          htmlFor="vault-date"
          hint="Facultatif. Avant cette date, les fonds ne sont pas disponibles."
        >
          <Input
            id="vault-date"
            type="date"
            min={minDate}
            value={unlockDate}
            onChange={(event) => setUnlockDate(event.target.value)}
          />
        </Field>

        <Alert tone="info">
          Le montant initial quitte votre solde disponible dès la création du
          coffre. Il reste à vous, mais n&apos;est plus utilisable pour un
          transfert ou un retrait.
        </Alert>
      </form>

      <div className="mt-5 flex justify-end gap-2">
        <Button variant="ghost" onClick={onClose} type="button">
          Annuler
        </Button>
        <Button
          type="submit"
          form="create-vault"
          loading={submitting}
          disabled={!name.trim()}
        >
          Créer le coffre
        </Button>
      </div>
    </Modal>
  );
}
