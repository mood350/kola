"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { apiFetch, queryString } from "@/lib/api";
import { useResource } from "@/lib/use-resource";
import {
  Card,
  EmptyBlock,
  ErrorBlock,
  LoadingBlock,
  PageHeading,
  Pagination,
  TableScroll,
  Td,
  Th,
} from "@/components/kola/shell";
import { AccountStateBadge, KycBadge } from "@/components/kola/status";
import Label from "@/components/form/Label";
import { displayName, formatDate } from "@/lib/format";
import {
  KYC_LABELS,
  KYC_LEVELS,
  type AdminUserSummary,
  type Page as PageResult,
} from "@/lib/types";

/* Contrôles de formulaire du template, extraits pour ne pas les recopier trois
   fois. Les composants `InputField`/`Select` de TailAdmin ne conviennent pas
   ici : le premier ne gère que `defaultValue` (non contrôlé), le second détient
   sa propre sélection en état interne — or ces filtres doivent être pilotés par
   la page, qui remet la pagination à zéro à chaque changement. */
const CONTROL =
  "h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400 focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90 dark:placeholder:text-white/30";

/**
 * Annuaire des comptes.
 *
 * ═══ RECHERCHE DIFFÉRÉE ═══
 *
 * La saisie n'est pas envoyée à chaque frappe : elle attend 350 ms de silence.
 * Sans ce délai, « aminata » déclenche sept requêtes dont six sont périmées à
 * l'arrivée — et comme les réponses ne reviennent pas dans l'ordre où elles
 * partent, c'est parfois le résultat de « amin » qui s'affiche pour « aminata ».
 * `useResource` annule en plus la requête précédente, ce qui ferme
 * définitivement cette course.
 *
 * ═══ RETOUR À LA PREMIÈRE PAGE ═══
 *
 * Tout changement de filtre remet la pagination à zéro. Rester en page 4 après
 * avoir filtré affiche un tableau vide alors que des résultats existent — l'un
 * des défauts les plus déroutants de ce type d'écran.
 */
export default function UsersPage() {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [kycLevel, setKycLevel] = useState("");
  const [state, setState] = useState("");
  const [page, setPage] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query);
      setPage(0);
    }, 350);
    return () => clearTimeout(timer);
  }, [query]);

  /**
   * Le filtre d'état combine deux champs distincts du backend (`enabled` et
   * `locked`) que l'opérateur perçoit comme un seul axe : dans quel état est ce
   * compte. Lui faire manipuler deux listes indépendantes l'obligerait à
   * connaître le modèle de données.
   */
  const stateParams =
    state === "locked"
      ? { locked: true }
      : state === "pending"
        ? { enabled: false }
        : state === "active"
          ? { enabled: true, locked: false }
          : {};

  const load = useCallback(
    (signal: AbortSignal) =>
      apiFetch<PageResult<AdminUserSummary>>(
        `/admin/users${queryString({
          q: debouncedQuery,
          kycLevel,
          ...stateParams,
          page,
          size: 20,
        })}`,
        { signal }
      ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [debouncedQuery, kycLevel, state, page]
  );

  const users = useResource(load);

  return (
    <>
      <PageHeading
        title="Utilisateurs"
        description="Rechercher un compte, consulter sa fiche, ajuster son niveau de vérification ou lever un verrou."
      />

      <Card>
        {/* Filtres sur une ligne au-dessus du tableau : ils commandent ce qui
            s'affiche en dessous, la proximité doit le dire. */}
        <div className="grid gap-4 border-b border-gray-100 p-5 sm:grid-cols-2 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)_minmax(0,1fr)] dark:border-gray-800">
          <div>
            <Label htmlFor="recherche">Rechercher</Label>
            <input
              id="recherche"
              type="search"
              className={CONTROL}
              placeholder="Nom, e-mail ou téléphone — même partiel"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>

          <div>
            <Label htmlFor="kyc">Niveau de vérification</Label>
            <select
              id="kyc"
              className={CONTROL}
              value={kycLevel}
              onChange={(event) => {
                setKycLevel(event.target.value);
                setPage(0);
              }}
            >
              <option value="">Tous les niveaux</option>
              {KYC_LEVELS.map((level) => (
                <option key={level} value={level}>
                  {level.replace("TIER_", "Niveau ")} · {KYC_LABELS[level]}
                </option>
              ))}
            </select>
          </div>

          <div>
            <Label htmlFor="etat">État du compte</Label>
            <select
              id="etat"
              className={CONTROL}
              value={state}
              onChange={(event) => {
                setState(event.target.value);
                setPage(0);
              }}
            >
              <option value="">Tous les états</option>
              <option value="active">Actifs</option>
              <option value="locked">Verrouillés</option>
              <option value="pending">Non activés</option>
            </select>
          </div>
        </div>

        {users.error ? (
          <ErrorBlock message={users.error} onRetry={users.reload} />
        ) : null}

        {/* Les données précédentes sont conservées pendant un rechargement : le
            tableau ne disparaît pas à chaque frappe. Vider l'écran ferait
            clignoter toute la page. */}
        {users.loading && !users.data ? (
          <LoadingBlock label="Chargement des comptes…" />
        ) : null}

        {users.data ? (
          users.data.content.length === 0 ? (
            <EmptyBlock
              title="Aucun compte ne correspond"
              hint="Élargissez la recherche ou retirez un filtre."
            />
          ) : (
            <>
              <div className={users.loading ? "opacity-60 transition-opacity" : undefined}>
                <TableScroll>
                  <table className="w-full">
                    <thead>
                      <tr>
                        <Th>Utilisateur</Th>
                        <Th>Téléphone</Th>
                        <Th>Vérification</Th>
                        <Th>État</Th>
                        <Th align="right">Échecs</Th>
                        <Th align="right">Inscription</Th>
                      </tr>
                    </thead>
                    <tbody>
                      {users.data.content.map((user) => (
                        <tr
                          key={user.id}
                          className="transition-colors hover:bg-gray-50 dark:hover:bg-white/[0.02]"
                        >
                          <Td>
                            {/* Le lien porte le NOM, pas un « voir » générique :
                                un lecteur d'écran qui liste les liens de la page
                                doit obtenir des intitulés distincts. */}
                            <Link
                              href={`/utilisateurs/${user.id}`}
                              className="font-medium text-gray-800 hover:text-brand-500 dark:text-white/90"
                            >
                              {displayName(user.firstName, user.lastName, user.email)}
                            </Link>
                            <span className="block text-theme-xs text-gray-500 dark:text-gray-400">
                              {user.email}
                            </span>
                          </Td>
                          <Td>
                            <span className="font-mono text-theme-xs">
                              {user.phoneNumber}
                            </span>
                          </Td>
                          <Td>
                            <KycBadge level={user.kycLevel} />
                          </Td>
                          <Td>
                            <AccountStateBadge user={user} />
                          </Td>
                          <Td align="right">
                            {/* Le compteur n'est mis en avant qu'à partir de 3 :
                                en dessous c'est du bruit, une faute de frappe
                                arrive à tout le monde. */}
                            <span
                              className={
                                user.failedLoginAttempts >= 3
                                  ? "font-medium text-error-600"
                                  : "text-gray-500 dark:text-gray-400"
                              }
                            >
                              {user.failedLoginAttempts}
                            </span>
                          </Td>
                          <Td align="right">{formatDate(user.createdAt)}</Td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </TableScroll>
              </div>

              <Pagination
                page={users.data.number}
                totalPages={users.data.totalPages}
                totalElements={users.data.totalElements}
                onChange={setPage}
              />
            </>
          )
        ) : null}
      </Card>
    </>
  );
}
