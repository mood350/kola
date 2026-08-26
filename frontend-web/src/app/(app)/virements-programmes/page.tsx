"use client";

import { useCallback, useState } from "react";
import { useSession } from "@/lib/session";
import { scheduledTransferApi, vaultApi } from "@/lib/services";
import { describeActionError, useResource } from "@/lib/use-resource";
import { formatAmount, formatDate } from "@/lib/format";
import {
  SCHEDULE_FREQUENCY_LABEL,
  SCHEDULE_STATUS_LABEL,
  SCHEDULE_STATUS_TONE,
  scheduleLabel,
} from "@/lib/labels";
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
  Select,
  SkeletonList,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { AmountField, parseAmount } from "@/components/money/amount-field";
import { NoWalletNotice, WalletPicker } from "@/components/money/wallet-picker";
import { ClockIcon, PauseIcon, PlayIcon, PlusIcon, TrashIcon } from "@/components/ui/icons";
import type { ScheduleFrequency, ScheduledTransfer } from "@/lib/types";

/**
 * Virements programmés.
 *
 * Ils automatisent l'épargne : à date fixe, une somme quitte le wallet, vers un
 * coffre si l'on en désigne un. C'est le mécanisme qui alimente la régularité
 * mesurée par le score de crédit (`ScoringRule.DEPOSIT_REGULARITY`).
 *
 * METTRE EN PAUSE PLUTÔT QUE SUPPRIMER est proposé en premier, et l'ordre des
 * boutons le dit : un mois difficile ne doit pas coûter la programmation
 * elle-même, qu'il faudrait ensuite recréer de zéro.
 */
export default function ScheduledTransfersPage() {
  const { activeWallet } = useSession();
  const load = useCallback((signal: AbortSignal) => scheduledTransferApi.list(signal), []);
  const schedules = useResource(load);

  const [creating, setCreating] = useState(false);
  const [removing, setRemoving] = useState<ScheduledTransfer | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const run = async (id: number, task: () => Promise<unknown>) => {
    setError(null);
    setBusyId(id);
    try {
      await task();
      setRemoving(null);
      schedules.reload();
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <>
      <PageHeader
        title="Virements programmés"
        description="Automatisez votre épargne : une somme, une fréquence."
        action={
          activeWallet ? (
            <Button icon={<PlusIcon />} onClick={() => setCreating(true)}>
              Programmer
            </Button>
          ) : null
        }
      />

      {error ? (
        <div className="mb-4">
          <Alert tone="danger" onDismiss={() => setError(null)}>
            {error}
          </Alert>
        </div>
      ) : null}

      {!activeWallet ? (
        <NoWalletNotice />
      ) : schedules.loading ? (
        <SkeletonList rows={2} />
      ) : schedules.error ? (
        <LoadError message={schedules.error} onRetry={schedules.reload} />
      ) : schedules.data && schedules.data.length > 0 ? (
        <ul className="space-y-3">
          {schedules.data.map((schedule) => (
            <li key={schedule.id}>
              <Card>
                <div className="flex items-start gap-3">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-kola-50 text-lg text-kola-600">
                    <ClockIcon />
                  </span>

                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="tabular font-semibold text-ink-900">
                        {formatAmount(schedule.amount)}
                      </p>
                      <Badge tone={SCHEDULE_STATUS_TONE[schedule.status]}>
                        {SCHEDULE_STATUS_LABEL[schedule.status]}
                      </Badge>
                    </div>
                    <p className="text-sm text-ink-600">
                      {scheduleLabel(schedule.frequency, schedule.executionDay)}
                      {schedule.targetVaultName
                        ? ` → coffre « ${schedule.targetVaultName} »`
                        : ""}
                    </p>
                    {schedule.description ? (
                      <p className="truncate text-xs text-ink-500">{schedule.description}</p>
                    ) : null}
                    <p className="mt-1 text-xs text-ink-400">
                      {schedule.nextExecutionDate
                        ? `Prochaine exécution : ${formatDate(schedule.nextExecutionDate)}`
                        : "Aucune exécution planifiée"}
                      {schedule.lastExecutedAt
                        ? ` · dernière : ${formatDate(schedule.lastExecutedAt)}`
                        : ""}
                    </p>
                  </div>
                </div>

                {/* Un virement en échec permanent n'est ni reprenable ni
                    pausable côté backend : n'afficher que le retrait évite de
                    proposer une action qui échouerait. */}
                <div className="mt-4 flex gap-2 border-t border-line pt-3">
                  {schedule.status === "ACTIVE" ? (
                    <Button
                      variant="secondary"
                      size="sm"
                      icon={<PauseIcon />}
                      loading={busyId === schedule.id}
                      onClick={() =>
                        void run(schedule.id, () => scheduledTransferApi.pause(schedule.id))
                      }
                    >
                      Mettre en pause
                    </Button>
                  ) : schedule.status === "PAUSED" ? (
                    <Button
                      variant="secondary"
                      size="sm"
                      icon={<PlayIcon />}
                      loading={busyId === schedule.id}
                      onClick={() =>
                        void run(schedule.id, () => scheduledTransferApi.resume(schedule.id))
                      }
                    >
                      Reprendre
                    </Button>
                  ) : null}

                  <Button
                    variant="ghost"
                    size="sm"
                    icon={<TrashIcon />}
                    onClick={() => setRemoving(schedule)}
                  >
                    Supprimer
                  </Button>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      ) : (
        <EmptyState
          icon={<ClockIcon />}
          title="Aucun virement programmé"
          description="Programmez un versement régulier vers un coffre : c'est la façon la plus simple de faire progresser votre score."
          action={<Button onClick={() => setCreating(true)}>Programmer un virement</Button>}
        />
      )}

      <CreateScheduleModal
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={() => {
          setCreating(false);
          schedules.reload();
        }}
      />

      <Modal
        open={removing !== null}
        onClose={() => setRemoving(null)}
        title="Supprimer la programmation"
      >
        <p className="text-sm text-ink-600">
          Ce virement ne sera plus exécuté. Les virements déjà passés restent
          dans votre historique. Pour l&apos;interrompre temporairement,
          préférez la mise en pause.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setRemoving(null)}>
            Annuler
          </Button>
          <Button
            variant="danger"
            loading={busyId === removing?.id}
            onClick={() => {
              if (removing) {
                void run(removing.id, () => scheduledTransferApi.remove(removing.id));
              }
            }}
          >
            Supprimer
          </Button>
        </div>
      </Modal>
    </>
  );
}

/**
 * Création d'une programmation.
 *
 * `executionDay` change de sens avec la fréquence — quantième du mois (1 à 31)
 * ou jour de la semaine (1 = lundi). Le champ change donc de nature avec elle,
 * plutôt que de demander un nombre nu que l'utilisateur interpréterait de
 * travers une fois sur deux.
 */
function CreateScheduleModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}) {
  const { activeWallet } = useSession();
  const loadVaults = useCallback((signal: AbortSignal) => vaultApi.list(signal), []);
  const vaults = useResource(loadVaults);

  const [frequency, setFrequency] = useState<ScheduleFrequency>("MONTHLY");
  const [executionDay, setExecutionDay] = useState("1");
  const [amount, setAmount] = useState("");
  const [targetVaultId, setTargetVaultId] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const value = parseAmount(amount, { min: 1 });
  const activeVaults = (vaults.data ?? []).filter((vault) => vault.status === "ACTIVE");

  const handleFrequencyChange = (next: ScheduleFrequency) => {
    setFrequency(next);
    /* Le jour est réinitialisé : « 28 » a un sens mensuel, aucun hebdomadaire —
       le backend le refuserait au-delà de 7 jours de la semaine attendus. */
    setExecutionDay("1");
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!activeWallet || value === null) return;

    setError(null);
    setSubmitting(true);
    try {
      await scheduledTransferApi.create({
        walletId: activeWallet.id,
        targetVaultId: targetVaultId ? Number(targetVaultId) : undefined,
        frequency,
        executionDay: Number(executionDay),
        amount: value,
        description: description.trim() || undefined,
      });
      setAmount("");
      setDescription("");
      setTargetVaultId("");
      onCreated();
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setSubmitting(false);
    }
  };

  const dayOptions =
    frequency === "MONTHLY"
      ? Array.from({ length: 31 }, (_, index) => ({
          value: String(index + 1),
          label: `Le ${index + 1} du mois`,
        }))
      : ["lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"].map(
          (day, index) => ({ value: String(index + 1), label: `Chaque ${day}` })
        );

  return (
    <Modal open={open} onClose={onClose} title="Programmer un virement">
      <form id="create-schedule" onSubmit={handleSubmit} className="space-y-4" noValidate>
        {error ? <Alert tone="danger">{error}</Alert> : null}

        <WalletPicker label="Compte à débiter" />

        <AmountField
          id="schedule-amount"
          value={amount}
          onChange={setAmount}
          label="Montant à virer"
          quickAmounts={[1000, 5000, 10_000]}
        />

        <Field label="Fréquence" htmlFor="frequency">
          <Select
            id="frequency"
            value={frequency}
            onChange={(event) =>
              handleFrequencyChange(event.target.value as ScheduleFrequency)
            }
          >
            {(["MONTHLY", "WEEKLY"] as ScheduleFrequency[]).map((item) => (
              <option key={item} value={item}>
                {SCHEDULE_FREQUENCY_LABEL[item]}
              </option>
            ))}
          </Select>
        </Field>

        <Field
          label="Jour d'exécution"
          htmlFor="execution-day"
          hint={
            frequency === "MONTHLY"
              ? "Pour les mois plus courts, l'exécution a lieu le dernier jour disponible."
              : undefined
          }
        >
          <Select
            id="execution-day"
            value={executionDay}
            onChange={(event) => setExecutionDay(event.target.value)}
          >
            {dayOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
        </Field>

        <Field
          label="Coffre de destination"
          htmlFor="target-vault"
          hint="Facultatif — sans coffre, la somme reste sur le compte."
        >
          <Select
            id="target-vault"
            value={targetVaultId}
            onChange={(event) => setTargetVaultId(event.target.value)}
          >
            <option value="">Aucun coffre</option>
            {activeVaults.map((vault) => (
              <option key={vault.id} value={vault.id}>
                {vault.name}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="Libellé" htmlFor="schedule-description" hint="Facultatif.">
          <Input
            id="schedule-description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Ex : épargne mensuelle"
            maxLength={140}
          />
        </Field>
      </form>

      <div className="mt-5 flex justify-end gap-2">
        <Button variant="ghost" type="button" onClick={onClose}>
          Annuler
        </Button>
        <Button
          type="submit"
          form="create-schedule"
          loading={submitting}
          disabled={value === null}
        >
          Programmer
        </Button>
      </div>
    </Modal>
  );
}
