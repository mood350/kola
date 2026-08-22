import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/fade_slide_in.dart';
import '../../core/widgets/kola_card.dart';
import '../../core/widgets/skeleton_list_item.dart';
import '../../models/beneficiary.dart';
import '../../providers/beneficiary_provider.dart';
import 'add_beneficiary_screen.dart';
import 'transfer_amount_screen.dart';

/// Première étape du transfert : choix (ou ajout) d'un bénéficiaire.
class BeneficiaryPickerScreen extends StatefulWidget {
  final int walletId;

  const BeneficiaryPickerScreen({super.key, required this.walletId});

  @override
  State<BeneficiaryPickerScreen> createState() =>
      _BeneficiaryPickerScreenState();
}

class _BeneficiaryPickerScreenState extends State<BeneficiaryPickerScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<BeneficiaryProvider>().loadBeneficiaries();
    });
  }

  Future<void> _addBeneficiary() async {
    final result = await Navigator.push<Beneficiary>(
      context,
      MaterialPageRoute(builder: (_) => const AddBeneficiaryScreen()),
    );
    if (result != null && mounted) {
      _openTransfer(result);
    }
  }

  void _openTransfer(Beneficiary beneficiary) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => TransferAmountScreen(
          walletId: widget.walletId,
          beneficiary: beneficiary,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<BeneficiaryProvider>();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Envoyer à', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: Builder(
          builder: (context) {
            if (provider.isLoading && provider.beneficiaries.isEmpty) {
              return const Padding(
                padding: EdgeInsets.all(AppSpacing.marginMobile),
                child: SkeletonList(),
              );
            }
            return ListView(
              padding: const EdgeInsets.all(AppSpacing.marginMobile),
              children: [
                KolaCard(
                  padding: const EdgeInsets.all(AppSpacing.md),
                  onTap: _addBeneficiary,
                  child: Row(
                    children: [
                      const Icon(
                        Icons.person_add_alt_rounded,
                        color: AppColors.primary,
                      ),
                      const SizedBox(width: AppSpacing.sm),
                      Text(
                        'Ajouter un bénéficiaire',
                        style: AppTypography.bodyMdBold,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: AppSpacing.md),
                if (provider.errorMessage != null &&
                    provider.beneficiaries.isEmpty)
                  EmptyState(
                    icon: Icons.error_outline_rounded,
                    title: 'Impossible de charger les bénéficiaires',
                    subtitle: provider.errorMessage,
                  )
                else if (provider.beneficiaries.isEmpty)
                  const EmptyState(
                    icon: Icons.contacts_outlined,
                    title: 'Aucun bénéficiaire enregistré',
                  )
                else
                  ...provider.beneficiaries.asMap().entries.map((entry) {
                    final index = entry.key;
                    final b = entry.value;
                    return FadeSlideIn(
                      delay: Duration(milliseconds: index * 40),
                      child: Padding(
                        padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                        child: KolaCard(
                          padding: const EdgeInsets.all(AppSpacing.md),
                          onTap: () => _openTransfer(b),
                          child: Row(
                            children: [
                              const CircleAvatar(
                                backgroundColor: AppColors.surfaceVariant,
                                child: Icon(
                                  Icons.person_rounded,
                                  color: AppColors.onSurfaceVariant,
                                ),
                              ),
                              const SizedBox(width: AppSpacing.sm),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      b.alias,
                                      style: AppTypography.bodyMdBold,
                                    ),
                                    Text(
                                      '${b.phoneNumber} · ${b.network.label}',
                                      style: AppTypography.bodySm,
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    );
                  }),
              ],
            );
          },
        ),
      ),
    );
  }
}
