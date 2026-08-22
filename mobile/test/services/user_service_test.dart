import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/user.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/user_service.dart';
import 'package:mocktail/mocktail.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  late MockApiClient apiClient;
  late UserService service;

  setUp(() {
    apiClient = MockApiClient();
    service = UserService(apiClient: apiClient);
  });

  test('getMe() calls GET /users/me and decodes the profile', () async {
    when(
      () => apiClient.get<User>('/users/me', decode: any(named: 'decode')),
    ).thenAnswer((invocation) async {
      final decode =
          invocation.namedArguments[#decode] as User Function(dynamic);
      return ApiResult.ok(
        decode({
          'id': 1,
          'firstName': 'Ama',
          'lastName': 'Koffi',
          'email': 'ama@example.com',
          'phoneNumber': '+22890000000',
          'countryCode': 'TG',
          'kycLevel': 'TIER_1',
          'createdAt': '2026-01-01T00:00:00',
        }),
      );
    });

    final result = await service.getMe();

    expect(result.success, true);
    expect(result.data!.fullName, 'Ama Koffi');
    expect(result.data!.kycLevel, 'TIER_1');
  });

  test('propagates errors from ApiClient', () async {
    when(
      () => apiClient.get<User>('/users/me', decode: any(named: 'decode')),
    ).thenAnswer(
      (_) async => const ApiResult.fail(
        ApiException(code: 'TOKEN_EXPIRED', message: 'Session expirée'),
      ),
    );

    final result = await service.getMe();

    expect(result.success, false);
    expect(result.error?.code, 'TOKEN_EXPIRED');
  });
}
