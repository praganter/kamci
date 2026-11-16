import 'package:flutter/material.dart';
import '../l10n/exception_l10n.dart';
import '../l10n/l10n.dart';

void showErrorSnackBar(BuildContext context, Object error) {
  final cs = Theme.of(context).colorScheme;
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      content: Text(
        error.toLocalizedMessage(context.l10n),
        style: TextStyle(color: cs.onError),
      ),
      backgroundColor: cs.error,
    ),
  );
}

void showSuccessSnackBar(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(content: Text(message)),
  );
}
