import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import '../../core/l10n/l10n.dart';

Future<bool> showConfirmDialog(
  BuildContext context, {
  required String title,
  required String content,
  String? confirmText,
  String? cancelText,
}) async {
  final l10n = context.l10n;
  final confirmLabel = confirmText ?? l10n.delete;
  final cancelLabel = cancelText ?? l10n.cancel;
  final result = await showAdaptiveDialog<bool>(
    context: context,
    builder: (context) {
      final isIOS = Theme.of(context).platform == TargetPlatform.iOS;
      return AlertDialog.adaptive(
        title: Text(title),
        content: Text(content),
        actions: isIOS
            ? [
                CupertinoDialogAction(
                  onPressed: () => Navigator.pop(context, false),
                  child: Text(cancelLabel),
                ),
                CupertinoDialogAction(
                  isDestructiveAction: true,
                  onPressed: () => Navigator.pop(context, true),
                  child: Text(confirmLabel),
                ),
              ]
            : [
                TextButton(
                  onPressed: () => Navigator.pop(context, false),
                  child: Text(cancelLabel),
                ),
                FilledButton(
                  onPressed: () => Navigator.pop(context, true),
                  style: FilledButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.error,
                  ),
                  child: Text(confirmLabel),
                ),
              ],
      );
    },
  );
  return result ?? false;
}
