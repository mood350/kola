import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/notification.dart';
import 'package:mobile/models/page_response.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/notification_service.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  late MockApiClient apiClient;
  late NotificationService service;

  setUp(() {
    apiClient = MockApiClient();
    service = NotificationService(apiClient: apiClient);
  });

  test('getMyNotifications() decodes a paginated response', () async {
    when(
      () => apiClient.get<PageResponse<AppNotification>>(
        '/notifications',
        query: {'page': '0', 'size': '20'},
        decode: any(named: 'decode'),
      ),
    ).thenAnswer((invocation) async {
      final decode =
          invocation.namedArguments[#decode]
              as PageResponse<AppNotification> Function(dynamic);
      return ApiResult.ok(
        decode({
          'content': [
            {
              'id': 1,
              'title': 'Dépôt effectué',
              'body': 'Votre wallet a été crédité de 5000 XOF.',
              'type': 'TRANSACTION',
              'read': false,
              'createdAt': '2026-01-01T10:00:00',
            },
          ],
          'totalElements': 1,
          'totalPages': 1,
          'number': 0,
          'size': 20,
          'last': true,
        }),
      );
    });

    final result = await service.getMyNotifications();

    expect(result.success, true);
    expect(result.data!.content, hasLength(1));
    expect(result.data!.content.first.type, AppNotificationType.transaction);
    expect(result.data!.content.first.read, false);
  });

  test('getUnreadCount() decodes the count field', () async {
    when(
      () => apiClient.get<int>(
        '/notifications/unread-count',
        decode: any(named: 'decode'),
      ),
    ).thenAnswer((invocation) async {
      final decode =
          invocation.namedArguments[#decode] as int Function(dynamic);
      return ApiResult.ok(decode({'count': 3}));
    });

    final result = await service.getUnreadCount();

    expect(result.success, true);
    expect(result.data, 3);
  });

  test('markAllAsRead() calls postEmpty on /notifications/read-all', () async {
    when(
      () => apiClient.postEmpty('/notifications/read-all'),
    ).thenAnswer((_) async => const ApiResult<void>.ok(null));

    final result = await service.markAllAsRead();

    expect(result.success, true);
  });
}
