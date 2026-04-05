import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:in_app_purchase/in_app_purchase.dart';

import '../../../../core/l10n/l10n.dart';
import '../../../../core/theme/app_colors.dart';
import '../../data/models/verify_receipt_request.dart';
import '../../subscription_constants.dart';
import '../providers/subscription_providers.dart';

class PremiumCard extends ConsumerStatefulWidget {
  const PremiumCard({super.key});

  @override
  ConsumerState<PremiumCard> createState() => _PremiumCardState();
}

class _PremiumCardState extends ConsumerState<PremiumCard> {
  StreamSubscription<List<PurchaseDetails>>? _purchaseSub;
  bool _purchasing = false;

  @override
  void initState() {
    super.initState();
    _purchaseSub =
        InAppPurchase.instance.purchaseStream.listen(_onPurchaseUpdate);
  }

  @override
  void dispose() {
    _purchaseSub?.cancel();
    super.dispose();
  }

  Future<void> _onPurchaseUpdate(List<PurchaseDetails> purchases) async {
    for (final p in purchases) {
      switch (p.status) {
        case PurchaseStatus.purchased:
        case PurchaseStatus.restored:
          await ref.read(subscriptionNotifierProvider.notifier).verifyReceipt(
                VerifyReceiptRequest(
                  platform: Platform.isIOS ? 'IOS' : 'ANDROID',
                  plan: SubscriptionConstants.planForProductId(p.productID),
                  receiptData: Platform.isIOS
                      ? p.verificationData.serverVerificationData
                      : null,
                  purchaseToken: Platform.isAndroid
                      ? p.verificationData.serverVerificationData
                      : null,
                ),
              );
          await InAppPurchase.instance.completePurchase(p);
        case PurchaseStatus.error:
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

  Future<void> _showPlanSheet(BuildContext context) async {
    final available = await InAppPurchase.instance.isAvailable();
    if (!available || !context.mounted) return;

    final response = await InAppPurchase.instance
        .queryProductDetails(SubscriptionConstants.productIds);

    if (!context.mounted) return;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => _PlanSheet(
        products: response.productDetails,
        onSelect: (product) {
          setState(() => _purchasing = true);
          InAppPurchase.instance.buyNonConsumable(
            purchaseParam: PurchaseParam(productDetails: product),
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isPremium = ref.watch(isPremiumProvider);
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return isPremium
        ? _PremiumActiveCard(cs: cs, tt: tt)
        : _UpgradeCard(
            cs: cs,
            tt: tt,
            loading: _purchasing,
            onTap: () => _showPlanSheet(context),
          );
  }
}

// ── Premium active card ───────────────────────────────────────────────────────

class _PremiumActiveCard extends ConsumerWidget {
  const _PremiumActiveCard({required this.cs, required this.tt});

  final ColorScheme cs;
  final TextTheme tt;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final statusAsync = ref.watch(subscriptionStatusProvider);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFFF59E0B), Color(0xFFF97316)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.workspace_premium_rounded,
            size: 32,
            color: Colors.white,
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  context.l10n.premium,
                  style: tt.titleMedium?.copyWith(
                    color: Colors.white,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                statusAsync.whenOrNull(
                      data: (sub) => sub.expiresDate != null
                          ? Text(
                              context.l10n.renewsOn(_fmt(sub.expiresDate!)),
                              style: tt.bodySmall?.copyWith(
                                color: Colors.white.withValues(alpha: 0.85),
                              ),
                            )
                          : null,
                    ) ??
                    const SizedBox.shrink(),
              ],
            ),
          ),
          Container(
            padding:
                const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.25),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(
              context.l10n.premiumActive,
              style: tt.labelSmall?.copyWith(
                color: Colors.white,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }

  String _fmt(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';
}

// ── Upgrade CTA card ──────────────────────────────────────────────────────────

class _UpgradeCard extends StatelessWidget {
  const _UpgradeCard({
    required this.cs,
    required this.tt,
    required this.loading,
    required this.onTap,
  });

  final ColorScheme cs;
  final TextTheme tt;
  final bool loading;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            AppColors.primary.withValues(alpha: 0.1),
            AppColors.secondary.withValues(alpha: 0.07),
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: AppColors.primary.withValues(alpha: 0.3),
        ),
      ),
      child: InkWell(
        onTap: loading ? null : onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(
                  Icons.workspace_premium_rounded,
                  size: 24,
                  color: AppColors.primary,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      context.l10n.getPremium,
                      style: tt.titleSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      context.l10n.premiumUnlimitedDesc,
                      style: tt.bodySmall?.copyWith(
                        color: cs.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              if (loading)
                const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              else
                const Icon(
                  Icons.chevron_right_rounded,
                  size: 20,
                  color: AppColors.primary,
                ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Plan selection sheet ──────────────────────────────────────────────────────

class _PlanSheet extends StatelessWidget {
  const _PlanSheet({required this.products, required this.onSelect});

  final List<ProductDetails> products;
  final void Function(ProductDetails) onSelect;

  ProductDetails? _find(String id) =>
      products.where((p) => p.id == id).firstOrNull;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final monthly = _find(SubscriptionConstants.monthlyProductId);
    final yearly = _find(SubscriptionConstants.yearlyProductId);

    return Padding(
      padding: EdgeInsets.fromLTRB(
        20, 20, 20, MediaQuery.of(context).viewInsets.bottom + 32),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: cs.outlineVariant,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: 20),
          const Icon(
            Icons.workspace_premium_rounded,
            size: 48,
            color: AppColors.warning,
          ),
          const SizedBox(height: 12),
          Text(
            context.l10n.goPremium,
            style: tt.titleLarge?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 6),
          Text(
            context.l10n.premiumUnlockDesc,
            style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 24),
          _PlanOption(
            label: context.l10n.periodMonthly,
            price: monthly?.price,
            badge: null,
            onTap: monthly == null
                ? null
                : () {
                    Navigator.pop(context);
                    onSelect(monthly);
                  },
          ),
          const SizedBox(height: 10),
          _PlanOption(
            label: context.l10n.periodYearly,
            price: yearly?.price,
            badge: context.l10n.bestValue,
            onTap: yearly == null
                ? null
                : () {
                    Navigator.pop(context);
                    onSelect(yearly);
                  },
          ),
          const SizedBox(height: 16),
          Text(
            context.l10n.autoRenewInfo,
            style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}

class _PlanOption extends StatelessWidget {
  const _PlanOption({
    required this.label,
    required this.price,
    required this.badge,
    required this.onTap,
  });

  final String label;
  final String? price;
  final String? badge;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final enabled = onTap != null;

    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding:
            const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
        decoration: BoxDecoration(
          color: cs.surface,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: enabled
                ? AppColors.primary.withValues(alpha: 0.4)
                : cs.outlineVariant,
            width: 1.5,
          ),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(
                        label,
                        style: tt.titleSmall
                            ?.copyWith(fontWeight: FontWeight.w700),
                      ),
                      if (badge != null) ...[
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 7, vertical: 2),
                          decoration: BoxDecoration(
                            color: AppColors.warning
                                .withValues(alpha: 0.15),
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
                  if (price != null) ...[
                    const SizedBox(height: 2),
                    Text(
                      price!,
                      style: tt.bodyMedium?.copyWith(
                        color: AppColors.primary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ],
              ),
            ),
            Icon(
              Icons.arrow_forward_ios_rounded,
              size: 16,
              color: enabled ? AppColors.primary : cs.onSurfaceVariant,
            ),
          ],
        ),
      ),
    );
  }
}
