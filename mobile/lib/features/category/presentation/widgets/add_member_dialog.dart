import 'package:flutter/material.dart';
import '../../../../core/l10n/l10n.dart';
import '../../domain/entities/member_embedded.dart';

class AddMemberDialog extends StatefulWidget {
  const AddMemberDialog({super.key});

  @override
  State<AddMemberDialog> createState() => _AddMemberDialogState();
}

class _AddMemberDialogState extends State<AddMemberDialog> {
  final _userIdCtrl = TextEditingController();
  AccessLevel _selectedLevel = AccessLevel.read;

  @override
  void dispose() {
    _userIdCtrl.dispose();
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
            controller: _userIdCtrl,
            decoration: InputDecoration(
              labelText: context.l10n.userId,
              hintText: context.l10n.userIdHint,
            ),
          ),
          const SizedBox(height: 16),
          DropdownButtonFormField<AccessLevel>(
            // ignore: deprecated_member_use
            value: _selectedLevel,
            decoration: InputDecoration(labelText: context.l10n.accessLevel),
            items: AccessLevel.values
                .map((l) => DropdownMenuItem(value: l, child: Text(l.label)))
                .toList(),
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
            if (_userIdCtrl.text.trim().isNotEmpty) {
              Navigator.pop(
                context,
                (userId: _userIdCtrl.text.trim(), accessLevel: _selectedLevel),
              );
            }
          },
          child: Text(context.l10n.add),
        ),
      ],
    );
  }
}
