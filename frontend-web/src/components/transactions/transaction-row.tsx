import Link from "next/link";
import { cn } from "@/lib/cn";
import { formatRelative, formatSignedAmount } from "@/lib/format";
import {
  TRANSACTION_STATUS_LABEL,
  TRANSACTION_TYPE_LABEL,
  TRANSACTION_TYPE_TONE,
  isCredit,
} from "@/lib/labels";
import { TONE_SOFT } from "@/components/ui/primitives";
import type { Transaction, TransactionType } from "@/lib/types";
import {
  ArrowDownIcon,
  ArrowUpIcon,
  ClockIcon,
  CreditIcon,
  LockIcon,
  ReceiptIcon,
  SendIcon,
  StoreIcon,
  UserIcon,
} from "@/components/ui/icons";

/**
 * Icône par type de mouvement — même correspondance que sur mobile
 * (`TransactionType.icon`). Les deux clients doivent dessiner un dépôt de la
 * même façon, sinon le même relevé se lit différemment selon l'appareil.
 */
const TYPE_ICON: Record<TransactionType, React.ComponentType<{ className?: string }>> = {
  DEPOSIT: ArrowDownIcon,
  WITHDRAWAL: ArrowUpIcon,
  TRANSFER_OUT: SendIcon,
  TRANSFER_IN: UserIcon,
  VAULT_LOCK: LockIcon,
  VAULT_UNLOCK: LockIcon,
  FEE: ReceiptIcon,
  SCHEDULED_TRANSFER: ClockIcon,
  LOAN_DISBURSEMENT: CreditIcon,
  LOAN_REPAYMENT: CreditIcon,
  MERCHANT_PAYMENT: StoreIcon,
};

/**
 * Sous-titre d'une ligne.
 *
 * Ordre repris du mobile (`Transaction.displaySubtitle`) : le numéro du
 * destinataire prime, à défaut la description, à défaut le statut — et
 * uniquement s'il n'est pas « réussie », auquel cas il n'apprend rien. Ce
 * dernier repli est ce qui fait qu'un échec ne passe jamais inaperçu dans une
 * liste parcourue rapidement.
 */
function subtitle(transaction: Transaction): string {
  if (transaction.receiverPhoneNumber) return transaction.receiverPhoneNumber;
  if (transaction.description) return transaction.description;
  if (transaction.status !== "SUCCESS") {
    return TRANSACTION_STATUS_LABEL[transaction.status];
  }
  return formatRelative(transaction.createdAt);
}

export function TransactionRow({
  transaction,
  showDate = true,
}: {
  transaction: Transaction;
  showDate?: boolean;
}) {
  const Icon = TYPE_ICON[transaction.type];
  const tone = TRANSACTION_TYPE_TONE[transaction.type];
  const credit = isCredit(transaction.type);
  const failed = transaction.status === "FAILED" || transaction.status === "CANCELLED";

  return (
    <Link
      href={`/transactions/${encodeURIComponent(transaction.reference)}`}
      className="flex items-center gap-3 rounded-surface bg-surface p-4 ring-1 ring-line transition-colors hover:bg-ink-50"
    >
      <span
        className={cn(
          "flex size-10 shrink-0 items-center justify-center rounded-full text-lg",
          TONE_SOFT[tone]
        )}
      >
        <Icon />
      </span>

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-semibold text-ink-900">
          {TRANSACTION_TYPE_LABEL[transaction.type]}
        </p>
        <p className="truncate text-xs text-ink-500">{subtitle(transaction)}</p>
      </div>

      <div className="text-right">
        <p
          className={cn(
            "tabular text-sm font-semibold",
            /* Un mouvement échoué est barré et grisé : afficher « − 50 000 F »
               en rouge vif pour une opération qui n'a PAS eu lieu ferait
               chercher de l'argent qui n'est jamais parti. */
            failed
              ? "text-ink-400 line-through"
              : credit
                ? "text-positive-600"
                : "text-ink-900"
          )}
        >
          {formatSignedAmount(transaction.amount, credit)}
        </p>
        {showDate ? (
          <p className="text-xs text-ink-400">{formatRelative(transaction.createdAt)}</p>
        ) : null}
      </div>
    </Link>
  );
}
