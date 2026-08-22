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
import '../transfer/add_beneficiary_screen.dart';

/// Gestion des bénéficiaires depuis le profil (ajout / suppression).
/// Distinct du sélecteur utilisé pendant un transfert, qui sert à choisir
/// un destinataire et non à administrer la liste.
class ManageBeneficiariesScreen extends StatefulWidget {
  const ManageBeneficiariesScreen({super.key});

  @override
  State<ManageBeneficiariesScreen> createState() =>
      _ManageBeneficiariesScreenState();
}

class _ManageBeneficiariesScreenState extends State<ManageBeneficiariesScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<BeneficiaryProvider>().loadBeneficiaries();
    });
  }

  Future<void> _confirmDelete(Beneficiary b) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text('Supprimer ${b.alias} ?'),
        content: const Text(
          'Ce bénéficiaire ne sera plus proposé lors de vos transferts. '
          'Vos transferts déjà effectués restent dans votre historique.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Annuler'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Supprimer'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    final provider = context.read<BeneficiaryProvider>();
    final success = await provider.removeBeneficiary(b.id);
    if (!mounted) return;
    if (!success) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(provider.errorMessage ?? 'Échec de la suppression'),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<BeneficiaryProvider>();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Mes bénéficiaires', style: AppTypography.headingSm),
        actions: [
          IconButton(
            icon: const Icon(
              Icons.person_add_alt_rounded,
              color: AppColors.primary,
            ),
            onPressed: () async {
              await Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const AddBeneficiaryScreen()),
              );
              if (context.mounted) {
                context.read<BeneficiaryProvider>().loadBeneficiaries();
              }
            },
          ),
        ],
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () => provider.loadBeneficiaries(),
          child: Builder(
            builder: (context) {
              if (provider.isLoading && provider.beneficiaries.isEmpty) {
                return const Padding(
                  padding: EdgeInsets.all(AppSpacing.marginMobile),
                  child: SkeletonList(),
                );
              }
              if (provider.errorMessage != null &&
                  provider.beneficiaries.isEmpty) {
                return ListView(
                  children: [
                    EmptyState(
                      icon: Icons.error_outline_rounded,
                      title: 'Impossible de charger les bénéficiaires',
                      subtitle: provider.errorMessage,
                    ),
                  ],
                );
              }
              if (provider.beneficiaries.isEmpty) {
                return ListView(
                  children: const [
                    EmptyState(
                      icon: Icons.contacts_outlined,
                      title: 'Aucun bénéficiaire enregistré',
                      subtitle:
                          'Ajoutez un bénéficiaire pour lui envoyer de l\'argent plus rapidement.',
                    ),
                  ],
                );
              }

              return ListView.separated(
                padding: const EdgeInsets.all(AppSpacing.marginMobile),
                itemCount: provider.beneficiaries.length,
                separatorBuilder: (_, _) =>
                    const SizedBox(height: AppSpacing.sm),
                itemBuilder: (context, index) {
                  final b = provider.beneficiaries[index];
                  return FadeSlideIn(
                    delay: Duration(milliseconds: index * 40),
                    child: KolaCard(
                      padding: const EdgeInsets.all(AppSpacing.md),
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
                                Text(b.alias, style: AppTypography.bodyMdBold),
                                Text(
                                  '${b.phoneNumber} · ${b.network.label}',
                                  style: AppTypography.bodySm,
                                ),
                              ],
                            ),
                          ),
                          IconButton(
                            icon: const Icon(
                              Icons.delete_outline_rounded,
                              color: AppColors.danger,
                            ),
                            onPressed: provider.isSubmitting
                                ? null
                                : () => _confirmDelete(b),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              );
            },
          ),
        ),
      ),
    );
  }
}
