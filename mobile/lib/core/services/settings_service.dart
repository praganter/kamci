import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../features/debt/domain/entities/debt.dart';

class SettingsService {
  const SettingsService(this._prefs);
  final SharedPreferences _prefs;

  static const _keyTheme = 'theme_mode';
  static const _keyLocale = 'locale';
  static const _keyDebtStatusFilter = 'debt_status_filter';
  static const _keyBaseCurrency = 'base_currency';

  ThemeMode get themeMode {
    return switch (_prefs.getString(_keyTheme)) {
      'light' => ThemeMode.light,
      'dark' => ThemeMode.dark,
      _ => ThemeMode.system,
    };
  }

  Future<void> setThemeMode(ThemeMode mode) =>
      _prefs.setString(_keyTheme, switch (mode) {
        ThemeMode.light => 'light',
        ThemeMode.dark => 'dark',
        _ => 'system',
      });

  Locale? get locale {
    final v = _prefs.getString(_keyLocale);
    return v != null ? Locale(v) : null;
  }

  Future<void> setLocale(Locale? locale) {
    if (locale == null) {
      return _prefs.remove(_keyLocale).then((_) {});
    }
    return _prefs.setString(_keyLocale, locale.languageCode);
  }

  DebtStatusFilter get debtStatusFilter {
    return switch (_prefs.getString(_keyDebtStatusFilter)) {
      'active' => DebtStatusFilter.active,
      'completed' => DebtStatusFilter.completed,
      _ => DebtStatusFilter.all,
    };
  }

  Future<void> setDebtStatusFilter(DebtStatusFilter filter) =>
      _prefs.setString(_keyDebtStatusFilter, switch (filter) {
        DebtStatusFilter.active => 'active',
        DebtStatusFilter.completed => 'completed',
        DebtStatusFilter.all => 'all',
      });

  String get baseCurrency => _prefs.getString(_keyBaseCurrency) ?? 'TRY';

  Future<void> setBaseCurrency(String code) =>
      _prefs.setString(_keyBaseCurrency, code);
}
