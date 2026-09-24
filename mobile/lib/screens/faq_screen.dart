import 'package:flutter/material.dart';
import '../core/theme/kola_icons.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../routes/kola_route.dart';

class _Faq {
  const _Faq(this.id, this.category, this.question, this.answer);
  final String id;
  final String category;
  final String question;
  final String answer;
}

const _faqs = <_Faq>[
  _Faq(
    'account',
    'Compte',
    'Comment créer et sécuriser mon compte KOLA ?',
    'Inscrivez-vous avec un numéro togolais, confirmez le code OTP puis choisissez un code PIN à 4 chiffres. Ne communiquez jamais votre OTP ou votre PIN.',
  ),
  _Faq(
    'pin',
    'Compte',
    'Que faire si j’oublie mon code PIN ?',
    'Depuis la page de connexion, utilisez la récupération du compte. Un code de vérification sera envoyé sur votre numéro enregistré.',
  ),
  _Faq(
    'recharge',
    'Portefeuille',
    'Comment recharger mon portefeuille ?',
    'Sur l’accueil, appuyez sur « Recharger », choisissez Mixx by Yas, Moov Money ou carte bancaire, puis confirmez le montant et le compte togolais.',
  ),
  _Faq(
    'p2p',
    'Transferts',
    'Comment envoyer de l’argent à une personne ?',
    'Appuyez sur « Envoyer P2P », scannez le QR KOLA du bénéficiaire ou saisissez son numéro au format togolais, puis vérifiez les informations avant de confirmer.',
  ),
  _Faq(
    'pending',
    'Transferts',
    'Pourquoi mon transfert est-il en attente ?',
    'Une opération peut rester en attente pendant la confirmation de l’opérateur. Consultez l’historique avant de recommencer afin d’éviter un double envoi.',
  ),
  _Faq(
    'vault',
    'Coffres',
    'À quoi sert un coffre KOLA ?',
    'Un coffre vous permet de mettre de l’argent de côté pour un objectif. Vous pouvez définir un montant cible et une date, puis suivre votre progression.',
  ),
  _Faq(
    'schedule',
    'Planification',
    'Comment programmer un envoi ?',
    'Ouvrez « Planifié », choisissez une personne ou un coffre, saisissez le montant, la date et l’heure du Togo, puis confirmez la programmation.',
  ),
  _Faq(
    'credit',
    'Crédit',
    'Comment mon éligibilité au crédit est-elle calculée ?',
    'Elle dépend notamment de votre vérification KYC, de votre activité, de votre discipline Bankivi et du remboursement de vos précédents crédits.',
  ),
  _Faq(
    'security',
    'Sécurité',
    'KOLA me demandera-t-il mon OTP ou mon PIN ?',
    'Non. Aucun agent KOLA ne doit vous demander votre code OTP ou votre PIN. Refusez et signalez immédiatement toute demande de ce type.',
  ),
];

class FaqScreen extends StatefulWidget {
  const FaqScreen({super.key, required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  State<FaqScreen> createState() => _FaqScreenState();
}

class _FaqScreenState extends State<FaqScreen> {
  final _search = TextEditingController();
  String _query = '';
  String? _openId;

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final query = _query.trim().toLowerCase();
    final results = query.isEmpty
        ? _faqs
        : _faqs
              .where(
                (item) => '${item.category} ${item.question} ${item.answer}'
                    .toLowerCase()
                    .contains(query),
              )
              .toList();

    final categories = <String, List<_Faq>>{};
    for (final item in results) {
      categories.putIfAbsent(item.category, () => []).add(item);
    }

    return KolaScreen(
      route: KolaRoute.faq,
      onNavigate: widget.onNavigate,
      children: [
        BackLink(
          label: 'Mon profil',
          onTap: () => widget.onNavigate(KolaRoute.profile),
        ),
        const PageHeader(
          overline: 'CENTRE D’AIDE',
          title: 'Comment pouvons-nous vous aider ?',
          subtitle: 'Retrouvez rapidement les réponses sur KOLA.',
          icon: KolaIcons.helpCircleOutline,
        ),
        const SizedBox(height: 21),
        Container(
          height: 49,
          padding: const EdgeInsets.symmetric(horizontal: 13),
          decoration: BoxDecoration(
            color: AppColors.white,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: AppColors.border),
          ),
          child: Row(
            children: [
              const Icon(
                KolaIcons.searchOutline,
                size: 19,
                color: AppColors.muted,
              ),
              const SizedBox(width: 9),
              Expanded(
                child: TextField(
                  controller: _search,
                  onChanged: (value) => setState(() => _query = value),
                  style: AppTypography.body.copyWith(fontSize: 12),
                  decoration: const InputDecoration(
                    hintText: 'Rechercher une question',
                    border: InputBorder.none,
                    enabledBorder: InputBorder.none,
                    focusedBorder: InputBorder.none,
                    filled: false,
                    isDense: true,
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
              ),
              if (_query.isNotEmpty)
                GestureDetector(
                  onTap: () {
                    _search.clear();
                    setState(() => _query = '');
                  },
                  child: const Icon(
                    KolaIcons.closeCircle,
                    size: 18,
                    color: AppColors.muted,
                  ),
                ),
            ],
          ),
        ),
        for (final entry in categories.entries) ...[
          Padding(
            padding: const EdgeInsets.only(top: 20, bottom: 8),
            child: Text(
              entry.key.toUpperCase(),
              style: AppTypography.fieldLabel.copyWith(fontSize: 8),
            ),
          ),
          for (final item in entry.value)
            Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.xs),
              child: _FaqTile(
                faq: item,
                expanded: _openId == item.id,
                onTap: () => setState(
                  () => _openId = _openId == item.id ? null : item.id,
                ),
              ),
            ),
        ],
        if (results.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 42),
            child: EmptyState(
              icon: KolaIcons.searchOutline,
              title: 'Aucune réponse trouvée',
              message: 'Essayez avec un autre mot-clé.',
            ),
          ),
        Container(
          margin: const EdgeInsets.only(top: 22),
          padding: const EdgeInsets.all(15),
          decoration: BoxDecoration(
            color: AppColors.greenPale,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            children: [
              const Icon(
                KolaIcons.shieldCheckmarkOutline,
                size: 21,
                color: AppColors.greenDark,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Conseil de sécurité',
                      style: AppTypography.smallBold.copyWith(
                        fontWeight: FontWeight.w800,
                        color: AppColors.greenDark,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      'Ne partagez jamais votre code OTP ou votre PIN.',
                      style: AppTypography.caption.copyWith(fontSize: 9),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _FaqTile extends StatelessWidget {
  const _FaqTile({
    required this.faq,
    required this.expanded,
    required this.onTap,
  });

  final _Faq faq;
  final bool expanded;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 160),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: expanded ? AppColors.pale : AppColors.white,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: expanded ? const Color(0xFFB5C1F8) : AppColors.border,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    faq.question,
                    style: AppTypography.badge.copyWith(
                      fontSize: 12,
                      fontWeight: FontWeight.w800,
                      color: AppColors.ink,
                      height: 1.42,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Icon(
                  expanded ? KolaIcons.remove : KolaIcons.add,
                  size: 20,
                  color: AppColors.primary,
                ),
              ],
            ),
            if (expanded) ...[
              Container(
                margin: const EdgeInsets.only(top: 11),
                padding: const EdgeInsets.only(top: 11),
                decoration: const BoxDecoration(
                  border: Border(top: BorderSide(color: AppColors.border)),
                ),
                child: Text(
                  faq.answer,
                  style: AppTypography.caption.copyWith(height: 1.7),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
