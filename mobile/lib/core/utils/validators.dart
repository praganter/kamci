import 'package:flutter/widgets.dart';
import '../l10n/l10n.dart';

abstract final class Validators {
  static FormFieldValidator<String> email(AppLocalizations l10n) => (value) {
    if (value == null || value.trim().isEmpty) return l10n.validationEmailRequired;
    final emailRegex = RegExp(r'^[\w-.]+@([\w-]+\.)+[a-zA-Z]{2,}$');
    if (!emailRegex.hasMatch(value.trim())) return l10n.validationEmailInvalid;
    return null;
  };

  static FormFieldValidator<String> password(AppLocalizations l10n) => (value) {
    if (value == null || value.isEmpty) return l10n.validationPasswordRequired;
    if (value.length < 8) return l10n.validationPasswordMinLength;
    if (value.length > 72) return l10n.validationPasswordMaxLength;
    return null;
  };

  static String? confirmPassword(
    AppLocalizations l10n,
    String? value,
    String? original,
  ) {
    if (value == null || value.isEmpty) return l10n.validationConfirmPasswordRequired;
    if (value != original) return l10n.validationPasswordMismatch;
    return null;
  }

  static String? required(
    AppLocalizations l10n,
    String? value, {
    required String fieldName,
  }) {
    if (value == null || value.trim().isEmpty) {
      return l10n.validationFieldRequired(fieldName);
    }
    return null;
  }

  static FormFieldValidator<String> amount(AppLocalizations l10n) => (value) {
    if (value == null || value.trim().isEmpty) return l10n.validationAmountRequired;
    final parsed = double.tryParse(value.replaceAll(',', '.'));
    if (parsed == null) return l10n.validationAmountInvalid;
    if (parsed <= 0) return l10n.validationAmountPositive;
    return null;
  };

  static bool isValidEmail(String? value) {
    if (value == null || value.trim().isEmpty) return false;
    return RegExp(r'^[\w-.]+@([\w-]+\.)+[a-zA-Z]{2,}$').hasMatch(value.trim());
  }
}
