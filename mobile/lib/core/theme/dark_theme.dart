import 'package:flutter/material.dart';
import 'app_colors.dart';
import 'app_text_styles.dart';

ThemeData buildDarkTheme() {
  const colorScheme = ColorScheme(
    brightness: Brightness.dark,
    primary: Color(0xFFA78BFA),
    onPrimary: Color(0xFF2E1065),
    primaryContainer: Color(0xFF4C1D95),
    onPrimaryContainer: Color(0xFFEDE9FE),
    secondary: Color(0xFFC084FC),
    onSecondary: Color(0xFF3B0764),
    secondaryContainer: Color(0xFF581C87),
    onSecondaryContainer: Color(0xFFF3E8FF),
    error: Color(0xFFF87171),
    onError: Color(0xFF7F1D1D),
    errorContainer: Color(0xFF991B1B),
    onErrorContainer: Color(0xFFFECACA),
    surface: AppColors.surfaceDark,
    onSurface: AppColors.textPrimaryDark,
    onSurfaceVariant: AppColors.textSecondaryDark,
    outline: Color(0xFF5B4B8A),
    outlineVariant: Color(0xFF2E1F55),
    inverseSurface: Color(0xFFF0EEFF),
    onInverseSurface: Color(0xFF1A1025),
    inversePrimary: AppColors.primary,
    surfaceContainerHighest: Color(0xFF2E2050),
    surfaceContainerHigh: Color(0xFF231840),
    surfaceContainer: Color(0xFF1A1235),
    surfaceContainerLow: Color(0xFF130E28),
    surfaceContainerLowest: Color(0xFF0D0A1A),
  );

  return ThemeData(
    useMaterial3: true,
    colorScheme: colorScheme,
    scaffoldBackgroundColor: AppColors.backgroundDark,
    textTheme: _buildTextTheme(
      AppColors.textPrimaryDark,
      AppColors.textSecondaryDark,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: AppColors.surfaceDark,
      foregroundColor: AppColors.textPrimaryDark,
      elevation: 0,
      scrolledUnderElevation: 0.5,
      shadowColor: Color(0x407C3AED),
      centerTitle: false,
      titleTextStyle: TextStyle(
        color: AppColors.textPrimaryDark,
        fontSize: 18,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.3,
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: AppColors.surfaceDark,
      indicatorColor: const Color(0xFFA78BFA).withValues(alpha: 0.18),
      elevation: 0,
      shadowColor: Colors.transparent,
      labelTextStyle: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.selected)) {
          return const TextStyle(
            color: Color(0xFFA78BFA),
            fontSize: 11,
            fontWeight: FontWeight.w600,
          );
        }
        return const TextStyle(
          color: AppColors.textSecondaryDark,
          fontSize: 11,
          fontWeight: FontWeight.w400,
        );
      }),
      iconTheme: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.selected)) {
          return const IconThemeData(color: Color(0xFFA78BFA), size: 22);
        }
        return const IconThemeData(
          color: AppColors.textSecondaryDark,
          size: 22,
        );
      }),
    ),
    tabBarTheme: const TabBarThemeData(
      indicatorColor: Color(0xFFA78BFA),
      indicatorSize: TabBarIndicatorSize.tab,
      labelColor: Color(0xFFA78BFA),
      unselectedLabelColor: AppColors.textSecondaryDark,
      labelStyle: TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
      unselectedLabelStyle: TextStyle(
        fontSize: 13,
        fontWeight: FontWeight.w400,
      ),
      dividerColor: Color(0xFF2E1F55),
    ),
    inputDecorationTheme: _buildInputDecorationTheme(colorScheme),
    elevatedButtonTheme: _buildElevatedButtonTheme(colorScheme),
    cardTheme: const CardThemeData(
      color: AppColors.surfaceDark,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(16)),
        side: BorderSide(color: Color(0xFF2E1F55), width: 1),
      ),
    ),
    listTileTheme: const ListTileThemeData(
      contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
    ),
    expansionTileTheme: ExpansionTileThemeData(
      backgroundColor: Colors.transparent,
      collapsedBackgroundColor: Colors.transparent,
      iconColor: Color(0xFFA78BFA),
      collapsedIconColor: AppColors.textSecondaryDark,
      tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      childrenPadding: EdgeInsets.zero,
      shape: const Border(),
      collapsedShape: const Border(),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: const Color(0xFFA78BFA).withValues(alpha: 0.12),
      labelStyle: const TextStyle(color: Color(0xFFA78BFA), fontSize: 12),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 0),
    ),
    dividerTheme: const DividerThemeData(
      color: Color(0xFF2E1F55),
      thickness: 1,
      space: 1,
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: Color(0xFFA78BFA),
      foregroundColor: Color(0xFF2E1065),
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(16)),
      ),
    ),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: const Color(0xFF2E2050),
      contentTextStyle: const TextStyle(color: Color(0xFFF0EEFF), fontSize: 14),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      behavior: SnackBarBehavior.floating,
    ),
  );
}

TextTheme _buildTextTheme(Color primaryColor, Color secondaryColor) {
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

InputDecorationTheme _buildInputDecorationTheme(ColorScheme colorScheme) {
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

ElevatedButtonThemeData _buildElevatedButtonTheme(ColorScheme colorScheme) {
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
