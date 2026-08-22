import 'package:flutter/foundation.dart';
import '../models/user.dart';
import '../services/api_client.dart';
import '../services/user_service.dart';

enum UserLoadStatus { initial, loading, loaded, error }

/// Provider gérant le profil utilisateur courant (GET /api/users/me).
class UserProvider extends ChangeNotifier {
  UserProvider({UserService? userService})
    : _userService = userService ?? UserService();

  final UserService _userService;

  User? _user;
  UserLoadStatus _status = UserLoadStatus.initial;
  String? _errorMessage;

  User? get user => _user;
  UserLoadStatus get status => _status;
  String? get errorMessage => _errorMessage;

  Future<void> loadMe() async {
    _status = UserLoadStatus.loading;
    notifyListeners();

    final result = await _userService.getMe();
    if (result.success) {
      _user = result.data;
      _status = UserLoadStatus.loaded;
    } else {
      _errorMessage =
          result.error?.message ?? 'Impossible de charger le profil';
      _status = UserLoadStatus.error;
    }
    notifyListeners();
  }

  bool _isSubmitting = false;
  bool get isSubmitting => _isSubmitting;

  Future<bool> updateProfile({
    required String firstName,
    required String lastName,
    required String phoneNumber,
    String? avatar,
  }) async {
    return _mutate(
      () => _userService.updateProfile(
        firstName: firstName,
        lastName: lastName,
        phoneNumber: phoneNumber,
        avatar: avatar,
      ),
    );
  }

  Future<bool> updateAvatar(String avatar) async {
    return _mutate(() => _userService.updateAvatar(avatar));
  }

  /// Le changement de mot de passe ne renvoie pas de profil (204) :
  /// on ne met donc pas `_user` à jour ici.
  Future<bool> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    final result = await _userService.changePassword(
      currentPassword: currentPassword,
      newPassword: newPassword,
    );
    if (!result.success) {
      _errorMessage =
          result.error?.message ?? 'Impossible de changer le mot de passe';
    }
    _isSubmitting = false;
    notifyListeners();
    return result.success;
  }

  Future<bool> _mutate(Future<ApiResult<User>> Function() action) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    final result = await action();
    if (result.success) {
      _user = result.data;
      _status = UserLoadStatus.loaded;
    } else {
      _errorMessage = result.error?.message ?? "Échec de la mise à jour";
    }
    _isSubmitting = false;
    notifyListeners();
    return result.success;
  }

  /// Appelé à la déconnexion pour ne pas garder le profil du user précédent.
  void clear() {
    _user = null;
    _status = UserLoadStatus.initial;
    _errorMessage = null;
    notifyListeners();
  }
}
