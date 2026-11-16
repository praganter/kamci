import 'package:flutter/material.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/utils/validators.dart';
import '../../domain/entities/member_embedded.dart';
import 'access_level_field.dart';

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
          AccessLevelField(
            value: _selectedLevel,
            onChanged: (v) => setState(() => _selectedLevel = v),
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
            if (Validators.isValidEmail(email)) {
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
