import 'credit_score.dart';

/// Statut d'un prêt (cf. backend LoanStatus).
enum LoanStatus {
  pending,
  approved,
  rejected,
  disbursed,
  repaid,
  defaulted;

  static LoanStatus fromBackend(String value) {
    switch (value) {
      case 'PENDING':
        return LoanStatus.pending;
      case 'APPROVED':
        return LoanStatus.approved;
      case 'REJECTED':
        return LoanStatus.rejected;
      case 'DISBURSED':
        return LoanStatus.disbursed;
      case 'REPAID':
        return LoanStatus.repaid;
      case 'DEFAULTED':
        return LoanStatus.defaulted;
      default:
        return LoanStatus.pending;
    }
  }

  String get label {
    switch (this) {
      case LoanStatus.pending:
        return 'En attente';
      case LoanStatus.approved:
        return 'Approuvé';
      case LoanStatus.rejected:
        return 'Refusé';
      case LoanStatus.disbursed:
        return 'Décaissé';
      case LoanStatus.repaid:
        return 'Remboursé';
      case LoanStatus.defaulted:
        return 'En défaut';
    }
  }
}

/// Modèle représentant un prêt (cf. LoanDtos.LoanResponse backend).
class Loan {
  final int id;
  final double requestedAmount;
  final double totalRepayment;
  final double monthlyRate;
  final int durationMonths;
  final LoanStatus status;
  final String? purpose;
  final DateTime? dueDate;
  final String? rejectionReason;
  final int creditScoreAtRequest;
  final CreditTier tierAtRequest;

  const Loan({
    required this.id,
    required this.requestedAmount,
    required this.totalRepayment,
    required this.monthlyRate,
    required this.durationMonths,
    required this.status,
    this.purpose,
    this.dueDate,
    this.rejectionReason,
    required this.creditScoreAtRequest,
    required this.tierAtRequest,
  });

  factory Loan.fromJson(Map<String, dynamic> json) {
    return Loan(
      id: json['id'] as int,
      requestedAmount: (json['requestedAmount'] as num?)?.toDouble() ?? 0,
      totalRepayment: (json['totalRepayment'] as num?)?.toDouble() ?? 0,
      monthlyRate: (json['monthlyRate'] as num?)?.toDouble() ?? 0,
      durationMonths: json['durationMonths'] as int? ?? 0,
      status: LoanStatus.fromBackend(json['status'] as String? ?? 'PENDING'),
      purpose: json['purpose'] as String?,
      dueDate: json['dueDate'] != null
          ? DateTime.tryParse(json['dueDate'] as String)
          : null,
      rejectionReason: json['rejectionReason'] as String?,
      creditScoreAtRequest: json['creditScoreAtRequest'] as int? ?? 0,
      tierAtRequest: CreditTier.fromBackend(
        json['tierAtRequest'] as String? ?? 'INELIGIBLE',
      ),
    );
  }
}
