import 'package:flutter/material.dart';
import 'app_text_styles.dart';

TextTheme buildTextTheme(Color primaryColor, Color secondaryColor) {
  return TextTheme(
    displayLarge: AppTextStyles.displayLarge.copyWith(color: primaryColor),
    displayMedium: AppTextStyles.displayMedium.copyWith(color: primaryColor),
    headlineLarge: AppTextStyles.headlineLarge.copyWith(color: primaryColor),
    headlineMedium: AppTextStyles.headlineMedium.copyWith(color: primaryColor),
    titleLarge: AppTextStyles.titleLarge.copyWith(color: primaryColor),
    titleMedium: AppTextStyles.titleMedium.copyWith(color: primaryColor),
    bodyLarge: AppTextStyles.bodyLarge.copyWith(color: primaryColor),
    bodyMedium: AppTextStyles.bodyMedium.copyWith(color: primaryColor),
    bodySmall: AppTextStyles.bodySmall.copyWith(color: secondaryColor),
    labelLarge: AppTextStyles.labelLarge.copyWith(color: primaryColor),
    labelSmall: AppTextStyles.bodySmall.copyWith(color: secondaryColor),
  );
}

InputDecorationTheme buildInputDecorationTheme(ColorScheme colorScheme) {
  return InputDecorationTheme(
    filled: true,
    fillColor: colorScheme.surfaceContainerLowest,
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: colorScheme.outline),
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: colorScheme.outlineVariant),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: colorScheme.primary, width: 2),
    ),
    errorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: colorScheme.error),
    ),
    focusedErrorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: colorScheme.error, width: 2),
    ),
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
    labelStyle: TextStyle(color: colorScheme.onSurfaceVariant, fontSize: 14),
    hintStyle: TextStyle(color: colorScheme.onSurfaceVariant, fontSize: 14),
    prefixIconColor: colorScheme.primary,
  );
}

ElevatedButtonThemeData buildElevatedButtonTheme(ColorScheme colorScheme) {
  return ElevatedButtonThemeData(
    style: ElevatedButton.styleFrom(
      backgroundColor: colorScheme.primary,
      foregroundColor: colorScheme.onPrimary,
      disabledBackgroundColor: colorScheme.outlineVariant,
      disabledForegroundColor: colorScheme.onSurfaceVariant,
      minimumSize: const Size(double.infinity, 54),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      textStyle: AppTextStyles.labelLarge.copyWith(
        fontSize: 16,
        fontWeight: FontWeight.w600,
      ),
      elevation: 0,
    ),
  );
}

/// alt-b kart yüzeyi. Koyu: düz #1C1C1E (onaylı, değişmez). Açık: beyaz kart
/// + ince çizgi + çok hafif gölge — gri zeminden net ayrışsın.
BoxDecoration altBCard(ColorScheme cs, {double radius = 14}) {
  final light = cs.brightness == Brightness.light;
  return BoxDecoration(
    color: cs.surface,
    borderRadius: BorderRadius.circular(radius),
    border: light ? Border.all(color: const Color(0xFFE3E3E8)) : null,
    boxShadow: light
        ? const [
            BoxShadow(
              color: Color(0x0D000000),
              blurRadius: 12,
              offset: Offset(0, 4),
            ),
          ]
        : null,
  );
}
