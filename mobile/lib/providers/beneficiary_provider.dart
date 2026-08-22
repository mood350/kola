import 'package:flutter/foundation.dart';
import '../models/beneficiary.dart';
import '../services/beneficiary_service.dart';

/// Provider gérant la liste des bénéficiaires (nécessaire au flux de transfert,
/// qui exige un beneficiaryId pré-enregistré côté backend).
class BeneficiaryProvider extends ChangeNotifier {
  BeneficiaryProvider({BeneficiaryService? beneficiaryService})
    : _service = beneficiaryService ?? BeneficiaryService();

  final BeneficiaryService _service;

  List<Beneficiary> _beneficiaries = [];
  bool _isLoading = false;
  bool _isSubmitting = false;
  String? _errorMessage;

  List<Beneficiary> get beneficiaries => _beneficiaries;
  bool get isLoading => _isLoading;
  bool get isSubmitting => _isSubmitting;
  String? get errorMessage => _errorMessage;

  Future<void> loadBeneficiaries() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    final result = await _service.getMyBeneficiaries();
    if (result.success) {
      _beneficiaries = result.data ?? [];
    } else {
      _errorMessage =
          result.error?.message ?? 'Impossible de charger les bénéficiaires';
    }
    _isLoading = false;
    notifyListeners();
  }

  Future<Beneficiary?> addBeneficiary({
    required String alias,
    required String phoneNumber,
    required String countryCode,
    required MobileNetwork network,
  }) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    final result = await _service.createBeneficiary(
      alias: alias,
      phoneNumber: phoneNumber,
      countryCode: countryCode,
      network: network,
    );

    if (result.success) {
      await loadBeneficiaries();
      _isSubmitting = false;
      notifyListeners();
      return result.data;
    }
    _errorMessage =
        result.error?.message ?? "Impossible d'ajouter le bénéficiaire";
    _isSubmitting = false;
    notifyListeners();
    return null;
  }

  Future<bool> removeBeneficiary(int id) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();

    final result = await _service.deleteBeneficiary(id);
    if (result.success) {
      _beneficiaries.removeWhere((b) => b.id == id);
      _isSubmitting = false;
      notifyListeners();
      return true;
    }
    _errorMessage =
        result.error?.message ?? 'Impossible de supprimer le bénéficiaire';
    _isSubmitting = false;
    notifyListeners();
    return false;
  }

  void clear() {
    _beneficiaries = [];
    _errorMessage = null;
    notifyListeners();
  }
}
