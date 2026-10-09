"use client";

import { use, useCallback } from "react";
import { transactionApi } from "@/lib/services";
import { useResource } from "@/lib/use-resource";
import { formatAmount, formatDateTime, formatSignedAmount } from "@/lib/format";
import {
  TRANSACTION_STATUS_LABEL,
  TRANSACTION_STATUS_TONE,
  TRANSACTION_TYPE_LABEL,
  isCredit,
} from "@/lib/labels";
import { Alert, Badge, Card, DetailRow, LoadError, Skeleton } from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { cn } from "@/lib/cn";

/**
 * Reçu d'une opération.
 *
 * On interroge le backend par la RÉFÉRENCE et non par l'identifiant : c'est
 * l'endpoint qui existe (`GET /transactions/{reference}`), et c'est aussi la
 * seule donnée que l'utilisateur possède — celle du reçu, celle qu'il donnera
 * au support. L'URL est donc partageable et fait sens hors de l'application.
 *
 * `params` est une promesse dans cette version de Next ; `use()` la dénoue dans
 * un composant client.
 */
export default function TransactionDetailPage({
  params,
}: {
  params: Promise<{ reference: string }>;
}) {
  const { reference } = use(params);
  const decoded = decodeURIComponent(reference);

  const load = useCallback(
    (signal: AbortSignal) => transactionApi.byReference(decoded, signal),
    [decoded]
  );
  const resource = useResource(load);
  const transaction = resource.data;

  return (
    <>
      <PageHeader
        title="Détail de l'opération"
        backHref="/transactions"
        backLabel="Transactions"
      />

      {resource.loading ? (
        <Card className="space-y-4">
          <Skeleton className="mx-auto h-10 w-48" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-2/3" />
        </Card>
      ) : resource.error ? (
        <LoadError message={resource.error} onRetry={resource.reload} />
      ) : transaction ? (
        <Card>
          <div className="border-b border-line pb-5 text-center">
            <p className="text-sm text-ink-500">
              {TRANSACTION_TYPE_LABEL[transaction.type]}
            </p>
            <p
              className={cn(
                "tabular mt-1 font-display text-3xl font-semibold",
                isCredit(transaction.type) ? "text-positive-600" : "text-ink-950"
              )}
            >
              {formatSignedAmount(transaction.amount, isCredit(transaction.type))}
            </p>
            <div className="mt-3 flex justify-center">
              <Badge tone={TRANSACTION_STATUS_TONE[transaction.status]}>
                {TRANSACTION_STATUS_LABEL[transaction.status]}
              </Badge>
            </div>
          </div>

          <dl className="mt-4">
            <DetailRow label="Référence" value={transaction.reference} mono />
            <DetailRow label="Date" value={formatDateTime(transaction.createdAt)} />
            <DetailRow label="Devise" value={transaction.currency} />

            {/* Les frais ne sont montrés QUE s'ils existent : une ligne
                « 0 F CFA » sur chaque reçu ferait chercher un prélèvement qui
                n'a pas eu lieu. */}
            {transaction.fee > 0 ? (
              <DetailRow label="Frais" value={formatAmount(transaction.fee)} />
            ) : null}

            {transaction.receiverPhoneNumber ? (
              <DetailRow
                label="Destinataire"
                value={`${transaction.receiverPhoneNumber}${
                  transaction.receiverCountryCode
                    ? ` (${transaction.receiverCountryCode})`
                    : ""
                }`}
              />
            ) : null}

            {/* Le taux de change n'apparaît que sur une opération transfrontière
                — sinon il n'a rien à expliquer. */}
            {transaction.exchangeRate ? (
              <DetailRow
                label="Taux de change"
                value={`1 ${transaction.currency} = ${transaction.exchangeRate} ${
                  transaction.receiverCurrency ?? ""
                }`}
              />
            ) : null}

            {transaction.description ? (
              <DetailRow label="Motif" value={transaction.description} />
            ) : null}

            {/* Utile au support pour retrouver une opération soupçonnée d'avoir
                été soumise deux fois. */}
            {transaction.idempotencyKey ? (
              <DetailRow label="Clé d'idempotence" value={transaction.idempotencyKey} mono />
            ) : null}
          </dl>

          {/* Reprise d'un dépôt interrompu.
              Le lien de paiement est stocké avec l'écriture, donc un client qui
              a fermé l'onglet retrouve ici le paiement en cours — au lieu d'en
              ouvrir un second et de payer deux fois. Rien ne s'affiche une fois
              l'écriture réglée : le lien est alors expiré. */}
          {transaction.status === "PENDING" && transaction.paymentUrl ? (
            <div className="mt-6 border-t border-line pt-5">
              <Alert tone="warning" title="Ce dépôt n'est pas encore réglé">
                Le paiement n&apos;a pas été confirmé par le prestataire. Vous
                pouvez le terminer maintenant — votre solde sera mis à jour
                automatiquement.
              </Alert>
              <a
                href={transaction.paymentUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-4 inline-flex h-11 w-full items-center justify-center rounded-full bg-kola-600 px-5 text-[0.95rem] font-semibold text-white transition-colors hover:bg-kola-700"
              >
                Payer {formatAmount(transaction.amount)}
              </a>
            </div>
          ) : null}
        </Card>
      ) : null}
    </>
  );
}
