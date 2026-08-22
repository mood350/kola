import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/notification.dart';
import 'package:mobile/models/page_response.dart';
import 'package:mobile/providers/notification_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/notification_service.dart';
import 'package:mocktail/mocktail.dart';

class MockNotificationService extends Mock implements NotificationService {}

AppNotification _notification({int id = 1, bool read = false}) =>
    AppNotification(
      id: id,
      title: 'Dépôt effectué',
      body: 'Votre wallet a été crédité de 5000 XOF.',
      type: AppNotificationType.transaction,
      read: read,
      createdAt: DateTime(2026, 1, 1),
    );

void main() {
  late MockNotificationService service;
  late NotificationProvider provider;

  setUp(() {
    service = MockNotificationService();
    provider = NotificationProvider(notificationService: service);
  });

  test('loadUnreadCount() populates unreadCount', () async {
    when(
      () => service.getUnreadCount(),
    ).thenAnswer((_) async => const ApiResult.ok(3));

    await provider.loadUnreadCount();

    expect(provider.unreadCount, 3);
  });

  test('loadNotifications() populates the list and hasMore', () async {
    when(() => service.getMyNotifications(page: 0)).thenAnswer(
      (_) async => ApiResult.ok(
        PageResponse<AppNotification>(
          content: [_notification()],
          totalElements: 2,
          totalPages: 2,
          number: 0,
          size: 1,
          last: false,
        ),
      ),
    );

    await provider.loadNotifications();

    expect(provider.notifications, hasLength(1));
    expect(provider.hasMore, true);
  });

  test('markAsRead() flips the read flag and decrements unreadCount', () async {
    when(() => service.getMyNotifications(page: 0)).thenAnswer(
      (_) async => ApiResult.ok(
        PageResponse<AppNotification>(
          content: [_notification()],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 1,
          last: true,
        ),
      ),
    );
    await provider.loadNotifications();

    when(
      () => service.getUnreadCount(),
    ).thenAnswer((_) async => const ApiResult.ok(1));
    await provider.loadUnreadCount();

    when(
      () => service.markAsRead(1),
    ).thenAnswer((_) async => const ApiResult<void>.ok(null));

    await provider.markAsRead(1);

    expect(provider.notifications.first.read, true);
    expect(provider.unreadCount, 0);
  });

  test(
    'markAllAsRead() marks every notification read and zeroes unreadCount',
    () async {
      when(() => service.getMyNotifications(page: 0)).thenAnswer(
        (_) async => ApiResult.ok(
          PageResponse<AppNotification>(
            content: [_notification(id: 1), _notification(id: 2)],
            totalElements: 2,
            totalPages: 1,
            number: 0,
            size: 2,
            last: true,
          ),
        ),
      );
      await provider.loadNotifications();

      when(
        () => service.markAllAsRead(),
      ).thenAnswer((_) async => const ApiResult<void>.ok(null));

      await provider.markAllAsRead();

      expect(provider.notifications.every((n) => n.read), true);
      expect(provider.unreadCount, 0);
    },
  );
}
