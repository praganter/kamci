import 'package:flutter/material.dart';

abstract final class AppColors {
  static const Color primary = Color(0xFF059669);
  static const Color primaryDark = Color(0xFF047857);
  static const Color secondary = Color(0xFF34D399);

  static const Color backgroundLight = Color(0xFFF2F2F7);
  static const Color backgroundDark = Color(0xFF000000);

  static const Color surfaceLight = Color(0xFFFFFFFF);
  static const Color surfaceDark = Color(0xFF1C1C1E);

  static const Color error = Color(0xFFEF4444);
  static const Color success = Color(0xFF22C55E);
  static const Color warning = Color(0xFFF59E0B);

  static const Color textPrimaryLight = Color(0xFF111827);
  static const Color textSecondaryLight = Color(0xFF6B7280);
  static const Color textPrimaryDark = Color(0xFFF5F5F7);
  static const Color textSecondaryDark = Color(0xFF8E8E93);

  static const Color accessRead = Color(0xFF8E8E93);
  static const Color accessWrite = Color(0xFF22C55E);
  static const Color accessEdit = Color(0xFFF59E0B);
  static const Color accessDelete = Color(0xFFEF4444);

  static const Color statusActive = Color(0xFF22C55E);
  static const Color statusInactive = Color(0xFF8E8E93);
}
