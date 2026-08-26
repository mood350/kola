"use client";

import { use, useCallback, useState } from "react";
import { useSession } from "@/lib/session";
import { vaultApi } from "@/lib/services";
import { describeActionError, useResource } from "@/lib/use-resource";
import {
  formatAmount,
  formatCountdown,
  formatDate,
  isDueOrPast,
  progressRatio,
} from "@/lib/format";
import { VAULT_STATUS_LABEL, VAULT_STATUS_TONE } from "@/lib/labels";
import {
  Alert,
  Badge,
  Button,
  Card,
  DetailRow,
  LoadError,
  Modal,
  ProgressBar,
  Skeleton,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { AmountField, parseAmount } from "@/components/money/amount-field";
import { LockIcon } from "@/components/ui/icons";

/**
 * Détail d'un coffre.
 *
 * Trois actions, et la distinction entre les deux dernières est le point à ne
 * pas rater :
 *
 * — ALIMENTER : déplace de l'argent du wallet vers le coffre.
 * — DÉBLOQUER : rend les fonds au wallet. Le backend REFUSE avant l'échéance
 *   (`VaultLockedException`) — c'est tout l'intérêt d'un coffre.
 * — FERMER PAR ANTICIPATION : la sortie de secours avant l'échéance. Elle
 *   existe (personne ne doit être prisonnier de son épargne) mais elle est
 *   présentée comme telle : bouton discret, confirmation explicite, et rappel
 *   que la discipline d'épargne compte dans le score de crédit
 *   (`ScoringRule.VAULT_DISCIPLINE`).
 */
export default function VaultDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const vaultId = Number(id);
  const { reloadWallets } = useSession();

  const load = useCallback(
    (signal: AbortSignal) => vaultApi.get(vaultId, signal),
    [vaultId]
  );
  const resource = useResource(load);
  const vault = resource.data;

  const [action, setAction] = useState<"add" | "unlock" | "close" | null>(null);
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const run = async (task: () => Promise<unknown>) => {
    setError(null);
    setSubmitting(true);
    try {
      await task();
      setAction(null);
      setAmount("");
      resource.reload();
      await reloadWallets();
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setSubmitting(false);
    }
  };

  const ratio = vault ? progressRatio(vault.currentAmount, vault.targetAmount) : null;
  /* Sans date de déblocage, le coffre est déblocable à tout moment : c'est ce
     que renvoie `isDueOrPast` pour une valeur absente. */
  const matured = isDueOrPast(vault?.unlockDate);
  const addValue = parseAmount(amount, { min: 1 });

  return (
    <>
      <PageHeader
        title={vault?.name ?? "Coffre"}
        description={vault?.purpose ?? undefined}
        backHref="/coffres"
        backLabel="Coffres"
      />

      {resource.loading ? (
        <Card className="space-y-4">
          <Skeleton className="h-8 w-40" />
          <Skeleton className="h-2 w-full" />
          <Skeleton className="h-4 w-2/3" />
        </Card>
      ) : resource.error ? (
        <LoadError message={resource.error} onRetry={resource.reload} />
      ) : vault ? (
        <div className="space-y-5">
          {error ? <Alert tone="danger">{error}</Alert> : null}

          <Card>
            <div className="flex items-center justify-between gap-3">
              <span className="flex items-center gap-2 text-sm text-ink-500">
                <LockIcon className="text-lg text-kola-600" />
                Épargne bloquée
              </span>
              <Badge tone={VAULT_STATUS_TONE[vault.status]}>
                {VAULT_STATUS_LABEL[vault.status]}
              </Badge>
            </div>

            <p className="tabular mt-3 font-display text-3xl font-semibold text-ink-950">
              {formatAmount(vault.currentAmount)}
            </p>

            {ratio !== null ? (
              <div className="mt-3">
                <ProgressBar ratio={ratio} tone={vault.status === "ACTIVE" ? "brand" : "positive"} />
                <p className="mt-1.5 text-xs text-ink-500">
                  {Math.round(ratio * 100)} % de l&apos;objectif de{" "}
                  {formatAmount(vault.targetAmount)}
                </p>
              </div>
            ) : null}

            <dl className="mt-5">
              <DetailRow label="Devise" value={vault.currency} />
              {vault.unlockDate ? (
                <DetailRow
                  label="Date de déblocage"
                  value={`${formatDate(vault.unlockDate)} (${formatCountdown(vault.unlockDate)})`}
                />
              ) : (
                <DetailRow label="Date de déblocage" value="Aucune — déblocable à tout moment" />
              )}
              <DetailRow label="Compte associé" value={`#${vault.walletId}`} />
            </dl>
          </Card>

          {vault.status === "ACTIVE" ? (
            <div className="space-y-3">
              <div className="flex flex-col gap-2 sm:flex-row">
                <Button full onClick={() => setAction("add")}>
                  Alimenter
                </Button>
                <Button
                  full
                  variant="secondary"
                  onClick={() => setAction("unlock")}
                  disabled={!matured}
                >
                  Débloquer
                </Button>
              </div>

              {!matured ? (
                <Alert tone="info">
                  Les fonds seront déblocables {formatCountdown(vault.unlockDate)}. En
                  cas de besoin urgent, vous pouvez fermer ce coffre par
                  anticipation.
                </Alert>
              ) : null}

              <button
                type="button"
                onClick={() => setAction("close")}
                className="w-full py-2 text-sm font-semibold text-danger-600 hover:underline"
              >
                Fermer ce coffre par anticipation
              </button>
            </div>
          ) : (
            <Alert tone="neutral">
              Ce coffre est {VAULT_STATUS_LABEL[vault.status].toLowerCase()} : il
              n&apos;accepte plus d&apos;opération. Les fonds ont été rendus à
              votre compte.
            </Alert>
          )}
        </div>
      ) : null}

      {/* ---- Alimenter ---- */}
      <Modal open={action === "add"} onClose={() => setAction(null)} title="Alimenter le coffre">
        <AmountField
          value={amount}
          onChange={setAmount}
          id="vault-add"
          label="Montant à ajouter"
          helper="Prélevé sur le solde disponible de votre compte."
        />
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setAction(null)}>
            Annuler
          </Button>
          <Button
            loading={submitting}
            disabled={addValue === null}
            onClick={() => {
              if (addValue !== null) void run(() => vaultApi.addFunds(vaultId, addValue));
            }}
          >
            Ajouter
          </Button>
        </div>
      </Modal>

      {/* ---- Débloquer ---- */}
      <Modal open={action === "unlock"} onClose={() => setAction(null)} title="Débloquer le coffre">
        <p className="text-sm text-ink-600">
          {vault ? formatAmount(vault.currentAmount) : ""} seront rendus au solde
          disponible de votre compte, et ce coffre sera clôturé.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setAction(null)}>
            Annuler
          </Button>
          <Button loading={submitting} onClick={() => void run(() => vaultApi.unlock(vaultId))}>
            Débloquer
          </Button>
        </div>
      </Modal>

      {/* ---- Fermeture anticipée ---- */}
      <Modal
        open={action === "close"}
        onClose={() => setAction(null)}
        title="Fermer par anticipation"
      >
        <Alert tone="warning" title="Avant de confirmer">
          La régularité de votre épargne entre dans le calcul de votre score de
          crédit. Fermer un coffre avant son échéance peut le faire baisser, et
          donc réduire le montant que vous pouvez emprunter.
        </Alert>
        <p className="mt-3 text-sm text-ink-600">
          {vault ? formatAmount(vault.currentAmount) : ""} seront rendus à votre
          compte immédiatement.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setAction(null)}>
            Annuler
          </Button>
          <Button
            variant="danger"
            loading={submitting}
            onClick={() => void run(() => vaultApi.closeEarly(vaultId))}
          >
            Fermer le coffre
          </Button>
        </div>
      </Modal>
    </>
  );
}
