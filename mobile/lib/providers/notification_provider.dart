import 'package:flutter/foundation.dart';
import '../models/notification.dart';
import '../services/notification_service.dart';

/// Provider gérant la liste des notifications in-app et le badge non-lu
/// affiché sur la cloche de Home.
class NotificationProvider extends ChangeNotifier {
  NotificationProvider({NotificationService? notificationService})
    : _service = notificationService ?? NotificationService();

  final NotificationService _service;

  List<AppNotification> _notifications = [];
  bool _isLoading = false;
  bool _hasMore = true;
  int _page = 0;
  String? _errorMessage;

  int _unreadCount = 0;

  List<AppNotification> get notifications => _notifications;
  bool get isLoading => _isLoading;
  bool get hasMore => _hasMore;
  String? get errorMessage => _errorMessage;
  int get unreadCount => _unreadCount;

  Future<void> loadUnreadCount() async {
    final result = await _service.getUnreadCount();
    if (result.success) {
      _unreadCount = result.data ?? 0;
      notifyListeners();
    }
  }

  Future<void> loadNotifications() async {
    _notifications = [];
    _page = 0;
    _hasMore = true;
    await _fetchPage();
  }

  Future<void> loadMore() async {
    if (_isLoading || !_hasMore) return;
    await _fetchPage();
  }

  Future<void> _fetchPage() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    final result = await _service.getMyNotifications(page: _page);
    if (result.success) {
      final page = result.data!;
      _notifications.addAll(page.content);
      _hasMore = !page.last;
      _page++;
    } else {
      _errorMessage =
          result.error?.message ?? 'Impossible de charger les notifications';
    }
    _isLoading = false;
    notifyListeners();
  }

  Future<void> markAsRead(int id) async {
    final index = _notifications.indexWhere((n) => n.id == id);
    if (index == -1 || _notifications[index].read) return;

    final result = await _service.markAsRead(id);
    if (result.success) {
      _notifications[index] = AppNotification(
        id: _notifications[index].id,
        title: _notifications[index].title,
        body: _notifications[index].body,
        type: _notifications[index].type,
        read: true,
        createdAt: _notifications[index].createdAt,
      );
      if (_unreadCount > 0) _unreadCount--;
      notifyListeners();
    }
  }

  Future<void> markAllAsRead() async {
    final result = await _service.markAllAsRead();
    if (result.success) {
      _notifications = _notifications
          .map(
            (n) => AppNotification(
              id: n.id,
              title: n.title,
              body: n.body,
              type: n.type,
              read: true,
              createdAt: n.createdAt,
            ),
          )
          .toList();
      _unreadCount = 0;
      notifyListeners();
    }
  }

  void clear() {
    _notifications = [];
    _unreadCount = 0;
    _page = 0;
    _hasMore = true;
    _errorMessage = null;
    notifyListeners();
  }
}
