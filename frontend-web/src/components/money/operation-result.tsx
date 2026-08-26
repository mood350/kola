"use client";

import Link from "next/link";
import { formatAmount, formatDateTime } from "@/lib/format";
import { TRANSACTION_STATUS_LABEL, TRANSACTION_TYPE_LABEL } from "@/lib/labels";
import { Badge, Button, Card, DetailRow } from "@/components/ui/primitives";
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
          {pending ? "Opération en cours de traitement" : "Opération effectuée"}
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
    </Card>
  );
}
