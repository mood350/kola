"use client";

import Link from "next/link";
import { useCallback, useState } from "react";
import { beneficiaryApi } from "@/lib/services";
import { describeActionError, useResource } from "@/lib/use-resource";
import { NETWORKS, NETWORK_COLOR, NETWORK_LABEL } from "@/lib/labels";
import {
  COUNTRIES,
  DEFAULT_COUNTRY,
  dialCodeFor,
  isValidPhone,
  normalizePhone,
} from "@/lib/countries";
import {
  Alert,
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
import { PlusIcon, SendIcon, TrashIcon, UsersIcon } from "@/components/ui/icons";
import type { Beneficiary, MobileNetwork } from "@/lib/types";

/**
 * Bénéficiaires.
 *
 * C'est le carnet d'adresses qui rend un transfert possible : le backend
 * n'accepte pas de numéro libre, seulement un `beneficiaryId`. La vérification
 * du numéro se fait donc ICI, une fois, à froid — pas au moment où l'on envoie
 * de l'argent.
 *
 * « Retirer » et non « supprimer » : le backend désactive le bénéficiaire
 * plutôt que de l'effacer (politique de non-suppression, cf. `User.java`), car
 * les virements passés continuent de le référencer.
 */
export default function BeneficiariesPage() {
  const load = useCallback((signal: AbortSignal) => beneficiaryApi.list(signal), []);
  const beneficiaries = useResource(load);

  const [adding, setAdding] = useState(false);
  const [removing, setRemoving] = useState<Beneficiary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const remove = async (beneficiary: Beneficiary) => {
    setError(null);
    setSubmitting(true);
    try {
      await beneficiaryApi.remove(beneficiary.id);
      setRemoving(null);
      beneficiaries.reload();
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Bénéficiaires"
        description="Les destinataires vers lesquels vous pouvez envoyer de l'argent."
        action={
          <Button icon={<PlusIcon />} onClick={() => setAdding(true)}>
            Ajouter
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

      {beneficiaries.loading ? (
        <SkeletonList rows={3} />
      ) : beneficiaries.error ? (
        <LoadError message={beneficiaries.error} onRetry={beneficiaries.reload} />
      ) : beneficiaries.data && beneficiaries.data.length > 0 ? (
        <ul className="space-y-2">
          {beneficiaries.data.map((beneficiary) => {
            const color = NETWORK_COLOR[beneficiary.network];
            return (
              <li key={beneficiary.id}>
                <Card className="flex items-center gap-3">
                  <span
                    className="flex size-11 shrink-0 items-center justify-center rounded-full text-sm font-bold"
                    style={{
                      backgroundColor: color ? `${color}26` : "var(--color-ink-100)",
                      color: color ?? "var(--color-ink-600)",
                    }}
                  >
                    {beneficiary.alias.slice(0, 2).toUpperCase()}
                  </span>

                  <div className="min-w-0 flex-1">
                    <p className="truncate font-semibold text-ink-900">
                      {beneficiary.alias}
                    </p>
                    <p className="truncate text-xs text-ink-500">
                      {beneficiary.phoneNumber} · {NETWORK_LABEL[beneficiary.network]} ·{" "}
                      {beneficiary.countryCode}
                    </p>
                  </div>

                  <Link
                    href={`/operations/envoi?beneficiaire=${beneficiary.id}`}
                    className="inline-flex size-10 items-center justify-center rounded-full text-kola-600 transition-colors hover:bg-kola-50"
                    aria-label={`Envoyer de l'argent à ${beneficiary.alias}`}
                    title="Envoyer"
                  >
                    <SendIcon className="text-lg" />
                  </Link>
                  <button
                    type="button"
                    onClick={() => setRemoving(beneficiary)}
                    aria-label={`Retirer ${beneficiary.alias}`}
                    title="Retirer"
                    className="inline-flex size-10 items-center justify-center rounded-full text-ink-400 transition-colors hover:bg-danger-50 hover:text-danger-600"
                  >
                    <TrashIcon className="text-lg" />
                  </button>
                </Card>
              </li>
            );
          })}
        </ul>
      ) : (
        <EmptyState
          icon={<UsersIcon />}
          title="Aucun bénéficiaire"
          description="Enregistrez un destinataire pour pouvoir lui envoyer de l'argent. Le numéro se vérifie une fois, ici."
          action={<Button onClick={() => setAdding(true)}>Ajouter un bénéficiaire</Button>}
        />
      )}

      <AddBeneficiaryModal
        open={adding}
        onClose={() => setAdding(false)}
        onCreated={() => {
          setAdding(false);
          beneficiaries.reload();
        }}
      />

      <Modal
        open={removing !== null}
        onClose={() => setRemoving(null)}
        title="Retirer ce bénéficiaire"
      >
        <p className="text-sm text-ink-600">
          {removing?.alias} n&apos;apparaîtra plus dans la liste des
          destinataires. Les transferts déjà effectués vers ce numéro restent
          dans votre historique.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="ghost" onClick={() => setRemoving(null)}>
            Annuler
          </Button>
          <Button
            variant="danger"
            loading={submitting}
            onClick={() => {
              if (removing) void remove(removing);
            }}
          >
            Retirer
          </Button>
        </div>
      </Modal>
    </>
  );
}

function AddBeneficiaryModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}) {
  const [alias, setAlias] = useState("");
  const [countryCode, setCountryCode] = useState(DEFAULT_COUNTRY);
  const [phone, setPhone] = useState(dialCodeFor(DEFAULT_COUNTRY));
  const [network, setNetwork] = useState<MobileNetwork>("MIXX_BY_YAS");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [touched, setTouched] = useState(false);

  const phoneError =
    touched && !isValidPhone(phone)
      ? "Numéro attendu au format international, ex : +22890000000"
      : null;

  const handleCountryChange = (code: string) => {
    const previous = dialCodeFor(countryCode);
    setCountryCode(code);
    if (phone === previous || phone === "") setPhone(dialCodeFor(code));
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (!alias.trim() || !isValidPhone(phone)) return;

    setError(null);
    setSubmitting(true);
    try {
      await beneficiaryApi.create({
        alias: alias.trim(),
        phoneNumber: normalizePhone(phone),
        countryCode,
        network,
      });
      setAlias("");
      setPhone(dialCodeFor(countryCode));
      setTouched(false);
      onCreated();
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Nouveau bénéficiaire">
      <form id="add-beneficiary" onSubmit={handleSubmit} className="space-y-4" noValidate>
        {error ? <Alert tone="danger">{error}</Alert> : null}

        <Field label="Nom" htmlFor="alias" hint="Le nom sous lequel vous le reconnaîtrez.">
          <Input
            id="alias"
            required
            value={alias}
            onChange={(event) => setAlias(event.target.value)}
            placeholder="Ex : Maman"
          />
        </Field>

        <Field label="Pays" htmlFor="beneficiary-country">
          <Select
            id="beneficiary-country"
            value={countryCode}
            onChange={(event) => handleCountryChange(event.target.value)}
          >
            {COUNTRIES.map((country) => (
              <option key={country.code} value={country.code}>
                {country.name}
              </option>
            ))}
          </Select>
        </Field>

        <Field
          label="Numéro de téléphone"
          htmlFor="beneficiary-phone"
          error={phoneError}
          hint="Vérifiez-le maintenant : c'est ce numéro qui recevra l'argent."
        >
          <Input
            id="beneficiary-phone"
            type="tel"
            inputMode="tel"
            required
            invalid={Boolean(phoneError)}
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            placeholder="+22890000000"
          />
        </Field>

        <Field label="Réseau Mobile Money" htmlFor="network">
          <Select
            id="network"
            value={network}
            onChange={(event) => setNetwork(event.target.value as MobileNetwork)}
          >
            {NETWORKS.map((item) => (
              <option key={item} value={item}>
                {NETWORK_LABEL[item]}
              </option>
            ))}
          </Select>
        </Field>
      </form>

      <div className="mt-5 flex justify-end gap-2">
        <Button variant="ghost" type="button" onClick={onClose}>
          Annuler
        </Button>
        <Button type="submit" form="add-beneficiary" loading={submitting}>
          Enregistrer
        </Button>
      </div>
    </Modal>
  );
}
