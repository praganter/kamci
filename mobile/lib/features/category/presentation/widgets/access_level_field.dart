import 'package:flutter/material.dart';
import '../../../../core/l10n/l10n.dart';
import '../../domain/entities/member_embedded.dart';

/// Access level picker shared by the add-member dialog, bulk share sheet and
/// category management sheet. Menu rows show name + description; the closed
/// field shows only the name so it fits narrow dialogs without overflowing.
class AccessLevelField extends StatelessWidget {
  const AccessLevelField({
    super.key,
    required this.value,
    required this.onChanged,
    this.prefixIcon,
  });

  final AccessLevel value;
  final ValueChanged<AccessLevel> onChanged;
  final Widget? prefixIcon;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    String label(AccessLevel l) => switch (l) {
      AccessLevel.read => l10n.accessRead,
      AccessLevel.write => l10n.accessWrite,
      AccessLevel.edit => l10n.accessEdit,
      AccessLevel.admin => l10n.accessDelete,
    };
    String desc(AccessLevel l) => switch (l) {
      AccessLevel.read => l10n.accessReadDesc,
      AccessLevel.write => l10n.accessWriteDesc,
      AccessLevel.edit => l10n.accessEditDesc,
      AccessLevel.admin => l10n.accessDeleteDesc,
    };

    return DropdownButtonFormField<AccessLevel>(
      initialValue: value,
      isExpanded: true,
      // Menu rows grow to fit the two-line label + description.
      itemHeight: null,
      decoration: InputDecoration(
        labelText: l10n.accessLevel,
        prefixIcon: prefixIcon,
      ),
      selectedItemBuilder: (_) => AccessLevel.values
          .map((l) => Text(label(l), overflow: TextOverflow.ellipsis))
          .toList(),
      items: AccessLevel.values
          .map(
            (l) => DropdownMenuItem(
              value: l,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      label(l),
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    Text(
                      desc(l),
                      style: TextStyle(
                        fontSize: 11,
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          )
          .toList(),
      onChanged: (v) {
        if (v != null) onChanged(v);
      },
    );
  }
}
