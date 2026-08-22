import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/primary_button.dart';
import '../../models/beneficiary.dart';
import '../../providers/beneficiary_provider.dart';

const _kCountries = {
  'SN': 'Sénégal',
  'CI': "Côte d'Ivoire",
  'ML': 'Mali',
  'BF': 'Burkina Faso',
  'TG': 'Togo',
  'GH': 'Ghana',
  'NG': 'Nigeria',
};

/// Formulaire d'ajout d'un bénéficiaire, requis avant tout transfert
/// (POST /api/transactions/transfer exige un beneficiaryId pré-enregistré).
class AddBeneficiaryScreen extends StatefulWidget {
  const AddBeneficiaryScreen({super.key});

  @override
  State<AddBeneficiaryScreen> createState() => _AddBeneficiaryScreenState();
}

class _AddBeneficiaryScreenState extends State<AddBeneficiaryScreen> {
  final _aliasController = TextEditingController();
  final _phoneController = TextEditingController();
  String _countryCode = 'SN';
  MobileNetwork _network = MobileNetwork.orangeMoney;
  String? _phoneError;

  static final _phonePattern = RegExp(r'^\+[1-9]\d{6,14}$');

  @override
  void dispose() {
    _aliasController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _phoneError = null);

    if (_aliasController.text.trim().isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text("L'alias est obligatoire")));
      return;
    }
    if (!_phonePattern.hasMatch(_phoneController.text.trim())) {
      setState(
        () => _phoneError = 'Format international requis, ex: +221770000000',
      );
      return;
    }

    final provider = context.read<BeneficiaryProvider>();
    final beneficiary = await provider.addBeneficiary(
      alias: _aliasController.text.trim(),
      phoneNumber: _phoneController.text.trim(),
      countryCode: _countryCode,
      network: _network,
    );

    if (!mounted) return;
    if (beneficiary != null) {
      Navigator.of(context).pop(beneficiary);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            provider.errorMessage ?? "Impossible d'ajouter le bénéficiaire",
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isSubmitting = context.watch<BeneficiaryProvider>().isSubmitting;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Nouveau bénéficiaire', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.marginMobile),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Alias', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _aliasController,
                decoration: InputDecoration(
                  hintText: 'ex : Maman',
                  filled: true,
                  fillColor: AppColors.surfaceContainerLow,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppRadius.md),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.md),
              Text('Numéro de téléphone', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              TextField(
                controller: _phoneController,
                keyboardType: TextInputType.phone,
                decoration: InputDecoration(
                  hintText: '+221770000000',
                  errorText: _phoneError,
                  filled: true,
                  fillColor: AppColors.surfaceContainerLow,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppRadius.md),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.md),
              Text('Pays', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              _Dropdown<String>(
                value: _countryCode,
                items: _kCountries.entries
                    .map(
                      (e) =>
                          DropdownMenuItem(value: e.key, child: Text(e.value)),
                    )
                    .toList(),
                onChanged: (v) => setState(() => _countryCode = v!),
              ),
              const SizedBox(height: AppSpacing.md),
              Text('Réseau Mobile Money', style: AppTypography.bodySm),
              const SizedBox(height: AppSpacing.xxs),
              _Dropdown<MobileNetwork>(
                value: _network,
                items: MobileNetwork.values
                    .map(
                      (n) => DropdownMenuItem(value: n, child: Text(n.label)),
                    )
                    .toList(),
                onChanged: (v) => setState(() => _network = v!),
              ),
              const SizedBox(height: AppSpacing.xl),
              PrimaryButton(
                label: 'Ajouter',
                isLoading: isSubmitting,
                onPressed: _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Dropdown<T> extends StatelessWidget {
  final T value;
  final List<DropdownMenuItem<T>> items;
  final ValueChanged<T?> onChanged;

  const _Dropdown({
    required this.value,
    required this.items,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppRadius.md),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<T>(
          value: value,
          isExpanded: true,
          items: items,
          onChanged: onChanged,
        ),
      ),
    );
  }
}
