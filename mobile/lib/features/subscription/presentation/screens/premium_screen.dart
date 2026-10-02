import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:in_app_purchase/in_app_purchase.dart';

import '../../../../shared/widgets/glow_border.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/utils/snackbar_utils.dart';
import '../../data/models/verify_receipt_request.dart';
import '../../subscription_constants.dart';
import '../providers/subscription_providers.dart';

class PremiumScreen extends ConsumerStatefulWidget {
  const PremiumScreen({super.key});

  @override
  ConsumerState<PremiumScreen> createState() => _PremiumScreenState();
}

class _PremiumScreenState extends ConsumerState<PremiumScreen> {
  StreamSubscription<List<PurchaseDetails>>? _purchaseSub;
  List<ProductDetails> _products = [];
  String? _selectedProductId;
  bool _loading = true;
  bool _purchasing = false;
  String? _storeError;

  @override
  void initState() {
    super.initState();
    _purchaseSub = InAppPurchase.instance.purchaseStream.listen(
      _onPurchaseUpdate,
    );
    _loadProducts();
  }

  @override
  void dispose() {
    _purchaseSub?.cancel();
    super.dispose();
  }

  Future<void> _loadProducts() async {
    final available = await InAppPurchase.instance.isAvailable();
    if (!available || !mounted) {
      setState(() {
        _loading = false;
        _storeError = 'store_unavailable';
      });
      return;
    }

    final response = await InAppPurchase.instance.queryProductDetails(
      SubscriptionConstants.productIds,
    );

    if (!mounted) return;
    setState(() {
      _products = response.productDetails;
      if (_products.isEmpty) {
        _storeError =
            response.error?.message ??
            (response.notFoundIDs.isNotEmpty
                ? 'not_found:${response.notFoundIDs.join(',')}'
                : 'no_products');
      }
      _selectedProductId = _products
          .where((p) => p.id == SubscriptionConstants.yearlyProductId)
          .map((p) => p.id)
          .firstOrNull;
      _selectedProductId ??= _products.firstOrNull?.id;
      _loading = false;
    });
  }

  Future<void> _onPurchaseUpdate(List<PurchaseDetails> purchases) async {
    for (final p in purchases) {
      switch (p.status) {
        case PurchaseStatus.purchased:
        case PurchaseStatus.restored:
          await ref
              .read(subscriptionNotifierProvider.notifier)
              .verifyReceipt(
                VerifyReceiptRequest(
                  receiptData: p.verificationData.serverVerificationData,
                  plan: SubscriptionConstants.planForProductId(p.productID),
                  productId: p.productID,
                  platform: SubscriptionConstants.platform,
                  packageName: SubscriptionConstants.androidPackageName,
                ),
              );
          await InAppPurchase.instance.completePurchase(p);
          final subState = ref.read(subscriptionNotifierProvider);
          if (subState.hasError && mounted) {
            showErrorSnackBar(context, subState.error.toString());
          } else if (mounted) {
            Navigator.of(context).pop();
          }
        case PurchaseStatus.error:
          if (p.pendingCompletePurchase) {
            await InAppPurchase.instance.completePurchase(p);
          }
          if (mounted) {
            showErrorSnackBar(
              context,
              p.error?.message ?? 'Satın alma sırasında bir hata oluştu.',
            );
          }
        case PurchaseStatus.canceled:
          if (p.pendingCompletePurchase) {
            await InAppPurchase.instance.completePurchase(p);
          }
        case PurchaseStatus.pending:
          break;
      }
    }
    if (mounted) setState(() => _purchasing = false);
  }

  Future<void> _purchase() async {
    final product = _products
        .where((p) => p.id == _selectedProductId)
        .firstOrNull;
    if (product == null) return;
    setState(() => _purchasing = true);
    await InAppPurchase.instance.buyNonConsumable(
      purchaseParam: PurchaseParam(productDetails: product),
    );
  }

  Future<void> _restore() async {
    setState(() => _purchasing = true);
    await InAppPurchase.instance.restorePurchases();
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final l10n = context.l10n;

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
                    purchasing: _purchasing,
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

// ── Header ────────────────────────────────────────────────────────────────────

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

// ── Feature list ──────────────────────────────────────────────────────────────

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

// ── Plan selector ─────────────────────────────────────────────────────────────

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
            product: yearly,
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
            product: monthly,
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
    required this.product,
    required this.period,
    required this.priceLabel,
    required this.badge,
    required this.selected,
    required this.onTap,
    required this.tt,
    required this.cs,
  });

  final ProductDetails product;
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

// ── Footer ────────────────────────────────────────────────────────────────────

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
          // Premium vurgusu: aktifken dönen gradient çerçeve + glow
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

// ── Store error card ──────────────────────────────────────────────────────────

class _StoreErrorCard extends StatelessWidget {
  const _StoreErrorCard({
    required this.errorCode,
    required this.cs,
    required this.tt,
  });

  final String errorCode;
  final ColorScheme cs;
  final TextTheme tt;

  @override
  Widget build(BuildContext context) {
    final isUnavailable = errorCode == 'store_unavailable';
    final message = isUnavailable
        ? 'App Store\'a bağlanılamadı. İnternet bağlantını kontrol et.'
        : errorCode.startsWith('not_found:')
        ? 'Abonelik planları bulunamadı.\nUygulama kimliği: ${errorCode.replaceFirst('not_found:', '')}'
        : 'Abonelik planları yüklenemedi: $errorCode';

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: cs.errorContainer.withValues(alpha: 0.4),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: cs.error.withValues(alpha: 0.3)),
      ),
      child: Row(
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
    );
  }
}
