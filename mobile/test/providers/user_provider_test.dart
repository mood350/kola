import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/user.dart';
import 'package:mobile/providers/user_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/user_service.dart';
import 'package:mocktail/mocktail.dart';

class MockUserService extends Mock implements UserService {}

const _user = User(
  id: 1,
  firstName: 'Ama',
  lastName: 'Koffi',
  email: 'ama@example.com',
  phoneNumber: '+22890000000',
  countryCode: 'TG',
  kycLevel: 'TIER_1',
);

void main() {
  late MockUserService service;
  late UserProvider provider;

  setUp(() {
    service = MockUserService();
    provider = UserProvider(userService: service);
  });

  test('loadMe() transitions to loaded with the fetched user', () async {
    when(
      () => service.getMe(),
    ).thenAnswer((_) async => const ApiResult.ok(_user));

    expect(provider.status, UserLoadStatus.initial);

    await provider.loadMe();

    expect(provider.status, UserLoadStatus.loaded);
    expect(provider.user, _user);
    expect(provider.errorMessage, isNull);
  });

  test(
    'loadMe() transitions to error and keeps the message on failure',
    () async {
      when(() => service.getMe()).thenAnswer(
        (_) async => const ApiResult.fail(
          ApiException(code: 'TOKEN_EXPIRED', message: 'Session expirée'),
        ),
      );

      await provider.loadMe();

      expect(provider.status, UserLoadStatus.error);
      expect(provider.user, isNull);
      expect(provider.errorMessage, 'Session expirée');
    },
  );

  test('clear() resets the user and status', () async {
    when(
      () => service.getMe(),
    ).thenAnswer((_) async => const ApiResult.ok(_user));
    await provider.loadMe();

    provider.clear();

    expect(provider.user, isNull);
    expect(provider.status, UserLoadStatus.initial);
  });
}
