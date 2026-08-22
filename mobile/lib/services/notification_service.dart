import '../models/notification.dart';
import '../models/page_response.dart';
import 'api_client.dart';

/// Service gérant les appels API liés aux notifications in-app
/// (cf. NotificationController backend).
class NotificationService {
  NotificationService({ApiClient? apiClient}) : _api = apiClient ?? ApiClient();

  final ApiClient _api;

  Future<ApiResult<PageResponse<AppNotification>>> getMyNotifications({
    int page = 0,
    int size = 20,
  }) {
    return _api.get<PageResponse<AppNotification>>(
      '/notifications',
      query: {'page': '$page', 'size': '$size'},
      decode: (json) => PageResponse.fromJson(
        json as Map<String, dynamic>,
        AppNotification.fromJson,
      ),
    );
  }

  Future<ApiResult<int>> getUnreadCount() {
    return _api.get<int>(
      '/notifications/unread-count',
      decode: (json) => (json as Map<String, dynamic>)['count'] as int,
    );
  }

  Future<ApiResult<void>> markAsRead(int id) {
    return _api.postEmpty('/notifications/$id/read');
  }

  Future<ApiResult<void>> markAllAsRead() {
    return _api.postEmpty('/notifications/read-all');
  }
}
