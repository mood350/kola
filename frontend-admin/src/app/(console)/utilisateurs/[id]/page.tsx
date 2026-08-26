"use client";

import { use, useCallback, useState } from "react";
import Link from "next/link";
import { ApiError, apiFetch } from "@/lib/api";
import { useResource } from "@/lib/use-resource";
import {
  Card,
  DataItem,
  EmptyBlock,
  ErrorBlock,
  LoadingBlock,
  PageHeading,
  TableScroll,
  Td,
  Th,
} from "@/components/kola/shell";
import {
  AccountStateBadge,
  KycBadge,
  LoanStatusBadge,
  TierBadge,
} from "@/components/kola/status";
import Badge from "@/components/ui/badge/Badge";
import Button from "@/components/ui/button/Button";
import { Modal } from "@/components/ui/modal";
import Label from "@/components/form/Label";
import {
  displayName,
  formatAmount,
  formatDate,
  formatDateTime,
  formatMonthlyRate,
} from "@/lib/format";
import {
  KYC_LABELS,
  KYC_LEVELS,
  type AdminUserDetail,
  type KycLevel,
} from "@/lib/types";

const CONTROL =
  "h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90";

/**
 * Fiche d'un compte.
 *
 * ═══ ORGANISATION ═══
 *
 * Identité et état d'abord — c'est ce qu'on vient vérifier. Puis les avoirs
 * (portefeuilles, coffres), puis la solvabilité (score, prêts). Les actions
 * restent en haut, près de l'état qu'elles modifient.
 *
 * ═══ POURQUOI CHAQUE ACTION PASSE PAR UNE BOÎTE DE DIALOGUE ═══
 *
 * Relever un palier KYC élargit les plafonds d'opération d'un client ;
 * verrouiller un compte lui coupe l'accès à son argent. Ni l'un ni l'autre ne
 * doit pouvoir se déclencher d'un clic isolé au mauvais endroit.
 *
 * Le motif est obligatoire, et ce n'est pas une formalité d'interface : le
 * backend le refuse en dessous de dix caractères, et c'est lui qui est écrit
 * dans le journal d'action avec l'identité de l'administrateur. Faute d'une
 * table d'audit persistante — limite documentée dans `AdminUserService` — c'est
 * aujourd'hui la seule trace de la décision.
 */

type ActionKind = "kyc" | "lock" | "unlock";

export default function UserDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  /* `params` est une promesse dans cette version de Next, y compris en
     composant client : `use()` la déballe pendant le rendu. */
  const { id } = use(params);

  const load = useCallback(
    (signal: AbortSignal) =>
      apiFetch<AdminUserDetail>(`/admin/users/${id}`, { signal }),
    [id]
  );

  const user = useResource(load);
  const [action, setAction] = useState<ActionKind | null>(null);

  if (user.error) {
    return (
      <>
        <PageHeading
          title="Fiche utilisateur"
          backHref="/utilisateurs"
          backLabel="Retour aux utilisateurs"
        />
        <Card>
          <ErrorBlock message={user.error} onRetry={user.reload} />
        </Card>
      </>
    );
  }

  if (!user.data) {
    return (
      <>
        <PageHeading
          title="Fiche utilisateur"
          backHref="/utilisateurs"
          backLabel="Retour aux utilisateurs"
        />
        <Card>
          <LoadingBlock label="Chargement de la fiche…" />
        </Card>
      </>
    );
  }

  const { identity, wallets, vaults, credit, loans } = user.data;
  const name = displayName(identity.firstName, identity.lastName, identity.email);

  return (
    <>
      <PageHeading
        title={name}
        description={identity.email}
        backHref="/utilisateurs"
        backLabel="Retour aux utilisateurs"
        actions={
          <>
            <Button size="sm" variant="outline" onClick={() => setAction("kyc")}>
              Modifier le KYC
            </Button>
            {identity.accountLocked ? (
              <Button size="sm" onClick={() => setAction("unlock")}>
                Déverrouiller
              </Button>
            ) : (
              <Button
                size="sm"
                variant="outline"
                className="!text-error-600 !ring-error-300 hover:!bg-error-50 dark:!text-error-400"
                onClick={() => setAction("lock")}
              >
                Verrouiller
              </Button>
            )}
          </>
        }
      />

      <div className="space-y-6">
        {/* ── Identité et état ────────────────────────────────────────── */}
        <Card title="Identité et état du compte">
          <dl className="grid gap-5 p-5 sm:grid-cols-2 lg:grid-cols-4">
            <DataItem label="État">
              <AccountStateBadge user={identity} />
            </DataItem>
            <DataItem label="Vérification">
              <KycBadge level={identity.kycLevel} />
            </DataItem>
            <DataItem label="Téléphone" mono>
              {identity.phoneNumber}
            </DataItem>
            <DataItem label="Pays">
              {identity.countryCode ?? "Non renseigné"}
            </DataItem>
            <DataItem label="Rôles">
              <span className="flex flex-wrap gap-1.5">
                {identity.roles.length === 0 ? (
                  <span className="text-gray-500">Aucun</span>
                ) : (
                  identity.roles.map((role) => (
                    <Badge
                      key={role}
                      size="sm"
                      color={role === "ADMIN" ? "primary" : "light"}
                    >
                      {role}
                    </Badge>
                  ))
                )}
              </span>
            </DataItem>
            <DataItem label="Inscription">
              {formatDateTime(identity.createdAt)}
            </DataItem>
            <DataItem label="Échecs de connexion">
              {identity.failedLoginAttempts}
              {/* Le sens du verrou dépend de `lockedAt`, pas seulement du
                  booléen — un verrou administratif ne se lève jamais tout seul.
                  L'écran doit le dire, sinon on attend un déblocage qui ne
                  viendra pas. */}
              {identity.accountLocked ? (
                <span className="mt-1 block text-theme-xs text-gray-500 dark:text-gray-400">
                  {identity.lockedAt === null
                    ? "Verrou administratif : il ne se lève que manuellement."
                    : `Verrou automatique posé le ${formatDateTime(identity.lockedAt)}, levé seul après 30 minutes.`}
                </span>
              ) : null}
            </DataItem>
            {/* `lastKnownIp` vit sur la fiche, pas sur le bloc d'identité :
                c'est une donnée de connexion, absente du résumé des listes. */}
            <DataItem label="Dernière adresse IP" mono>
              {user.data.lastKnownIp ?? "—"}
            </DataItem>
          </dl>

          {user.data.lastKnownUserAgent ? (
            <div className="border-t border-gray-100 px-5 py-4 dark:border-gray-800">
              <DataItem label="Dernier appareil connu" mono>
                {user.data.lastKnownUserAgent}
              </DataItem>
            </div>
          ) : null}
        </Card>

        {/* ── Avoirs ─────────────────────────────────────────────────── */}
        <div className="grid gap-6 xl:grid-cols-2">
          <Card
            title="Portefeuilles"
            description="Un portefeuille est désactivé, jamais supprimé."
          >
            {wallets.length === 0 ? (
              <EmptyBlock title="Aucun portefeuille." />
            ) : (
              <TableScroll>
                <table className="w-full">
                  <thead>
                    <tr>
                      <Th>Devise</Th>
                      <Th align="right">Solde</Th>
                      <Th align="right">Bloqué</Th>
                      <Th>État</Th>
                    </tr>
                  </thead>
                  <tbody>
                    {wallets.map((wallet) => (
                      <tr key={wallet.id}>
                        <Td>{wallet.currency}</Td>
                        <Td align="right">{formatAmount(wallet.balance)}</Td>
                        <Td align="right">{formatAmount(wallet.lockedBalance)}</Td>
                        <Td>
                          <Badge size="sm" color={wallet.active ? "success" : "light"}>
                            {wallet.active ? "Actif" : "Désactivé"}
                          </Badge>
                        </Td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </TableScroll>
            )}
          </Card>

          <Card
            title="Coffres d'épargne"
            description="Fonds bloqués jusqu'à la date choisie par le client."
          >
            {vaults.length === 0 ? (
              <EmptyBlock title="Aucun coffre." />
            ) : (
              <TableScroll>
                <table className="w-full">
                  <thead>
                    <tr>
                      <Th>Coffre</Th>
                      <Th align="right">Épargné</Th>
                      <Th align="right">Déblocage</Th>
                      <Th>État</Th>
                    </tr>
                  </thead>
                  <tbody>
                    {vaults.map((vault) => (
                      <tr key={vault.id}>
                        <Td>{vault.name}</Td>
                        <Td align="right">
                          {formatAmount(vault.currentAmount)}
                          {vault.targetAmount ? (
                            <span className="block text-theme-xs text-gray-500">
                              sur {formatAmount(vault.targetAmount)}
                            </span>
                          ) : null}
                        </Td>
                        <Td align="right">{formatDate(vault.unlockDate)}</Td>
                        <Td>
                          <Badge
                            size="sm"
                            color={vault.status === "ACTIVE" ? "primary" : "light"}
                          >
                            {vault.status === "ACTIVE"
                              ? "Bloqué"
                              : vault.status === "UNLOCKED"
                                ? "Débloqué"
                                : "Fermé"}
                          </Badge>
                        </Td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </TableScroll>
            )}
          </Card>
        </div>

        {/* ── Solvabilité ────────────────────────────────────────────── */}
        <Card
          title="Score de confiance et crédit"
          description="Le score expire au bout de trente jours et se recalcule à la lecture."
        >
          {credit === null ? (
            <EmptyBlock
              title="Aucun score calculé"
              hint="Normal sur un compte récent : le score apparaît après les premières opérations."
            />
          ) : (
            <dl className="grid gap-5 border-b border-gray-100 p-5 sm:grid-cols-2 lg:grid-cols-5 dark:border-gray-800">
              <DataItem label="Score">
                <span className="text-lg font-semibold text-gray-800 dark:text-white/90">
                  {credit.score}
                  <span className="text-sm font-normal text-gray-500"> / 100</span>
                </span>
              </DataItem>
              <DataItem label="Palier">
                <TierBadge tier={credit.tier} />
              </DataItem>
              <DataItem label="Plafond">
                {formatAmount(credit.maxLoanAmount)}
              </DataItem>
              <DataItem label="Taux">
                {formatMonthlyRate(credit.monthlyRate)}
              </DataItem>
              <DataItem label="Expire le">
                {formatDateTime(credit.expiresAt)}
              </DataItem>
            </dl>
          )}

          {loans.length === 0 ? (
            <EmptyBlock title="Aucune demande de prêt." />
          ) : (
            <TableScroll>
              <table className="w-full">
                <thead>
                  <tr>
                    <Th>Demandé le</Th>
                    <Th align="right">Montant</Th>
                    <Th align="right">À rembourser</Th>
                    <Th align="right">Durée</Th>
                    <Th align="right">Échéance</Th>
                    <Th>Statut</Th>
                  </tr>
                </thead>
                <tbody>
                  {loans.map((loan) => (
                    <tr key={loan.id}>
                      <Td>
                        <Link
                          href={`/prets/${loan.id}`}
                          className="font-medium text-gray-800 hover:text-brand-500 dark:text-white/90"
                        >
                          {formatDate(loan.createdAt)}
                        </Link>
                      </Td>
                      <Td align="right">{formatAmount(loan.requestedAmount)}</Td>
                      <Td align="right">{formatAmount(loan.totalRepayment)}</Td>
                      <Td align="right">{loan.durationMonths} mois</Td>
                      <Td align="right">{formatDate(loan.dueDate)}</Td>
                      <Td>
                        <LoanStatusBadge status={loan.status} />
                        {/* Un défaut passé reste visible même après
                            régularisation : c'est la raison d'être du champ
                            `defaultedAt`, que le statut seul effacerait. */}
                        {loan.defaultedAt && loan.status !== "DEFAULTED" ? (
                          <span className="mt-1 block text-theme-xs text-error-600">
                            Défaut le {formatDate(loan.defaultedAt)}
                          </span>
                        ) : null}
                      </Td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableScroll>
          )}
        </Card>
      </div>

      {action ? (
        <ActionDialog
          kind={action}
          user={user.data}
          onClose={() => setAction(null)}
          onDone={() => {
            setAction(null);
            user.reload();
          }}
        />
      ) : null}
    </>
  );
}

/* ---------------------------------------------------------------------------
   Boîte de dialogue d'action
   ------------------------------------------------------------------------ */

/**
 * Confirmation d'une action sensible, dans la `Modal` du template — qui gère
 * déjà la fermeture à la touche Échap et le blocage du défilement de fond.
 *
 * Les erreurs de validation renvoyées par le backend (motif trop court, palier
 * identique) sont affichées telles quelles : elles sont rédigées en français
 * côté Java et disent précisément ce qui bloque.
 */
function ActionDialog({
  kind,
  user,
  onClose,
  onDone,
}: {
  kind: ActionKind;
  user: AdminUserDetail;
  onClose: () => void;
  onDone: () => void;
}) {
  const [level, setLevel] = useState<KycLevel>(user.identity.kycLevel);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const id = user.identity.id;
  const needsReason = kind !== "unlock";

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      if (kind === "kyc") {
        await apiFetch(`/admin/users/${id}/kyc`, {
          method: "PATCH",
          body: { kycLevel: level, reason },
        });
      } else if (kind === "lock") {
        await apiFetch(`/admin/users/${id}/lock`, {
          method: "POST",
          body: { reason },
        });
      } else {
        await apiFetch(`/admin/users/${id}/unlock`, { method: "POST" });
      }
      onDone();
    } catch (caught) {
      if (caught instanceof ApiError) {
        /* `details` porte les erreurs champ par champ produites par Bean
           Validation. Plus précises que le message général, on les préfère
           quand elles existent. */
        const fieldErrors = Object.values(caught.details);
        setError(fieldErrors.length > 0 ? fieldErrors.join(" ") : caught.message);
      } else {
        setError("Impossible de joindre le serveur.");
      }
      setSubmitting(false);
    }
  }

  const title =
    kind === "kyc"
      ? "Modifier le niveau de vérification"
      : kind === "lock"
        ? "Verrouiller ce compte"
        : "Déverrouiller ce compte";

  return (
    <Modal isOpen onClose={onClose} className="m-4 max-w-md p-6 lg:p-8">
      <h2 className="text-lg font-semibold text-gray-800 dark:text-white/90">
        {title}
      </h2>
      <p className="mt-2 text-sm leading-relaxed text-gray-500 dark:text-gray-400">
        {kind === "kyc"
          ? "Le palier commande les plafonds d'opération et pèse 15 points dans le score de crédit. Le score déjà calculé n'est pas invalidé : il expire naturellement sous trente jours."
          : kind === "lock"
            ? "Le client perdra immédiatement l'accès à son compte et à ses fonds. Ce verrou est administratif : contrairement au verrou automatique, il ne se lève pas seul au bout de trente minutes."
            : "Le compte redeviendra accessible et le compteur de tentatives échouées sera remis à zéro."}
      </p>

      <form onSubmit={submit} className="mt-6 space-y-5">
        {kind === "kyc" ? (
          <div>
            <Label htmlFor="niveau">Nouveau niveau</Label>
            <select
              id="niveau"
              className={CONTROL}
              value={level}
              onChange={(event) => setLevel(event.target.value as KycLevel)}
            >
              {KYC_LEVELS.map((option) => (
                <option key={option} value={option}>
                  {option.replace("TIER_", "Niveau ")} · {KYC_LABELS[option]}
                </option>
              ))}
            </select>
            <p className="mt-1.5 text-theme-xs text-gray-500 dark:text-gray-400">
              Actuellement : {user.identity.kycLevel.replace("TIER_", "Niveau ")} ·{" "}
              {KYC_LABELS[user.identity.kycLevel]}
            </p>
          </div>
        ) : null}

        {needsReason ? (
          <div>
            <Label htmlFor="motif">Motif</Label>
            <textarea
              id="motif"
              rows={3}
              required
              minLength={10}
              maxLength={500}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              className="w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90"
            />
            <p className="mt-1.5 text-theme-xs text-gray-500 dark:text-gray-400">
              Dix caractères minimum. Consigné dans le journal avec votre identité.
            </p>
          </div>
        ) : null}

        {error ? (
          <p
            role="alert"
            className="rounded-lg border border-error-300 bg-error-50 px-4 py-3 text-sm text-error-700 dark:border-error-500/40 dark:bg-error-500/10 dark:text-error-400"
          >
            {error}
          </p>
        ) : null}

        <div className="flex justify-end gap-3">
          {/* `type="button"` indispensable : sans lui, le bouton vaut `submit`
              et « Annuler » enverrait le formulaire qu'il est censé abandonner. */}
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onClose}
            disabled={submitting}
          >
            Annuler
          </Button>
          <Button type="submit" size="sm" disabled={submitting}>
            {submitting ? "En cours…" : "Confirmer"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
