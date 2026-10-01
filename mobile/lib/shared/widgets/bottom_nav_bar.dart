import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/l10n/l10n.dart';
import '../../core/providers/app_settings_provider.dart';
import '../../core/router/route_names.dart';
import '../../features/category/presentation/providers/category_providers.dart';

class AppShell extends ConsumerWidget {
  const AppShell({super.key, required this.child});
  final Widget child;

  static const _navIds = [
    'navDebts',
    'navCategories',
    'navCharts',
    'navSettings',
  ];

  int _selectedIndex(BuildContext context, WidgetRef ref) {
    final location = GoRouterState.of(context).matchedLocation;
    if (location.startsWith(RoutePaths.categories)) return 1;
    if (location.startsWith(RoutePaths.charts)) return 2;
    if (location.startsWith(RoutePaths.settings)) return 3;
    return ref.watch(debtTabProvider);
  }

  void _onTap(BuildContext context, WidgetRef ref, int index) {
    switch (index) {
      case 0:
        ref.read(debtTabProvider.notifier).state = 0;
        context.goNamed(RouteNames.debts);
      case 1:
        context.goNamed(RouteNames.categories);
      case 2:
        context.goNamed(RouteNames.charts);
      case 3:
        context.pushNamed(RouteNames.settings);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cs = Theme.of(context).colorScheme;
    final l10n = context.l10n;
    final selectedIndex = _selectedIndex(context, ref);
    // Keep categoryListProvider alive across all tab switches so the debt form
    // never opens while categories are still re-fetching after a tab change.
    ref.watch(categoryListProvider);

    final items = [
      (
        Icons.account_balance_wallet_outlined,
        Icons.account_balance_wallet_rounded,
        l10n.debts,
      ),
      (Icons.folder_outlined, Icons.folder_rounded, l10n.categories),
      (Icons.bar_chart_outlined, Icons.bar_chart_rounded, l10n.charts),
      (Icons.settings_outlined, Icons.settings_rounded, l10n.settings),
    ];

    return Scaffold(
      body: child,
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
          child: Container(
            height: 64,
            decoration: BoxDecoration(
              color: cs.surfaceContainer,
              borderRadius: BorderRadius.circular(28),
              boxShadow: [
                BoxShadow(
                  color: cs.shadow.withValues(alpha: 0.12),
                  blurRadius: 24,
                  offset: const Offset(0, 6),
                ),
              ],
            ),
            child: Stack(
              children: [
                // Seçim "ışını": eski sekmeden yenisine kayan parlak çizgi
                Positioned.fill(
                  child: IgnorePointer(
                    child: AnimatedAlign(
                      alignment: Alignment(
                        -1 + 2 * selectedIndex / (items.length - 1),
                        0,
                      ),
                      duration: const Duration(milliseconds: 320),
                      curve: Curves.easeOutCubic,
                      child: FractionallySizedBox(
                        widthFactor: 1 / items.length,
                        child: Align(
                          alignment: Alignment.bottomCenter,
                          child: Padding(
                            padding: const EdgeInsets.only(bottom: 6),
                            child: _NavBeam(
                              key: ValueKey(selectedIndex),
                              color: cs.primary,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                Row(
                  children: List.generate(items.length, (i) {
                    final (icon, selectedIcon, label) = items[i];
                    final isSelected = selectedIndex == i;
                    final color = isSelected ? cs.primary : cs.onSurfaceVariant;
                    return Expanded(
                      // button/selected: ekran okuyucu sekmeyi buton + seçili
                      // durumuyla duyurur. identifier: E2E testleri metin
                      // yerine id ile basar (Android'de Gboard'un "Ayarlar"
                      // butonu da aynı metni taşıyor).
                      child: Semantics(
                        container: true,
                        button: true,
                        selected: isSelected,
                        identifier: _navIds[i],
                        child: GestureDetector(
                          onTap: () => _onTap(context, ref, i),
                          behavior: HitTestBehavior.opaque,
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              AnimatedScale(
                                scale: isSelected ? 1.12 : 1.0,
                                duration: const Duration(milliseconds: 260),
                                curve: Curves.easeOutBack,
                                child: Icon(
                                  isSelected ? selectedIcon : icon,
                                  size: 22,
                                  color: color,
                                ),
                              ),
                              const SizedBox(height: 3),
                              AnimatedDefaultTextStyle(
                                duration: const Duration(milliseconds: 220),
                                style: TextStyle(
                                  fontSize: 10,
                                  fontWeight: isSelected
                                      ? FontWeight.w700
                                      : FontWeight.w500,
                                  color: color,
                                ),
                                child: Text(
                                  label,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              const SizedBox(height: 6),
                            ],
                          ),
                        ),
                      ),
                    );
                  }),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Seçili sekmenin altındaki ışın. Her seçimde (yeni key) yeniden oynar:
/// kayarken uzar, varınca toparlanır — hareket hissi; glow ile parlar.
class _NavBeam extends StatelessWidget {
  const _NavBeam({super.key, required this.color});

  final Color color;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: const Duration(milliseconds: 320),
      curve: Curves.easeOutCubic,
      builder: (context, t, _) {
        // t: 0→1; ortada en uzun (ışın), sonda 20px'e oturur
        final width = 20 + 22 * math.sin(math.pi * t);
        return Container(
          width: width,
          height: 3,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(2),
            boxShadow: [
              BoxShadow(
                color: color.withValues(alpha: 0.55),
                blurRadius: 10,
                spreadRadius: 1,
              ),
            ],
          ),
        );
      },
    );
  }
}
