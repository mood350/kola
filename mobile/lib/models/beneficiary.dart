/// Réseau de Mobile Money supporté (cf. backend MobileNetwork).
enum MobileNetwork {
  mixxByYas,
  moovTogo,
  wave,
  orangeMoney,
  freeMoney,
  mtnMomo,
  vodafoneCash,
  airtelTigo,
  opay,
  palmpay,
  westernUnion,
  moneygram;

  static MobileNetwork fromBackend(String value) {
    switch (value) {
      case 'MIXX_BY_YAS':
        return MobileNetwork.mixxByYas;
      case 'MOOV_TOGO':
        return MobileNetwork.moovTogo;
      case 'WAVE':
        return MobileNetwork.wave;
      case 'ORANGE_MONEY':
        return MobileNetwork.orangeMoney;
      case 'FREE_MONEY':
        return MobileNetwork.freeMoney;
      case 'MTN_MOMO':
        return MobileNetwork.mtnMomo;
      case 'VODAFONE_CASH':
        return MobileNetwork.vodafoneCash;
      case 'AIRTELTIGO':
        return MobileNetwork.airtelTigo;
      case 'OPAY':
        return MobileNetwork.opay;
      case 'PALMPAY':
        return MobileNetwork.palmpay;
      case 'WESTERN_UNION':
        return MobileNetwork.westernUnion;
      case 'MONEYGRAM':
        return MobileNetwork.moneygram;
      default:
        throw ArgumentError('Réseau Mobile Money inconnu : $value');
    }
  }

  /// Valeur exacte attendue par le backend (CreateBeneficiaryRequest.network).
  String toBackend() {
    switch (this) {
      case MobileNetwork.mixxByYas:
        return 'MIXX_BY_YAS';
      case MobileNetwork.moovTogo:
        return 'MOOV_TOGO';
      case MobileNetwork.wave:
        return 'WAVE';
      case MobileNetwork.orangeMoney:
        return 'ORANGE_MONEY';
      case MobileNetwork.freeMoney:
        return 'FREE_MONEY';
      case MobileNetwork.mtnMomo:
        return 'MTN_MOMO';
      case MobileNetwork.vodafoneCash:
        return 'VODAFONE_CASH';
      case MobileNetwork.airtelTigo:
        return 'AIRTELTIGO';
      case MobileNetwork.opay:
        return 'OPAY';
      case MobileNetwork.palmpay:
        return 'PALMPAY';
      case MobileNetwork.westernUnion:
        return 'WESTERN_UNION';
      case MobileNetwork.moneygram:
        return 'MONEYGRAM';
    }
  }

  /// Libellé affichable en français.
  String get label {
    switch (this) {
      case MobileNetwork.mixxByYas:
        return 'Mixx by Yas';
      case MobileNetwork.moovTogo:
        return 'Moov Togo';
      case MobileNetwork.wave:
        return 'Wave';
      case MobileNetwork.orangeMoney:
        return 'Orange Money';
      case MobileNetwork.freeMoney:
        return 'Free Money';
      case MobileNetwork.mtnMomo:
        return 'MTN MoMo';
      case MobileNetwork.vodafoneCash:
        return 'Vodafone Cash';
      case MobileNetwork.airtelTigo:
        return 'AirtelTigo';
      case MobileNetwork.opay:
        return 'OPay';
      case MobileNetwork.palmpay:
        return 'PalmPay';
      case MobileNetwork.westernUnion:
        return 'Western Union';
      case MobileNetwork.moneygram:
        return 'MoneyGram';
    }
  }
}

/// Modèle représentant un bénéficiaire (cf. BeneficiaryResponse backend).
class Beneficiary {
  final int id;
  final String alias;
  final String phoneNumber;
  final String countryCode;
  final MobileNetwork network;

  const Beneficiary({
    required this.id,
    required this.alias,
    required this.phoneNumber,
    required this.countryCode,
    required this.network,
  });

  factory Beneficiary.fromJson(Map<String, dynamic> json) {
    return Beneficiary(
      id: json['id'] as int,
      alias: json['alias'] as String? ?? '',
      phoneNumber: json['phoneNumber'] as String? ?? '',
      countryCode: json['countryCode'] as String? ?? '',
      network: MobileNetwork.fromBackend(json['network'] as String),
    );
  }
}
