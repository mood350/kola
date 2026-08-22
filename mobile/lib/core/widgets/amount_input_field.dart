import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../theme/app_colors.dart';
import '../theme/app_typography.dart';
import '../theme/app_spacing.dart';

/// Champ de saisie de montant XOF (entiers uniquement — pas de centimes
/// pour cette devise), avec séparateur de milliers affiché en temps réel.
/// Utiliser [AmountInputField.parse] pour récupérer la valeur numérique saisie.
class AmountInputField extends StatelessWidget {
  final TextEditingController controller;
  final String? label;
  final String? errorText;
  final String suffixText;

  const AmountInputField({
    super.key,
    required this.controller,
    this.label,
    this.errorText,
    this.suffixText = 'XOF',
  });

  /// Extrait la valeur numérique d'un texte formaté (ex: "12 000" → 12000).
  static double parse(String formatted) {
    final digits = formatted.replaceAll(RegExp(r'[^\d]'), '');
    return double.tryParse(digits) ?? 0;
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (label != null) ...[
          Text(label!, style: AppTypography.bodySm),
          const SizedBox(height: AppSpacing.xxs),
        ],
        TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          style: AppTypography.headingSm,
          inputFormatters: [
            FilteringTextInputFormatter.digitsOnly,
            _ThousandsSeparatorFormatter(),
          ],
          decoration: InputDecoration(
            suffixText: suffixText,
            errorText: errorText,
            filled: true,
            fillColor: AppColors.surfaceContainerLow,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppRadius.md),
              borderSide: BorderSide.none,
            ),
          ),
        ),
      ],
    );
  }
}

class _ThousandsSeparatorFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final digits = newValue.text.replaceAll(RegExp(r'[^\d]'), '');
    if (digits.isEmpty) return newValue.copyWith(text: '');

    final buffer = StringBuffer();
    for (int i = 0; i < digits.length; i++) {
      final posFromEnd = digits.length - i;
      buffer.write(digits[i]);
      if (posFromEnd > 1 && posFromEnd % 3 == 1) buffer.write(' ');
    }

    final formatted = buffer.toString();
    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}
