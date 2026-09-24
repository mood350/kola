import 'json.dart';

/// Client KOLA connecté.
class KolaUser {
  const KolaUser({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.phone,
    this.email,
    required this.kycTier,
    required this.phoneVerified,
    required this.emailVerified,
  });

  final String id;
  final String firstName;
  final String lastName;
  final String phone;
  final String? email;
  final String kycTier;
  final bool phoneVerified;
  final bool emailVerified;

  String get fullName => '$firstName $lastName'.trim();

  /// "TIER_2" → "TIER 2", tel qu'affiché dans l'en-tête.
  String get tierLabel => kycTier.replaceAll('_', ' ');

  /// Les 8 chiffres significatifs, pour préremplir un champ national.
  String get localPhone {
    final digits = phone.replaceAll(RegExp(r'\D'), '');
    return digits.length <= 8 ? digits : digits.substring(digits.length - 8);
  }

  factory KolaUser.fromJson(Map<String, dynamic> json) {
    return KolaUser(
      id: asString(json['id']),
      firstName: asString(json['firstName']),
      lastName: asString(json['lastName']),
      phone: asString(json['phone']),
      email: asStringOrNull(json['email']),
      kycTier: asString(json['kycTier'], 'TIER_0'),
      phoneVerified: asBool(json['phoneVerified']),
      emailVerified: asBool(json['emailVerified']),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'firstName': firstName,
    'lastName': lastName,
    'phone': phone,
    'email': email,
    'kycTier': kycTier,
    'phoneVerified': phoneVerified,
    'emailVerified': emailVerified,
  };
}

/// Jeton d'accès, jeton de renouvellement et profil, tels que renvoyés par
/// `/api/v1/auth/login` et `/api/v1/auth/register`.
class AuthSession {
  const AuthSession({
    required this.accessToken,
    required this.refreshToken,
    required this.user,
  });

  final String accessToken;
  final String refreshToken;
  final KolaUser user;

  factory AuthSession.fromJson(Map<String, dynamic> json) {
    return AuthSession(
      accessToken: asString(json['accessToken']),
      refreshToken: asString(json['refreshToken']),
      user: KolaUser.fromJson(
        (json['user'] as Map?)?.cast<String, dynamic>() ?? const {},
      ),
    );
  }
}

/// Destinataire confirmé, tel qu'affiché avant un envoi P2P
/// (`GET /api/v1/users/recipients/{phone}`).
class RecipientLookup {
  const RecipientLookup({
    required this.id,
    required this.displayName,
    required this.maskedPhone,
  });

  final String id;
  final String displayName;
  final String maskedPhone;

  factory RecipientLookup.fromJson(Map<String, dynamic> json) {
    return RecipientLookup(
      id: asString(json['id']),
      displayName: asString(json['displayName']),
      maskedPhone: asString(json['maskedPhone']),
    );
  }
}

/// Résolution plus permissive côté transaction : elle répond aussi pour un
/// numéro qui n'a pas encore de compte KOLA (`registered == false`), ce que
/// l'endpoint `users/recipients` refuse.
class TransferRecipientLookup {
  const TransferRecipientLookup({
    required this.phone,
    required this.phoneMasked,
    required this.registered,
    required this.self,
    this.name,
  });

  final String phone;
  final String phoneMasked;
  final bool registered;
  final bool self;
  final String? name;

  factory TransferRecipientLookup.fromJson(Map<String, dynamic> json) {
    return TransferRecipientLookup(
      phone: asString(json['phone']),
      phoneMasked: asString(json['phoneMasked']),
      registered: asBool(json['registered']),
      self: asBool(json['self']),
      name: asStringOrNull(json['name']),
    );
  }
}
