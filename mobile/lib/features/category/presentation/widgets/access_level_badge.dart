import 'package:flutter/material.dart';
import '../../../category/domain/entities/member_embedded.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/l10n/l10n.dart';

class AccessLevelBadge extends StatelessWidget {
  const AccessLevelBadge({super.key, required this.level});
  final AccessLevel level;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final (color, label) = switch (level) {
      AccessLevel.read => (AppColors.accessRead, l10n.accessRead),
      AccessLevel.write => (AppColors.accessWrite, l10n.accessWrite),
      AccessLevel.edit => (AppColors.accessEdit, l10n.accessEdit),
      AccessLevel.admin => (AppColors.accessDelete, l10n.accessDelete),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: color,
          fontSize: 11,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
