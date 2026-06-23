/// Modèle représentant un utilisateur Kola.
class User {
  final String id;
  final String fullName;
  final String phoneNumber;
  final String? email;
  final String kycTier; // TIER_0, TIER_1, TIER_2, TIER_3
  final int creditScore; // 0-100

  User({
    required this.id,
    required this.fullName,
    required this.phoneNumber,
    this.email,
    required this.kycTier,
    required this.creditScore,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as String,
      fullName: json['fullName'] as String,
      phoneNumber: json['phoneNumber'] as String,
      email: json['email'] as String?,
      kycTier: json['kycTier'] as String? ?? 'TIER_0',
      creditScore: json['creditScore'] as int? ?? 0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'fullName': fullName,
      'phoneNumber': phoneNumber,
      'email': email,
      'kycTier': kycTier,
      'creditScore': creditScore,
    };
  }
}