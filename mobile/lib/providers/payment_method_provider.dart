import 'package:flutter/foundation.dart';
import '../models/payment_method.dart';
import '../services/payment_service.dart';

/// Moyens de paiement proposés par le backend.
///
/// Chargés une fois et conservés : la liste des opérateurs ne change pas d'une
/// minute à l'autre, et la recharger à chaque ouverture de la feuille de dépôt
/// ferait clignoter l'écran pour rien.
class PaymentMethodProvider extends ChangeNotifier {
  PaymentMethodProvider({PaymentService? paymentService})
    : _service = paymentService ?? PaymentService();

  final PaymentService _service;

  PaymentMethods? _methods;
  bool _isLoading = false;
  String? _error;

  PaymentMethods? get methods => _methods;
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Vrai si un vrai paiement est possible sur ce serveur.
  ///
  /// Faux tant que le prestataire n'est pas configuré : l'écran doit alors
  /// n'offrir que le mode test, plutôt que de proposer un opérateur pour
  /// refuser la demande ensuite.
  bool get providerEnabled => _methods?.providerEnabled ?? false;

  List<PaymentMethod> get available => _methods?.available ?? const [];

  Future<void> load({String? country, bool force = false}) async {
    if (_methods != null && !force) return;

    _isLoading = true;
    _error = null;
    notifyListeners();

    final result = await _service.getMethods(country: country);
    if (result.success) {
      _methods = result.data;
    } else {
      _error = result.error?.message ?? 'Moyens de paiement indisponibles';
    }
    _isLoading = false;
    notifyListeners();
  }
}
