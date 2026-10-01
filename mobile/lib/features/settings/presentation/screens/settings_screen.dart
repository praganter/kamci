import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../../core/theme/theme_helpers.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/providers/app_settings_provider.dart';
import '../../../../core/router/route_names.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../auth/domain/entities/user_detail.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../../debt/data/models/currency_model.dart';
import '../../../debt/presentation/providers/debt_providers.dart';
import '../../../subscription/presentation/widgets/premium_card.dart';
import '../../../../shared/widgets/confirm_dialog.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.settings)),
      body: Column(
        children: [
          Expanded(
            child: ListView(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              children: [
                // ── Premium ───────────────────────────────────────────────
                const PremiumCard(),
                const SizedBox(height: 12),

                // ── Profile ────────────────────────────────────────────────
                _ProfileCard(),
                const SizedBox(height: 20),

                // ── Uygulama Ayarları ──────────────────────────────────────
                _SectionHeader(label: l10n.appearance),
                _SegmentCard(label: l10n.theme, child: _ThemeSegmented()),
                const SizedBox(height: 10),
                _SegmentCard(label: l10n.language, child: _LocaleSegmented()),
                const SizedBox(height: 10),
                _SegmentCard(
                  label: l10n.baseCurrency,
                  child: _BaseCurrencyPicker(),
                ),
                const SizedBox(height: 20),

                // ── Hesap ──────────────────────────────────────────────────
                _SectionHeader(label: l10n.account),
                _SettingsTile(
                  icon: Icons.lock_reset_outlined,
                  title: l10n.changePassword,
                  subtitle: l10n.changePasswordDesc,
                  onTap: () => context.pushNamed(RouteNames.changePassword),
                ),
                const SizedBox(height: 10),
                _SettingsTile(
                  icon: Icons.savings_outlined,
                  title: l10n.income,
                  subtitle: l10n.incomeDesc,
                  onTap: () => context.pushNamed(RouteNames.income),
                ),
                const SizedBox(height: 10),
                _SettingsTile(
                  icon: Icons.family_restroom,
                  title: l10n.household,
                  subtitle: l10n.householdDesc,
                  onTap: () => context.pushNamed(RouteNames.household),
                ),
                const SizedBox(height: 10),
                _SettingsTile(
                  icon: Icons.notifications_outlined,
                  title: l10n.notifications,
                  subtitle: l10n.notificationsDesc,
                  onTap: () => context.pushNamed(RouteNames.notifications),
                ),
                const SizedBox(height: 10),
                _LogoutTile(),
                const SizedBox(height: 20),

                // ── Geri Bildirim ──────────────────────────────────────────
                _SectionHeader(label: l10n.feedback),
                _SettingsTile(
                  icon: Icons.mail_outline_rounded,
                  title: l10n.sendFeedback,
                  subtitle: l10n.feedbackDesc,
                  onTap: () async {
                    final uri = Uri(
                      scheme: 'mailto',
                      path: AppConstants.supportEmail,
                      queryParameters: {
                        'subject': '[${AppConstants.appName}] Feedback',
                      },
                    );
                    if (await canLaunchUrl(uri)) {
                      await launchUrl(uri);
                    }
                  },
                ),
              ],
            ),
          ),

          // ── Footer — sabit alt ──────────────────────────────────────────
          Padding(
            padding: const EdgeInsets.only(bottom: 32, top: 16),
            child: Column(
              children: [
                Text(
                  'Developed by',
                  style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                ),
                const SizedBox(height: 2),
                Text(
                  AppConstants.developerName,
                  style: tt.bodySmall?.copyWith(
                    color: cs.primary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// ── Profile card ─────────────────────────────────────────────────────────────

class _ProfileCard extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final userAsync = ref.watch(userDetailProvider);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      decoration: altBCard(cs),
      child: userAsync.when(
        loading: () => const SizedBox(
          height: 48,
          child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
        ),
        error: (_, _) => const SizedBox.shrink(),
        data: (user) => _ProfileContent(user: user, cs: cs, tt: tt),
      ),
    );
  }
}

class _ProfileContent extends StatelessWidget {
  const _ProfileContent({
    required this.user,
    required this.cs,
    required this.tt,
  });

  final UserDetail user;
  final ColorScheme cs;
  final TextTheme tt;

  @override
  Widget build(BuildContext context) {
    final initial = user.email.isNotEmpty ? user.email[0].toUpperCase() : '?';

    return Row(
      children: [
        CircleAvatar(
          radius: 24,
          backgroundColor: AppColors.primary,
          child: Text(
            initial,
            style: tt.titleMedium?.copyWith(
              color: Colors.white,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                user.email,
                style: tt.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
                overflow: TextOverflow.ellipsis,
              ),
              if (user.premium) ...[
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.warning.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.star_rounded,
                        size: 13,
                        color: AppColors.warning,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        context.l10n.premium,
                        style: tt.labelSmall?.copyWith(
                          color: AppColors.warning,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }
}

// ── Section header ───────────────────────────────────────────────────────────

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 8),
      child: Text(
        label.toUpperCase(),
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: cs.onSurfaceVariant,
          fontWeight: FontWeight.w600,
          letterSpacing: 0.6,
        ),
      ),
    );
  }
}

// ── Settings tile ────────────────────────────────────────────────────────────

class _SettingsTile extends StatelessWidget {
  const _SettingsTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Container(
      decoration: altBCard(cs),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: cs.primaryContainer,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  icon,
                  size: 20,
                  color: cs.brightness == Brightness.light
                      ? cs.onPrimaryContainer
                      : cs.primary,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              Icon(
                Icons.chevron_right_rounded,
                size: 20,
                color: cs.onSurfaceVariant,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Logout tile ──────────────────────────────────────────────────────────────

class _LogoutTile extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;

    return Container(
      decoration: altBCard(cs),
      child: InkWell(
        onTap: () async {
          final confirm = await showConfirmDialog(
            context,
            title: l10n.signOut,
            content: l10n.signOutConfirm,
            confirmText: l10n.signOut,
            cancelText: l10n.cancel,
          );
          if (confirm) {
            await ref.read(authNotifierProvider.notifier).logout();
          }
        },
        borderRadius: BorderRadius.circular(14),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: cs.errorContainer,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  Icons.logout_rounded,
                  size: 20,
                  color: cs.error,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Text(
                  l10n.signOut,
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w600,
                    color: cs.error,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Segment card ─────────────────────────────────────────────────────────────

class _SegmentCard extends StatelessWidget {
  const _SegmentCard({required this.label, required this.child});

  final String label;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: altBCard(cs),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: Theme.of(
              context,
            ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 12),
          SizedBox(width: double.infinity, child: child),
        ],
      ),
    );
  }
}

// ── Theme segmented ──────────────────────────────────────────────────────────

class _ThemeSegmented extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final current = ref.watch(themeModeProvider);

    return SegmentedButton<ThemeMode>(
      showSelectedIcon: false,
      selected: {current},
      onSelectionChanged: (v) =>
          ref.read(themeModeProvider.notifier).set(v.first),
      segments: [
        ButtonSegment(value: ThemeMode.system, label: Text(l10n.themeSystem)),
        ButtonSegment(value: ThemeMode.light, label: Text(l10n.themeLight)),
        ButtonSegment(value: ThemeMode.dark, label: Text(l10n.themeDark)),
      ],
    );
  }
}

// ── Base currency picker ──────────────────────────────────────────────────────

class _BaseCurrencyPicker extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final current = ref.watch(baseCurrencyProvider);
    final currenciesAsync = ref.watch(currencyListProvider);
    final cs = Theme.of(context).colorScheme;

    return InkWell(
      borderRadius: BorderRadius.circular(12),
      onTap: () async {
        final currencies = currenciesAsync.valueOrNull ?? [];
        final selected = await showModalBottomSheet<String>(
          context: context,
          builder: (_) =>
              _CurrencySheet(currencies: currencies, current: current),
        );
        if (selected != null) {
          ref.read(baseCurrencyProvider.notifier).set(selected);
        }
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: cs.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              _label(current, currenciesAsync.valueOrNull),
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
            Icon(Icons.arrow_drop_down_rounded, color: cs.onSurfaceVariant),
          ],
        ),
      ),
    );
  }

  String _label(String code, List<CurrencyModel>? list) {
    final c = list?.where((x) => x.code == code).firstOrNull;
    return c != null ? '${c.symbol} $code' : code;
  }
}

class _CurrencySheet extends StatelessWidget {
  const _CurrencySheet({required this.currencies, required this.current});

  final List<CurrencyModel> currencies;
  final String current;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const SizedBox(height: 12),
          Container(
            width: 36,
            height: 4,
            decoration: BoxDecoration(
              color: cs.outlineVariant,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: Text(
              context.l10n.baseCurrency,
              style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w700),
            ),
          ),
          Flexible(
            child: ListView.builder(
              shrinkWrap: true,
              itemCount: currencies.length,
              itemBuilder: (_, i) {
                final c = currencies[i];
                final selected = c.code == current;
                return ListTile(
                  leading: SizedBox(
                    width: 32,
                    child: Text(
                      c.symbol,
                      style: tt.titleMedium,
                      textAlign: TextAlign.center,
                    ),
                  ),
                  title: Text(c.code, style: tt.bodyMedium),
                  trailing: selected
                      ? Icon(Icons.check_circle_rounded, color: cs.primary)
                      : null,
                  selected: selected,
                  onTap: () => Navigator.of(context).pop(c.code),
                );
              },
            ),
          ),
          const SizedBox(height: 8),
        ],
      ),
    );
  }
}

// ── Locale segmented ─────────────────────────────────────────────────────────

class _LocaleSegmented extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final current = ref.watch(localeProvider);

    return SegmentedButton<String>(
      showSelectedIcon: false,
      selected: {current?.languageCode ?? 'system'},
      onSelectionChanged: (v) {
        final code = v.first;
        ref
            .read(localeProvider.notifier)
            .set(code == 'system' ? null : Locale(code));
      },
      segments: [
        ButtonSegment(value: 'system', label: Text(l10n.langSystem)),
        ButtonSegment(value: 'tr', label: Text(l10n.langTurkish)),
        ButtonSegment(value: 'en', label: Text(l10n.langEnglish)),
      ],
    );
  }
}
