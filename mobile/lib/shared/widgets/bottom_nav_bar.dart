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
            child: Row(
              children: List.generate(items.length, (i) {
                final (icon, selectedIcon, label) = items[i];
                final isSelected = selectedIndex == i;
                return Expanded(
                  // button/selected: ekran okuyucu sekmeyi buton + seçili
                  // durumuyla duyurur. identifier: E2E testleri metin yerine
                  // id ile basar (Android'de Gboard'un "Ayarlar" butonu da
                  // aynı metni taşıyor).
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
                          AnimatedContainer(
                            duration: const Duration(milliseconds: 220),
                            curve: Curves.easeInOut,
                            padding: EdgeInsets.symmetric(
                              horizontal: isSelected ? 14 : 0,
                              vertical: 4,
                            ),
                            decoration: BoxDecoration(
                              color: isSelected
                                  ? cs.primaryContainer
                                  : Colors.transparent,
                              borderRadius: BorderRadius.circular(16),
                              boxShadow: isSelected
                                  ? [
                                      BoxShadow(
                                        color: cs.primary.withValues(
                                          alpha: 0.28,
                                        ),
                                        blurRadius: 12,
                                        offset: const Offset(0, 4),
                                      ),
                                    ]
                                  : [],
                            ),
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Icon(
                                  isSelected ? selectedIcon : icon,
                                  size: 22,
                                  color: isSelected
                                      ? cs.onPrimaryContainer
                                      : cs.onSurfaceVariant,
                                ),
                                if (isSelected) ...[
                                  const SizedBox(height: 3),
                                  Text(
                                    label,
                                    style: TextStyle(
                                      fontSize: 10,
                                      fontWeight: FontWeight.w600,
                                      color: cs.onPrimaryContainer,
                                    ),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ],
                              ],
                            ),
                          ),
                          if (!isSelected) ...[
                            const SizedBox(height: 3),
                            Text(
                              label,
                              style: TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.w400,
                                color: cs.onSurfaceVariant,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                );
              }),
            ),
          ),
        ),
      ),
    );
  }
}
