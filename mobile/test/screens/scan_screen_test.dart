import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/screens/scan_screen.dart';

void main() {
  group('togolesePhone', () {
    test('lit le QR KOLA au format JSON', () {
      expect(
        togolesePhone('{"type":"KOLA_P2P","phone":"+22890112233"}'),
        '+22890112233',
      );
    });

    test('accepte les anciennes clés du QR', () {
      expect(togolesePhone('{"recipientPhone":"90112233"}'), '+22890112233');
      expect(togolesePhone('{"account":"90112233"}'), '+22890112233');
    });

    test('extrait le numéro d une URL de paiement', () {
      expect(
        togolesePhone('https://kola.tg/pay?amount=500&phone=90112233'),
        '+22890112233',
      );
    });

    test('accepte un numéro brut, avec ou sans indicatif', () {
      expect(togolesePhone('90 11 22 33'), '+22890112233');
      expect(togolesePhone('+228 90 11 22 33'), '+22890112233');
    });

    // Un QR d'une autre application ne doit pas produire un numéro
    // approximatif : mieux vaut refuser que d'envoyer de l'argent à côté.
    test('refuse ce qui n est pas un numéro togolais', () {
      expect(togolesePhone('https://exemple.com'), isNull);
      expect(togolesePhone('12345'), isNull);
      expect(togolesePhone('+33612345678'), isNull);
      expect(togolesePhone(''), isNull);
    });
  });
}
