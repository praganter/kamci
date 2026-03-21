abstract final class Validators {
  static String? email(String? value) {
    if (value == null || value.trim().isEmpty) return 'Email gerekli';
    final emailRegex = RegExp(r'^[\w-.]+@([\w-]+\.)+[\w-]{2,4}$');
    if (!emailRegex.hasMatch(value.trim())) return 'Geçerli bir email girin';
    return null;
  }

  static String? password(String? value) {
    if (value == null || value.isEmpty) return 'Şifre gerekli';
    if (value.length < 8) return 'Şifre en az 8 karakter olmalı';
    return null;
  }

  static String? confirmPassword(String? value, String? original) {
    if (value == null || value.isEmpty) return 'Şifre tekrarı gerekli';
    if (value != original) return 'Şifreler eşleşmiyor';
    return null;
  }

  static String? required(String? value, {String fieldName = 'Alan'}) {
    if (value == null || value.trim().isEmpty) return '$fieldName gerekli';
    return null;
  }

  static String? amount(String? value) {
    if (value == null || value.trim().isEmpty) return 'Tutar gerekli';
    final parsed = double.tryParse(value.replaceAll(',', '.'));
    if (parsed == null) return 'Geçerli bir tutar girin';
    if (parsed <= 0) return 'Tutar 0\'dan büyük olmalı';
    return null;
  }
}
