/// Modèle représentant le profil utilisateur Kola,
/// tel que renvoyé par GET /api/users/me.
class User {
  final int id;
  final String firstName;
  final String lastName;
  final String? email;
  final String phoneNumber;
  final String countryCode;
  final String kycLevel; // TIER_0, TIER_1, TIER_2, TIER_3
  final String? avatar; // identifiant d'avatar prédéfini, ex: "avatar_03"
  final String? lastKnownIp;
  final String? lastKnownUserAgent;
  final DateTime? createdAt;

  const User({
    required this.id,
    required this.firstName,
    required this.lastName,
    this.email,
    required this.phoneNumber,
    required this.countryCode,
    required this.kycLevel,
    this.avatar,
    this.lastKnownIp,
    this.lastKnownUserAgent,
    this.createdAt,
  });

  String get fullName => '$firstName $lastName';

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as int,
      firstName: json['firstName'] as String? ?? '',
      lastName: json['lastName'] as String? ?? '',
      email: json['email'] as String?,
      phoneNumber: json['phoneNumber'] as String? ?? '',
      countryCode: json['countryCode'] as String? ?? '',
      kycLevel: json['kycLevel'] as String? ?? 'TIER_0',
      avatar: json['avatar'] as String?,
      lastKnownIp: json['lastKnownIp'] as String?,
      lastKnownUserAgent: json['lastKnownUserAgent'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
    );
  }
}
