import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/utils/idempotency_key.dart';

void main() {
  late IdempotencyKeyHolder holder;

  setUp(() => holder = IdempotencyKeyHolder());

  test('la meme intention rend toujours la meme cle', () {
    final premiere = holder.forIntent('wallet-1:benef-7:5000');
    final apresEchec = holder.forIntent('wallet-1:benef-7:5000');

    // Le cas qui coutait de l'argent : apres un timeout, l'app ignore si le
    // virement est passe. Rejouer la meme cle laisse le backend repondre avec
    // la transaction d'origine au lieu d'en creer une seconde.
    expect(apresEchec, premiere);
  });

  test('une intention differente rend une cle differente', () {
    final avant = holder.forIntent('wallet-1:benef-7:5000');
    final apresCorrection = holder.forIntent('wallet-1:benef-7:500');

    // Sans ca, corriger 5 000 en 500 puis renvoyer afficherait « succes »
    // alors que le backend aurait rejoue le virement de 5 000.
    expect(apresCorrection, isNot(avant));
  });

  test('reset() force une cle neuve pour une intention identique', () {
    final premierVirement = holder.forIntent('wallet-1:benef-7:5000');
    holder.reset();
    final secondVirement = holder.forIntent('wallet-1:benef-7:5000');

    // Deux virements volontairement identiques doivent bien partir deux fois.
    expect(secondVirement, isNot(premierVirement));
  });

  test('deux porteurs ne partagent jamais la meme cle', () {
    final cles = List.generate(
      200,
      (_) => IdempotencyKeyHolder().forIntent('meme-intention'),
    );

    expect(cles.toSet().length, 200);
  });
}
