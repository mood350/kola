/// Type de notification (cf. backend NotificationType).
enum AppNotificationType {
  transaction,
  security,
  system;

  static AppNotificationType fromBackend(String value) {
    switch (value) {
      case 'TRANSACTION':
        return AppNotificationType.transaction;
      case 'SECURITY':
        return AppNotificationType.security;
      case 'SYSTEM':
        return AppNotificationType.system;
      default:
        return AppNotificationType.system;
    }
  }
}

/// Modèle représentant une notification in-app (cf. NotificationResponse
/// backend). Nommée `AppNotification` (pas `Notification`) pour éviter le
/// conflit avec la classe `Notification` du framework Flutter
/// (`package:flutter/widgets.dart`, utilisée par `NotificationListener`).
class AppNotification {
  final int id;
  final String title;
  final String body;
  final AppNotificationType type;
  final bool read;
  final DateTime createdAt;

  const AppNotification({
    required this.id,
    required this.title,
    required this.body,
    required this.type,
    required this.read,
    required this.createdAt,
  });

  factory AppNotification.fromJson(Map<String, dynamic> json) {
    return AppNotification(
      id: json['id'] as int,
      title: json['title'] as String? ?? '',
      body: json['body'] as String? ?? '',
      type: AppNotificationType.fromBackend(
        json['type'] as String? ?? 'SYSTEM',
      ),
      read: json['read'] as bool? ?? false,
      createdAt:
          DateTime.tryParse(json['createdAt'] as String? ?? '') ??
          DateTime.now(),
    );
  }
}
