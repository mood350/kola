package com.kola.backend.modules.admin.service;

import com.kola.backend.common.enums.TransactionStatus;
import com.kola.backend.common.enums.TransactionType;

/**
 * French labels for the accounting export — the same wording as the console, so a line in the
 * spreadsheet reads like the screen the accountant filtered it from.
 */
final class ExportLabels {

    private ExportLabels() {
    }

    static String type(TransactionType type) {
        return switch (type) {
            case CASH_IN -> "Dépôt";
            case CASH_OUT -> "Retrait";
            case P2P_TRANSFER -> "Transfert";
            case MERCHANT_PAYMENT -> "Paiement marchand";
            case BILL_PAYMENT -> "Facture";
            case VAULT_DEPOSIT -> "Vers un coffre";
            case VAULT_WITHDRAWAL -> "Depuis un coffre";
            case SAVINGS_DEPOSIT -> "Vers l'épargne";
            case SAVINGS_WITHDRAWAL -> "Depuis l'épargne";
            case LOAN_DISBURSEMENT -> "Versement de prêt";
            case LOAN_REPAYMENT -> "Remboursement";
            case CHARGEBACK -> "Contre-passation";
        };
    }

    static String status(TransactionStatus status) {
        return switch (status) {
            case PENDING -> "En attente";
            case COMPLETED -> "Réussie";
            case FAILED -> "Échouée";
            case REVERSED -> "Contre-passée";
        };
    }

    /** The outside end of a Mobile Money movement is stored as "EXTERNAL". */
    static String counterparty(String name, String raw) {
        String value = name != null ? name : raw;
        return "EXTERNAL".equals(value) ? "Extérieur (Mobile Money)" : value;
    }
}
