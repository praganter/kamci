import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SettingsService {
  const SettingsService(this._prefs);
  final SharedPreferences _prefs;

  static const _keyTheme = 'theme_mode';
  static const _keyLocale = 'locale';

  ThemeMode get themeMode {
    return switch (_prefs.getString(_keyTheme)) {
      'light' => ThemeMode.light,
      'dark' => ThemeMode.dark,
      _ => ThemeMode.system,
    };
  }

  Future<void> setThemeMode(ThemeMode mode) => _prefs.setString(
        _keyTheme,
        switch (mode) {
          ThemeMode.light => 'light',
          ThemeMode.dark => 'dark',
          _ => 'system',
        },
      );

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
}
