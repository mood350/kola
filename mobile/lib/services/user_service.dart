import '../models/user.dart';
import 'api_client.dart';

/// Service gérant les appels API liés au profil utilisateur.
class UserService {
  UserService({ApiClient? apiClient}) : _api = apiClient ?? ApiClient();

  final ApiClient _api;

  /// GET /api/users/me
  Future<ApiResult<User>> getMe() {
    return _api.get<User>(
      '/users/me',
      decode: (json) => User.fromJson(json as Map<String, dynamic>),
    );
  }

  /// PUT /api/users/me
  Future<ApiResult<User>> updateProfile({
    required String firstName,
    required String lastName,
    required String phoneNumber,
    String? avatar,
  }) {
    return _api.put<User>(
      '/users/me',
      body: {
        'firstName': firstName,
        'lastName': lastName,
        'phoneNumber': phoneNumber,
        'avatar': ?avatar,
      },
      decode: (json) => User.fromJson(json as Map<String, dynamic>),
    );
  }

  /// PUT /api/users/me/avatar
  Future<ApiResult<User>> updateAvatar(String avatar) {
    return _api.put<User>(
      '/users/me/avatar',
      body: {'avatar': avatar},
      decode: (json) => User.fromJson(json as Map<String, dynamic>),
    );
  }

  /// POST /api/users/me/password
  Future<ApiResult<void>> changePassword({
    required String currentPassword,
    required String newPassword,
  }) {
    return _api.postEmpty(
      '/users/me/password',
      body: {'currentPassword': currentPassword, 'newPassword': newPassword},
    );
  }
}
