import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/app_colors.dart';
import '../theme/app_typography.dart';

/// Saisie d'un code chiffre par chiffre : une case par caractère.
///
/// Un seul vrai champ de texte porte la saisie, rendu transparent et étendu
/// sous les cases. Les cases ne font qu'afficher son contenu et laissent
/// passer les touchers. Ce montage évite le piège du champ par case, où
/// chaque effacement doit reculer le focus à la main — et où le collage d'un
/// code reçu par SMS ne remplit que la première case.
class CodeInput extends StatefulWidget {
  const CodeInput({
    super.key,
    required this.controller,
    required this.length,
    this.obscure = false,
    this.autofocus = false,
    this.onCompleted,
  });

  final TextEditingController controller;
  final int length;
  final bool obscure;
  final bool autofocus;

  /// Appelé dès que la dernière case est remplie.
  final VoidCallback? onCompleted;

  @override
  State<CodeInput> createState() => _CodeInputState();
}

class _CodeInputState extends State<CodeInput> {
  final FocusNode _focus = FocusNode();

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onTextChanged);
    _focus.addListener(_onFocusChanged);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onTextChanged);
    _focus.removeListener(_onFocusChanged);
    _focus.dispose();
    super.dispose();
  }

  void _onTextChanged() {
    setState(() {});
    if (widget.controller.text.length == widget.length) {
      widget.onCompleted?.call();
    }
  }

  void _onFocusChanged() => setState(() {});

  @override
  Widget build(BuildContext context) {
    final value = widget.controller.text;

    return SizedBox(
      height: 56,
      child: Stack(
        children: [
          // Sous les cases : c'est lui qui reçoit les touchers, ouvre le
          // clavier et tient le contenu.
          Positioned.fill(
            child: TextField(
              controller: widget.controller,
              focusNode: _focus,
              autofocus: widget.autofocus,
              keyboardType: TextInputType.number,
              maxLength: widget.length,
              enableInteractiveSelection: false,
              showCursor: false,
              cursorColor: Colors.transparent,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              style: const TextStyle(color: Colors.transparent, fontSize: 1),
              decoration: const InputDecoration(
                counterText: '',
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
                filled: false,
                isDense: true,
                contentPadding: EdgeInsets.zero,
              ),
            ),
          ),
          Positioned.fill(
            child: IgnorePointer(
              child: Row(
                children: [
                  for (var i = 0; i < widget.length; i++) ...[
                    if (i > 0) const SizedBox(width: 8),
                    Expanded(
                      child: _Cell(
                        filled: i < value.length,
                        character: i < value.length ? value[i] : '',
                        obscure: widget.obscure,
                        // La case courante est celle qu'on s'apprête à
                        // remplir ; une fois le code complet, plus aucune.
                        active: _focus.hasFocus && i == value.length,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Cell extends StatelessWidget {
  const _Cell({
    required this.filled,
    required this.character,
    required this.obscure,
    required this.active,
  });

  final bool filled;
  final String character;
  final bool obscure;
  final bool active;

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 140),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: active
              ? AppColors.primary
              : filled
              ? AppColors.pale2
              : AppColors.border,
          width: active ? 1.8 : 1,
        ),
      ),
      child: obscure && filled
          ? Container(
              width: 11,
              height: 11,
              decoration: const BoxDecoration(
                color: AppColors.primary,
                shape: BoxShape.circle,
              ),
            )
          : Text(
              character,
              style: AppTypography.amount.copyWith(
                fontSize: 22,
                fontWeight: FontWeight.w900,
              ),
            ),
    );
  }
}
