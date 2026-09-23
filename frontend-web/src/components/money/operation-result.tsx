"use client";

import Link from "next/link";
import { formatAmount, formatDateTime } from "@/lib/format";
import { TRANSACTION_STATUS_LABEL, TRANSACTION_TYPE_LABEL } from "@/lib/labels";
import { Alert, Badge, Button, Card, DetailRow } from "@/components/ui/primitives";
import { CheckIcon, ClockIcon } from "@/components/ui/icons";
import type { Transaction } from "@/lib/types";

/**
 * Confirmation d'une opération réussie.
 *
 * ELLE REMPLACE LE FORMULAIRE plutôt que de s'y superposer. Une bannière verte
 * au-dessus d'un formulaire encore rempli invite à renvoyer la même opération —
 * c'est ainsi qu'on double un virement. Ici, le formulaire disparaît et le seul
 * chemin vers un nouvel envoi est un bouton qui le réinitialise, clé
 * d'idempotence comprise.
 *
 * La RÉFÉRENCE est mise en avant : c'est elle qui identifie l'opération auprès
 * du support, et le seul moyen de la retrouver plus tard
 * (`GET /transactions/{reference}`).
 *
 * Un statut « en attente » n'est pas maquillé en succès : le backend crée
 * certaines opérations en `PENDING`, et laisser croire à un aboutissement
 * ferait s'inquiéter — ou pire, recommencer — quelqu'un dont l'argent est
 * simplement en cours de traitement.
 *
 * ═══ QUAND L'OPÉRATION RESTE À PAYER ═══
 *
 * Un dépôt peut revenir avec une `paymentUrl` : le prestataire n'a pas poussé
 * la demande sur le téléphone, il attend le client sur sa page. RIEN N'EST
 * ENCORE PAYÉ. L'écran le dit et met le paiement en action principale — la
 * mécanique habituelle (« c'est parti, voici votre reçu ») laisserait partir
 * quelqu'un persuadé d'avoir rechargé son compte.
 *
 * Le lien reste valable après un rechargement de page : il est stocké avec
 * l'écriture et ressort sur `/transactions/{reference}`. Fermer l'onglet ne
 * perd donc pas le dépôt.
 */
export function OperationResult({
  transaction,
  onRestart,
  restartLabel = "Nouvelle opération",
}: {
  transaction: Transaction;
  onRestart: () => void;
  restartLabel?: string;
}) {
  const pending = transaction.status === "PENDING";
  /* Une URL sur une opération déjà réglée n'a plus lieu d'être proposée : le
     lien est expiré et rouvrirait un paiement clos. */
  const toPay = pending ? transaction.paymentUrl : null;

  return (
    <Card className="rise">
      <div className="flex flex-col items-center text-center">
        <span
          className={
            pending
              ? "flex size-14 items-center justify-center rounded-full bg-warning-50 text-2xl text-warning-600"
              : "flex size-14 items-center justify-center rounded-full bg-positive-50 text-2xl text-positive-600"
          }
        >
          {pending ? <ClockIcon /> : <CheckIcon />}
        </span>

        <h2 className="mt-4 font-display text-xl font-semibold text-ink-950">
          {toPay
            ? "Il reste à régler votre dépôt"
            : pending
              ? "Opération en cours de traitement"
              : "Opération effectuée"}
        </h2>
        <p className="tabular mt-1 font-display text-3xl font-semibold text-ink-950">
          {formatAmount(transaction.amount)}
        </p>
        <p className="mt-1 text-sm text-ink-500">
          {TRANSACTION_TYPE_LABEL[transaction.type]}
        </p>
      </div>

      <dl className="mt-6">
        <DetailRow label="Référence" value={transaction.reference} mono />
        <DetailRow
          label="Statut"
          value={
            <Badge tone={pending ? "warning" : "positive"}>
              {TRANSACTION_STATUS_LABEL[transaction.status]}
            </Badge>
          }
        />
        {transaction.fee > 0 ? (
          <DetailRow label="Frais" value={formatAmount(transaction.fee)} />
        ) : null}
        <DetailRow label="Date" value={formatDateTime(transaction.createdAt)} />
      </dl>

      {toPay ? (
        <>
          <div className="mt-6">
            <Alert tone="warning" title="Votre compte n'est pas encore crédité">
              Terminez le paiement sur la page sécurisée du prestataire. Votre
              solde sera mis à jour dès que le paiement sera confirmé, sans
              autre action de votre part.
            </Alert>
          </div>

          <div className="mt-4 flex flex-col gap-2 sm:flex-row">
            <Button variant="secondary" full onClick={onRestart}>
              {restartLabel}
            </Button>
            {/* rel="noreferrer" en plus de noopener : la page de paiement n'a
                aucune raison de savoir d'où vient le client, et l'URL de nos
                écrans privés n'a pas à voyager dans un Referer. */}
            <a
              href={toPay}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex h-11 w-full items-center justify-center rounded-full bg-kola-600 px-5 text-[0.95rem] font-semibold text-white transition-colors hover:bg-kola-700"
            >
              Payer {formatAmount(transaction.amount)}
            </a>
          </div>

          <p className="mt-3 text-center text-sm text-ink-500">
            Vous pourrez retrouver ce paiement dans{" "}
            <Link
              href={`/transactions/${encodeURIComponent(transaction.reference)}`}
              className="font-semibold text-kola-700 underline underline-offset-2"
            >
              le détail de l&apos;opération
            </Link>{" "}
            si vous fermez cette page.
          </p>
        </>
      ) : (
        <div className="mt-6 flex flex-col gap-2 sm:flex-row">
          <Button variant="secondary" full onClick={onRestart}>
            {restartLabel}
          </Button>
          <Link
            href={`/transactions/${encodeURIComponent(transaction.reference)}`}
            className="inline-flex h-11 w-full items-center justify-center rounded-full bg-kola-600 px-5 text-[0.95rem] font-semibold text-white transition-colors hover:bg-kola-700"
          >
            Voir le reçu
          </Link>
        </div>
      )}
    </Card>
  );
}
