import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

Future<bool> showConfirmDialog(
  BuildContext context, {
  required String title,
  required String content,
  String confirmText = 'Sil',
  String cancelText = 'İptal',
}) async {
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
                  child: Text(cancelText),
                ),
                CupertinoDialogAction(
                  isDestructiveAction: true,
                  onPressed: () => Navigator.pop(context, true),
                  child: Text(confirmText),
                ),
              ]
            : [
                TextButton(
                  onPressed: () => Navigator.pop(context, false),
                  child: Text(cancelText),
                ),
                FilledButton(
                  onPressed: () => Navigator.pop(context, true),
                  style: FilledButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.error,
                  ),
                  child: Text(confirmText),
                ),
              ],
      );
    },
  );
  return result ?? false;
}
