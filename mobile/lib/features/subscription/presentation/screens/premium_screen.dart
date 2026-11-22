import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:in_app_purchase/in_app_purchase.dart';

import '../../../../shared/widgets/glow_border.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/utils/snackbar_utils.dart';
import '../../data/store/purchase_store.dart';
import '../../subscription_constants.dart';
import '../providers/purchase_flow.dart';

class PremiumScreen extends ConsumerStatefulWidget {
  const PremiumScreen({super.key});

  @override
  ConsumerState<PremiumScreen> createState() => _PremiumScreenState();
}

class _PremiumScreenState extends ConsumerState<PremiumScreen> {
  List<ProductDetails> _products = [];
  String? _selectedProductId;
  bool _loading = true;
  String? _storeError;

  @override
  void initState() {
    super.initState();
    _loadProducts();
  }

  Future<void> _loadProducts() async {
    final store = ref.read(purchaseStoreProvider);
    var products = <ProductDetails>[];
    String? error;
    try {
      if (await store.isAvailable()) {
        final response = await store.queryProducts(
          SubscriptionConstants.productIds,
        );
        products = response.productDetails;
        if (products.isEmpty) error = 'no_products';
      } else {
        error = 'store_unavailable';
      }
    } catch (_) {
      error = 'store_unavailable';
    }
    if (!mounted) return;
    setState(() {
      _products = products;
      _storeError = error;
      _selectedProductId =
          products
              .where((p) => p.id == SubscriptionConstants.yearlyProductId)
              .map((p) => p.id)
              .firstOrNull ??
          products.firstOrNull?.id;
      _loading = false;
    });
  }

  void _onPurchaseState(PurchaseState next) {
    switch (next.phase) {
      case PurchasePhase.success:
        Navigator.of(context).pop();
      case PurchasePhase.failed:
        final error = next.error;
        showErrorSnackBar(
          context,
          error is ApiException ? error : context.l10n.purchaseFailed,
        );
      case PurchasePhase.pending:
        showSuccessSnackBar(context, context.l10n.purchasePending);
      case PurchasePhase.nothingToRestore:
        showSuccessSnackBar(context, context.l10n.nothingToRestore);
      case PurchasePhase.idle:
      case PurchasePhase.purchasing:
      case PurchasePhase.restoring:
      case PurchasePhase.verifying:
      case PurchasePhase.canceled:
        break;
    }
  }

  Future<void> _purchase() async {
    final product = _products
        .where((p) => p.id == _selectedProductId)
        .firstOrNull;
    if (product == null) return;
    await ref.read(purchaseFlowProvider.notifier).buy(product);
  }

  Future<void> _restore() => ref.read(purchaseFlowProvider.notifier).restore();

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final l10n = context.l10n;
    ref.listen(purchaseFlowProvider, (_, next) => _onPurchaseState(next));
    final purchasing = ref.watch(purchaseFlowProvider).busy;

    return Scaffold(
      backgroundColor: cs.surface,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: Semantics(
          container: true,
          identifier: 'close',
          child: IconButton(
            icon: Icon(Icons.close_rounded, color: cs.onSurface),
            onPressed: () => Navigator.of(context).pop(),
          ),
        ),
      ),
      body: SafeArea(
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : Column(
                children: [
                  Expanded(
                    child: SingleChildScrollView(
                      padding: const EdgeInsets.symmetric(horizontal: 24),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          const SizedBox(height: 8),
                          _Header(tt: tt, l10n: l10n),
                          const SizedBox(height: 32),
                          _FeatureList(l10n: l10n, tt: tt, cs: cs),
                          const SizedBox(height: 32),
                          if (_storeError != null)
                            _StoreErrorCard(
                              errorCode: _storeError!,
                              cs: cs,
                              tt: tt,
                              onRetry: () {
                                setState(() {
                                  _loading = true;
                                  _storeError = null;
                                });
                                _loadProducts();
                              },
                            )
                          else
                            _PlanSelector(
                              products: _products,
                              selectedId: _selectedProductId,
                              onSelect: (id) =>
                                  setState(() => _selectedProductId = id),
                              tt: tt,
                              cs: cs,
                              l10n: l10n,
                            ),
                          const SizedBox(height: 12),
                        ],
                      ),
                    ),
                  ),
                  _Footer(
                    purchasing: purchasing,
                    canPurchase: _selectedProductId != null,
                    onPurchase: _purchase,
                    onRestore: _restore,
                    tt: tt,
                    cs: cs,
                    l10n: l10n,
                  ),
                ],
              ),
      ),
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.tt, required this.l10n});
  final TextTheme tt;
  final dynamic l10n;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          width: 72,
          height: 72,
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFFF59E0B), Color(0xFFF97316)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(20),
          ),
          child: const Icon(
            Icons.workspace_premium_rounded,
            size: 38,
            color: Colors.white,
          ),
        ),
        const SizedBox(height: 16),
        Text(
          l10n.goPremium,
          style: tt.headlineMedium?.copyWith(fontWeight: FontWeight.w800),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          l10n.premiumUnlockDesc,
          style: tt.bodyLarge?.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }
}

class _FeatureList extends StatelessWidget {
  const _FeatureList({required this.l10n, required this.tt, required this.cs});

  final dynamic l10n;
  final TextTheme tt;
  final ColorScheme cs;

  @override
  Widget build(BuildContext context) {
    final features = [
      l10n.featureUnlimitedDebts,
      l10n.featureUnlimitedCategories,
      l10n.featureReminders,
      l10n.featureCategorySharing,
    ];

    return Container(
      decoration: BoxDecoration(
        color: cs.surfaceContainerHighest.withValues(alpha: 0.4),
        borderRadius: BorderRadius.circular(14),
      ),
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Column(
        children: features
            .map(
              (f) => Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 10,
                ),
                child: Row(
                  children: [
                    Container(
                      width: 28,
                      height: 28,
                      decoration: BoxDecoration(
                        color: AppColors.success.withValues(alpha: 0.12),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        Icons.check_rounded,
                        size: 16,
                        color: AppColors.success,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Text(
                      f as String,
                      style: tt.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              ),
            )
            .toList(),
      ),
    );
  }
}

class _PlanSelector extends StatelessWidget {
  const _PlanSelector({
    required this.products,
    required this.selectedId,
    required this.onSelect,
    required this.tt,
    required this.cs,
    required this.l10n,
  });

  final List<ProductDetails> products;
  final String? selectedId;
  final ValueChanged<String> onSelect;
  final TextTheme tt;
  final ColorScheme cs;
  final dynamic l10n;

  ProductDetails? _find(String id) =>
      products.where((p) => p.id == id).firstOrNull;

  @override
  Widget build(BuildContext context) {
    final monthly = _find(SubscriptionConstants.monthlyProductId);
    final yearly = _find(SubscriptionConstants.yearlyProductId);

    return Column(
      children: [
        if (yearly != null)
          _PlanCard(
            period: l10n.periodYearly,
            priceLabel: '${yearly.price} ${l10n.perYear}',
            badge: l10n.bestValue,
            selected: selectedId == yearly.id,
            onTap: () => onSelect(yearly.id),
            tt: tt,
            cs: cs,
          ),
        if (yearly != null && monthly != null) const SizedBox(height: 12),
        if (monthly != null)
          _PlanCard(
            period: l10n.periodMonthly,
            priceLabel: '${monthly.price} ${l10n.perMonth}',
            badge: null,
            selected: selectedId == monthly.id,
            onTap: () => onSelect(monthly.id),
            tt: tt,
            cs: cs,
          ),
      ],
    );
  }
}

class _PlanCard extends StatelessWidget {
  const _PlanCard({
    required this.period,
    required this.priceLabel,
    required this.badge,
    required this.selected,
    required this.onTap,
    required this.tt,
    required this.cs,
  });

  final String period;
  final String priceLabel;
  final String? badge;
  final bool selected;
  final VoidCallback onTap;
  final TextTheme tt;
  final ColorScheme cs;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        decoration: BoxDecoration(
          color: selected
              ? AppColors.primary.withValues(alpha: 0.08)
              : cs.surfaceContainerHighest.withValues(alpha: 0.4),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: selected ? AppColors.primary : cs.outlineVariant,
            width: selected ? 2 : 1,
          ),
        ),
        child: Row(
          children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 150),
              width: 22,
              height: 22,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: selected ? AppColors.primary : cs.outline,
                  width: 2,
                ),
                color: selected ? AppColors.primary : Colors.transparent,
              ),
              child: selected
                  ? const Icon(Icons.check, size: 13, color: Colors.white)
                  : null,
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(
                        period,
                        style: tt.titleSmall?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      if (badge != null) ...[
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 2,
                          ),
                          decoration: BoxDecoration(
                            color: AppColors.warning.withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: Text(
                            badge!,
                            style: tt.labelSmall?.copyWith(
                              color: AppColors.warning,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(
                    priceLabel,
                    style: tt.bodyMedium?.copyWith(
                      color: selected ? AppColors.primary : cs.onSurfaceVariant,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Footer extends StatelessWidget {
  const _Footer({
    required this.purchasing,
    required this.canPurchase,
    required this.onPurchase,
    required this.onRestore,
    required this.tt,
    required this.cs,
    required this.l10n,
  });

  final bool purchasing;
  final bool canPurchase;
  final VoidCallback onPurchase;
  final VoidCallback onRestore;
  final TextTheme tt;
  final ColorScheme cs;
  final dynamic l10n;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
      child: Column(
        children: [
          GlowBorder(
            enabled: canPurchase && !purchasing,
            radius: 16,
            child: SizedBox(
              width: double.infinity,
              height: 52,
              child: FilledButton(
                onPressed: (purchasing || !canPurchase) ? null : onPurchase,
                style: FilledButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
                child: purchasing
                    ? const SizedBox(
                        width: 22,
                        height: 22,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.5,
                          color: Colors.white,
                        ),
                      )
                    : Text(
                        l10n.getPremium,
                        style: tt.titleSmall?.copyWith(
                          color: Colors.white,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
              ),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            l10n.autoRenewInfo,
            style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
            textAlign: TextAlign.center,
          ),
          TextButton(
            onPressed: purchasing ? null : onRestore,
            child: Text(
              l10n.restorePurchases,
              style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
            ),
          ),
        ],
      ),
    );
  }
}

class _StoreErrorCard extends StatelessWidget {
  const _StoreErrorCard({
    required this.errorCode,
    required this.cs,
    required this.tt,
    required this.onRetry,
  });

  final String errorCode;
  final ColorScheme cs;
  final TextTheme tt;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final message = errorCode == 'store_unavailable'
        ? l10n.storeUnavailable
        : l10n.subscriptionPlansLoadFailed;

    return Container(
      padding: const EdgeInsets.fromLTRB(16, 16, 8, 8),
      decoration: BoxDecoration(
        color: cs.errorContainer.withValues(alpha: 0.4),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: cs.error.withValues(alpha: 0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(Icons.warning_amber_rounded, color: cs.error, size: 20),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  message,
                  style: tt.bodySmall?.copyWith(color: cs.onErrorContainer),
                ),
              ),
            ],
          ),
          TextButton(onPressed: onRetry, child: Text(l10n.retry)),
        ],
      ),
    );
  }
}
