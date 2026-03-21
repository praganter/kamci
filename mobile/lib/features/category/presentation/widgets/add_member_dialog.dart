import 'package:flutter/material.dart';
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
      title: const Text('Üye Ekle'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _userIdCtrl,
            decoration: const InputDecoration(
              labelText: 'Kullanıcı ID',
              hintText: 'Kullanıcı ID\'sini girin',
            ),
          ),
          const SizedBox(height: 16),
          DropdownButtonFormField<AccessLevel>(
            // ignore: deprecated_member_use
            value: _selectedLevel,
            decoration: const InputDecoration(labelText: 'Erişim Seviyesi'),
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
          child: const Text('İptal'),
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
          child: const Text('Ekle'),
        ),
      ],
    );
  }
}
