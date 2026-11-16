import 'package:flutter/material.dart';
import 'app_colors.dart';
import 'theme_helpers.dart';

ThemeData buildDarkTheme() {
  const colorScheme = ColorScheme(
    brightness: Brightness.dark,
    primary: Color(0xFF6EE7B7),
    onPrimary: Color(0xFF022C22),
    primaryContainer: Color(0xFF064E3B),
    onPrimaryContainer: Color(0xFFE5E5EA),
    secondary: Color(0xFF34D399),
    onSecondary: Color(0xFF064E3B),
    secondaryContainer: Color(0xFF065F46),
    onSecondaryContainer: Color(0xFFECFDF5),
    error: Color(0xFFF87171),
    onError: Color(0xFF7F1D1D),
    errorContainer: Color(0xFF991B1B),
    onErrorContainer: Color(0xFFFECACA),
    surface: AppColors.surfaceDark,
    onSurface: AppColors.textPrimaryDark,
    onSurfaceVariant: AppColors.textSecondaryDark,
    outline: Color(0xFF3A3A3C),
    outlineVariant: Color(0xFF2C2C2E),
    inverseSurface: Color(0xFFF5F5F7),
    onInverseSurface: Color(0xFF111827),
    inversePrimary: AppColors.primary,
    surfaceContainerHighest: Color(0xFF2C2C2E),
    surfaceContainerHigh: Color(0xFF1C1C1E),
    surfaceContainer: Color(0xFF161618),
    surfaceContainerLow: Color(0xFF0E0E10),
    surfaceContainerLowest: Color(0xFF000000),
  );

  return ThemeData(
    useMaterial3: true,
    colorScheme: colorScheme,
    scaffoldBackgroundColor: AppColors.backgroundDark,
    textTheme: buildTextTheme(
      AppColors.textPrimaryDark,
      AppColors.textSecondaryDark,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: AppColors.surfaceDark,
      foregroundColor: AppColors.textPrimaryDark,
      elevation: 0,
      scrolledUnderElevation: 0.5,
      shadowColor: Color(0x40059669),
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
      indicatorColor: colorScheme.primary.withValues(alpha: 0.18),
      elevation: 0,
      shadowColor: Colors.transparent,
      labelTextStyle: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.selected)) {
          return TextStyle(
            color: colorScheme.primary,
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
          return IconThemeData(color: colorScheme.primary, size: 22);
        }
        return const IconThemeData(color: AppColors.textSecondaryDark, size: 22);
      }),
    ),
    tabBarTheme: TabBarThemeData(
      indicatorColor: colorScheme.primary,
      indicatorSize: TabBarIndicatorSize.tab,
      labelColor: colorScheme.primary,
      unselectedLabelColor: AppColors.textSecondaryDark,
      labelStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
      unselectedLabelStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w400),
      dividerColor: const Color(0xFF2C2C2E),
    ),
    inputDecorationTheme: buildInputDecorationTheme(colorScheme),
    elevatedButtonTheme: buildElevatedButtonTheme(colorScheme),
    cardTheme: const CardThemeData(
      color: AppColors.surfaceDark,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(14)),
      ),
    ),
    listTileTheme: const ListTileThemeData(
      contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
    ),
    expansionTileTheme: ExpansionTileThemeData(
      backgroundColor: Colors.transparent,
      collapsedBackgroundColor: Colors.transparent,
      iconColor: colorScheme.primary,
      collapsedIconColor: AppColors.textSecondaryDark,
      tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      childrenPadding: EdgeInsets.zero,
      shape: const Border(),
      collapsedShape: const Border(),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: colorScheme.primary.withValues(alpha: 0.08),
      labelStyle: TextStyle(color: colorScheme.primary, fontSize: 12),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 0),
    ),
    dividerTheme: const DividerThemeData(
      color: Color(0xFF2C2C2E),
      thickness: 1,
      space: 1,
    ),
    floatingActionButtonTheme: FloatingActionButtonThemeData(
      backgroundColor: colorScheme.primary,
      foregroundColor: colorScheme.onPrimary,
      elevation: 2,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(16)),
      ),
    ),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: colorScheme.surfaceContainerHighest,
      contentTextStyle: TextStyle(color: colorScheme.onSurface, fontSize: 14),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      behavior: SnackBarBehavior.floating,
    ),
  );
}
