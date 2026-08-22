import 'dart:math';

/// Porte la clé d'idempotence d'un écran qui déplace de l'argent.
///
/// Une clé identifie UNE intention de paiement : le backend rejoue la réponse
/// d'origine pour toute requête qui la réutilise, sans refaire le mouvement
/// (cf. `TransactionService.findReplay` + la contrainte unique sur
/// `Transaction.idempotencyKey`). Deux règles opposées en découlent, et c'est
/// pour les tenir ensemble que cette classe existe plutôt qu'un simple
/// générateur :
///
///  1. la clé doit SURVIVRE au re-tap sur « Réessayer » après un timeout —
///     le seul cas où l'app ignore si l'argent est parti. La générer à chaque
///     appel du provider (l'ancien comportement) ne protégeait donc que des
///     rejeux réseau, pas de l'utilisateur ;
///  2. elle doit CHANGER dès que l'intention change. Rejouer la clé d'un
///     virement de 5 000 après que l'utilisateur a corrigé le montant à 500
///     renverrait « succès » en ayant bel et bien envoyé 5 000.
///
/// L'écran en garde une instance dans son `State` : c'est sa durée de vie qui
/// délimite l'intention.
class IdempotencyKeyHolder {
  String? _key;
  String? _intent;

  /// Clé stable tant que [intent] — la signature des champs envoyés — ne
  /// bouge pas ; clé neuve dès qu'il change.
  String forIntent(String intent) {
    if (_key == null || _intent != intent) {
      _intent = intent;
      _key = _generate();
    }
    return _key!;
  }

  /// À appeler après un succès confirmé : l'intention est consommée. Sans ça,
  /// un second virement volontairement identique (même bénéficiaire, même
  /// montant) réutiliserait la clé et le backend rejouerait le premier au lieu
  /// d'en exécuter un second.
  void reset() {
    _key = null;
    _intent = null;
  }

  /// 128 bits d'aléa cryptographique, préfixés d'un horodatage pour rester
  /// lisibles dans les logs et triables côté support.
  static String _generate() {
    final random = Random.secure();
    final suffix = List.generate(
      4,
      (_) => random.nextInt(1 << 32),
    ).map((n) => n.toRadixString(16).padLeft(8, '0')).join();
    return '${DateTime.now().microsecondsSinceEpoch}-$suffix';
  }
}
