import 'package:flutter/material.dart';
import '../../features/category/domain/entities/member_embedded.dart';
import '../l10n/l10n.dart';

abstract final class PermissionUtils {
  static bool check(
    BuildContext context, {
    required bool isOwner,
    AccessLevel? level,
    required bool Function(AccessLevel level) action,
  }) {
    final hasPermission = isOwner || (level != null && action(level));

    if (!hasPermission) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(context.l10n.noPermission),
          backgroundColor: Theme.of(context).colorScheme.error,
        ),
      );
    }

    return hasPermission;
  }
}
