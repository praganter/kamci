import 'package:flutter/material.dart';
import 'app_colors.dart';
import 'app_text_styles.dart';

ThemeData buildLightTheme() {
  const colorScheme = ColorScheme(
    brightness: Brightness.light,
    primary: AppColors.primary,
    onPrimary: Colors.white,
    primaryContainer: Color(0xFFEDE9FE),
    onPrimaryContainer: Color(0xFF3B0764),
    secondary: AppColors.secondary,
    onSecondary: Colors.white,
    secondaryContainer: Color(0xFFF3E8FF),
    onSecondaryContainer: Color(0xFF4A044E),
    error: AppColors.error,
    onError: Colors.white,
    errorContainer: Color(0xFFFFE4E4),
    onErrorContainer: Color(0xFF7F0000),
    surface: AppColors.surfaceLight,
    onSurface: AppColors.textPrimaryLight,
    onSurfaceVariant: AppColors.textSecondaryLight,
    outline: Color(0xFFD8B4FE),
    outlineVariant: Color(0xFFEDE9FE),
    inverseSurface: Color(0xFF1A1025),
    onInverseSurface: Color(0xFFF0EEFF),
    inversePrimary: Color(0xFFA78BFA),
    surfaceContainerHighest: Color(0xFFEDE9FE),
    surfaceContainerHigh: Color(0xFFF2EDFF),
    surfaceContainer: Color(0xFFF6F3FF),
    surfaceContainerLow: Color(0xFFFAF8FF),
    surfaceContainerLowest: Color(0xFFFFFFFF),
  );

  return ThemeData(
    useMaterial3: true,
    colorScheme: colorScheme,
    scaffoldBackgroundColor: AppColors.backgroundLight,
    textTheme: _buildTextTheme(AppColors.textPrimaryLight, AppColors.textSecondaryLight),
    appBarTheme: const AppBarTheme(
      backgroundColor: AppColors.surfaceLight,
      foregroundColor: AppColors.textPrimaryLight,
      elevation: 0,
      scrolledUnderElevation: 0.5,
      shadowColor: Color(0x1A7C3AED),
      centerTitle: false,
      titleTextStyle: TextStyle(
        color: AppColors.textPrimaryLight,
        fontSize: 18,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.3,
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: AppColors.surfaceLight,
      indicatorColor: AppColors.primary.withValues(alpha: 0.12),
      elevation: 0,
      shadowColor: Colors.transparent,
      labelTextStyle: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.selected)) {
          return const TextStyle(
            color: AppColors.primary,
            fontSize: 11,
            fontWeight: FontWeight.w600,
          );
        }
        return const TextStyle(
          color: AppColors.textSecondaryLight,
          fontSize: 11,
          fontWeight: FontWeight.w400,
        );
      }),
      iconTheme: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.selected)) {
          return const IconThemeData(color: AppColors.primary, size: 22);
        }
        return const IconThemeData(color: AppColors.textSecondaryLight, size: 22);
      }),
    ),
    tabBarTheme: TabBarThemeData(
      indicatorColor: AppColors.primary,
      indicatorSize: TabBarIndicatorSize.tab,
      labelColor: AppColors.primary,
      unselectedLabelColor: AppColors.textSecondaryLight,
      labelStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
      unselectedLabelStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w400),
      dividerColor: Color(0xFFEDE9FE),
    ),
    inputDecorationTheme: _buildInputDecorationTheme(colorScheme),
    elevatedButtonTheme: _buildElevatedButtonTheme(colorScheme),
    cardTheme: const CardThemeData(
      color: AppColors.surfaceLight,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(16)),
        side: BorderSide(color: Color(0xFFEDE9FE), width: 1),
      ),
    ),
    listTileTheme: const ListTileThemeData(
      contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
    ),
    expansionTileTheme: ExpansionTileThemeData(
      backgroundColor: Colors.transparent,
      collapsedBackgroundColor: Colors.transparent,
      iconColor: AppColors.primary,
      collapsedIconColor: AppColors.textSecondaryLight,
      tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      childrenPadding: EdgeInsets.zero,
      shape: const Border(),
      collapsedShape: const Border(),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: AppColors.primary.withValues(alpha: 0.08),
      labelStyle: const TextStyle(color: AppColors.primary, fontSize: 12),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 0),
    ),
    dividerTheme: const DividerThemeData(
      color: Color(0xFFEDE9FE),
      thickness: 1,
      space: 1,
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: AppColors.primary,
      foregroundColor: Colors.white,
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(16))),
    ),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: const Color(0xFF1A1025),
      contentTextStyle: const TextStyle(color: Colors.white, fontSize: 14),
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
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
      ),
      textStyle: AppTextStyles.labelLarge.copyWith(fontSize: 16, fontWeight: FontWeight.w600),
      elevation: 0,
    ),
  );
}
