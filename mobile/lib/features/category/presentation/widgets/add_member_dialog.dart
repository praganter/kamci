import 'package:flutter/material.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/utils/validators.dart';
import '../../domain/entities/member_embedded.dart';

class AddMemberDialog extends StatefulWidget {
  const AddMemberDialog({super.key});

  @override
  State<AddMemberDialog> createState() => _AddMemberDialogState();
}

class _AddMemberDialogState extends State<AddMemberDialog> {
  final _emailCtrl = TextEditingController();
  AccessLevel _selectedLevel = AccessLevel.read;

  @override
  void dispose() {
    _emailCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(context.l10n.addMember),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _emailCtrl,
            decoration: InputDecoration(
              labelText: context.l10n.email,
              hintText: 'user@example.com',
            ),
            keyboardType: TextInputType.emailAddress,
          ),
          const SizedBox(height: 16),
          DropdownButtonFormField<AccessLevel>(
            initialValue: _selectedLevel,
            decoration: InputDecoration(labelText: context.l10n.accessLevel),
            items: AccessLevel.values.map((l) {
              final label = switch (l) {
                AccessLevel.read => context.l10n.accessRead,
                AccessLevel.write => context.l10n.accessWrite,
                AccessLevel.edit => context.l10n.accessEdit,
                AccessLevel.admin => context.l10n.accessDelete,
              };
              final desc = switch (l) {
                AccessLevel.read => context.l10n.accessReadDesc,
                AccessLevel.write => context.l10n.accessWriteDesc,
                AccessLevel.edit => context.l10n.accessEditDesc,
                AccessLevel.admin => context.l10n.accessDeleteDesc,
              };
              return DropdownMenuItem(
                value: l,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      label,
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    Text(
                      desc,
                      style: TextStyle(
                        fontSize: 11,
                        color: Theme.of(
                          context,
                        ).colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
            onChanged: (v) => setState(() => _selectedLevel = v!),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text(context.l10n.cancel),
        ),
        FilledButton(
          onPressed: () {
            final email = _emailCtrl.text.trim();
            if (Validators.email(email) == null) {
              Navigator.pop(context, (
                email: email,
                accessLevel: _selectedLevel,
              ));
            }
          },
          child: Text(context.l10n.add),
        ),
      ],
    );
  }
}
