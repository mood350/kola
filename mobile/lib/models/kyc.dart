import 'json.dart';

/// Pièce justificative transmise pour la vérification d'identité.
class KycDocument {
  const KycDocument({
    required this.id,
    required this.type,
    required this.status,
    required this.originalFilename,
    required this.sizeBytes,
    this.rejectionReason,
    this.submittedAt,
    this.reviewedAt,
  });

  final String id;
  final String type;
  final String status;
  final String originalFilename;
  final int sizeBytes;
  final String? rejectionReason;
  final DateTime? submittedAt;
  final DateTime? reviewedAt;

  bool get isApproved => status == 'APPROVED';
  bool get isRejected => status == 'REJECTED';

  static const Map<String, String> typeLabels = {
    'NATIONAL_ID': "Carte nationale d'identité",
    'PASSPORT': 'Passeport',
    'DRIVING_LICENCE': 'Permis de conduire',
    'VOTER_CARD': "Carte d'électeur",
    'SELFIE': 'Photo de visage',
    'PROOF_OF_ADDRESS': 'Justificatif de domicile',
  };

  String get typeLabel => typeLabels[type] ?? type;

  String get statusLabel => switch (status) {
    'APPROVED' => 'Validé',
    'REJECTED' => 'Refusé',
    _ => 'En cours d\'examen',
  };

  factory KycDocument.fromJson(Map<String, dynamic> json) {
    return KycDocument(
      id: asString(json['id']),
      type: asString(json['type']),
      status: asString(json['status'], 'PENDING'),
      originalFilename: asString(json['originalFilename']),
      sizeBytes: asInt(json['sizeBytes']),
      rejectionReason: asStringOrNull(json['rejectionReason']),
      submittedAt: asDate(json['submittedAt']),
      reviewedAt: asDate(json['reviewedAt']),
    );
  }
}

/// Plafonds associés au palier KYC courant. Un plafond nul vaut "illimité".
class KycLimits {
  const KycLimits({this.perTransaction, this.daily, this.monthly});

  final double? perTransaction;
  final double? daily;
  final double? monthly;

  factory KycLimits.fromJson(Map<String, dynamic> json) {
    double? read(dynamic value) => value == null ? null : asDouble(value);
    return KycLimits(
      perTransaction: read(json['perTransaction']),
      daily: read(json['daily']),
      monthly: read(json['monthly']),
    );
  }
}

/// État de la vérification d'identité et chemin vers le palier suivant.
class KycStatus {
  const KycStatus({
    required this.tier,
    required this.phoneVerified,
    required this.profileComplete,
    required this.identityDocumentApproved,
    required this.requirementsForNextTier,
    required this.documents,
    required this.limits,
    this.nextTier,
  });

  final String tier;
  final bool phoneVerified;
  final bool profileComplete;
  final bool identityDocumentApproved;
  final List<String> requirementsForNextTier;
  final List<KycDocument> documents;
  final KycLimits limits;
  final String? nextTier;

  String get tierLabel => tier.replaceAll('_', ' ');

  factory KycStatus.fromJson(Map<String, dynamic> json) {
    return KycStatus(
      tier: asString(json['tier'], 'TIER_0'),
      phoneVerified: asBool(json['phoneVerified']),
      profileComplete: asBool(json['profileComplete']),
      identityDocumentApproved: asBool(json['identityDocumentApproved']),
      requirementsForNextTier: asStringList(json['requirementsForNextTier']),
      documents: asList(json['documents'], KycDocument.fromJson),
      limits: KycLimits.fromJson(
        (json['limits'] as Map?)?.cast<String, dynamic>() ?? const {},
      ),
      nextTier: asStringOrNull(json['nextTier']),
    );
  }
}
