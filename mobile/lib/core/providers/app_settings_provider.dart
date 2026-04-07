import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/settings_service.dart';
import '../../features/debt/domain/entities/debt.dart';

final sharedPreferencesProvider = Provider<SharedPreferences>(
  (ref) => throw UnimplementedError(),
);

final settingsServiceProvider = Provider<SettingsService>(
  (ref) => SettingsService(ref.watch(sharedPreferencesProvider)),
);

// ── Theme ──────────────────────────────────────────────────────────────────

class ThemeModeNotifier extends StateNotifier<ThemeMode> {
  ThemeModeNotifier(this._service) : super(_service.themeMode);
  final SettingsService _service;

  Future<void> set(ThemeMode mode) async {
    await _service.setThemeMode(mode);
    state = mode;
  }
}

final themeModeProvider = StateNotifierProvider<ThemeModeNotifier, ThemeMode>((
  ref,
) {
  return ThemeModeNotifier(ref.watch(settingsServiceProvider));
});

// ── Locale ─────────────────────────────────────────────────────────────────

class LocaleNotifier extends StateNotifier<Locale?> {
  LocaleNotifier(this._service) : super(_service.locale);
  final SettingsService _service;

  Future<void> set(Locale? locale) async {
    await _service.setLocale(locale);
    state = locale;
  }
}

final localeProvider = StateNotifierProvider<LocaleNotifier, Locale?>((ref) {
  return LocaleNotifier(ref.watch(settingsServiceProvider));
});

// ── Debt tab (0 = owned, 1 = shared) ───────────────────────────────────────

final debtTabProvider = StateProvider<int>((ref) => 0);

// ── Debt status filter ─────────────────────────────────────────────────────

class DebtStatusFilterNotifier extends StateNotifier<DebtStatusFilter> {
  DebtStatusFilterNotifier(this._service) : super(_service.debtStatusFilter);
  final SettingsService _service;

  Future<void> set(DebtStatusFilter filter) async {
    await _service.setDebtStatusFilter(filter);
    state = filter;
  }
}

final debtStatusFilterProvider =
    StateNotifierProvider<DebtStatusFilterNotifier, DebtStatusFilter>((ref) {
      return DebtStatusFilterNotifier(ref.watch(settingsServiceProvider));
    });
