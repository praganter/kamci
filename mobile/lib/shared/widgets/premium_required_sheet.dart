import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/l10n/l10n.dart';
import '../../core/router/route_names.dart';
import '../../core/theme/app_colors.dart';
import '../../features/subscription/presentation/providers/subscription_providers.dart';

class PremiumRequiredSheet extends StatelessWidget {
  const PremiumRequiredSheet({
    super.key,
    required this.featureName,
    required this.limitCount,
    this.message,
  });

  final String featureName;
  final int limitCount;
  final String? message;

  static Future<void> show(
    BuildContext context, {
    required String featureName,
    required int limitCount,
    String? message,
  }) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => PremiumRequiredSheet(
        featureName: featureName,
        limitCount: limitCount,
        message: message,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        20,
        20,
        MediaQuery.of(context).viewInsets.bottom + 32,
      ),
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
          const SizedBox(height: 24),
          const Icon(
            Icons.workspace_premium_rounded,
            size: 64,
            color: AppColors.warning,
          ),
          const SizedBox(height: 16),
          Text(
            l10n.premiumRequiredTitle,
            style: tt.headlineSmall?.copyWith(fontWeight: FontWeight.w800),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 12),
          Text(
            message ?? l10n.premiumLimitReached(limitCount, featureName),
            style: tt.bodyLarge?.copyWith(color: cs.onSurfaceVariant),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 32),
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton(
              onPressed: () {
                Navigator.pop(context);
                context.pushNamed(RouteNames.premium);
              },
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.primary,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
              child: Text(
                l10n.getPremium,
                style: tt.titleSmall?.copyWith(
                  color: Colors.white,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ),
          const SizedBox(height: 8),
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(
              l10n.maybeLater,
              style: TextStyle(color: cs.onSurfaceVariant),
            ),
          ),
        ],
      ),
    );
  }
}

extension PremiumGuardRef on WidgetRef {
  /// Returns true if the user is premium; otherwise shows [PremiumRequiredSheet]
  /// and returns false. Use in callbacks to gate premium features:
  ///   if (!ref.requirePremium(context, featureName: l10n.reminder)) return;
  bool requirePremium(
    BuildContext context, {
    required String featureName,
    int limitCount = 0,
    String? message,
  }) {
    if (read(isPremiumProvider)) return true;
    PremiumRequiredSheet.show(
      context,
      featureName: featureName,
      limitCount: limitCount,
      message: message,
    );
    return false;
  }
}
