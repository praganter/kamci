import 'package:flutter/material.dart';
import 'app_colors.dart';
import 'theme_helpers.dart';

ThemeData buildLightTheme() {
  const colorScheme = ColorScheme(
    brightness: Brightness.light,
    primary: AppColors.primary,
    onPrimary: Colors.white,
    primaryContainer: Color(0xFFC4B5FD),
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
    textTheme: buildTextTheme(
      AppColors.textPrimaryLight,
      AppColors.textSecondaryLight,
    ),
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
        return const IconThemeData(
          color: AppColors.textSecondaryLight,
          size: 22,
        );
      }),
    ),
    tabBarTheme: const TabBarThemeData(
      indicatorColor: AppColors.primary,
      indicatorSize: TabBarIndicatorSize.tab,
      labelColor: AppColors.primary,
      unselectedLabelColor: AppColors.textSecondaryLight,
      labelStyle: TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
      unselectedLabelStyle: TextStyle(fontSize: 13, fontWeight: FontWeight.w400),
      dividerColor: Color(0xFFEDE9FE),
    ),
    inputDecorationTheme: buildInputDecorationTheme(colorScheme),
    elevatedButtonTheme: buildElevatedButtonTheme(colorScheme),
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
    expansionTileTheme: const ExpansionTileThemeData(
      backgroundColor: Colors.transparent,
      collapsedBackgroundColor: Colors.transparent,
      iconColor: AppColors.primary,
      collapsedIconColor: AppColors.textSecondaryLight,
      tilePadding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      childrenPadding: EdgeInsets.zero,
      shape: Border(),
      collapsedShape: Border(),
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
    floatingActionButtonTheme: FloatingActionButtonThemeData(
      backgroundColor: AppColors.primary,
      foregroundColor: colorScheme.onPrimary,
      elevation: 2,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(16)),
      ),
    ),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: colorScheme.inverseSurface,
      contentTextStyle: TextStyle(color: colorScheme.onInverseSurface, fontSize: 14),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      behavior: SnackBarBehavior.floating,
    ),
  );
}
