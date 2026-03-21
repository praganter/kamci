import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/l10n/l10n.dart';
import '../providers/change_password_provider.dart';

class ChangePasswordScreen extends ConsumerStatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  ConsumerState<ChangePasswordScreen> createState() =>
      _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends ConsumerState<ChangePasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _currentCtrl = TextEditingController();
  final _newCtrl = TextEditingController();
  bool _showCurrent = false;
  bool _showNew = false;

  @override
  void dispose() {
    _currentCtrl.dispose();
    _newCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final success = await ref
        .read(changePasswordProvider.notifier)
        .changePassword(_currentCtrl.text, _newCtrl.text);
    if (success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(context.l10n.passwordUpdated)),
      );
      Navigator.of(context).pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    final asyncState = ref.watch(changePasswordProvider);
    final isLoading = asyncState is AsyncLoading;
    final error = asyncState is AsyncError ? asyncState.error as String : null;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.changePassword)),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Current password
              TextFormField(
                controller: _currentCtrl,
                obscureText: !_showCurrent,
                textInputAction: TextInputAction.next,
                decoration: InputDecoration(
                  labelText: l10n.currentPassword,
                  suffixIcon: IconButton(
                    icon: Icon(_showCurrent
                        ? Icons.visibility_off_outlined
                        : Icons.visibility_outlined),
                    onPressed: () =>
                        setState(() => _showCurrent = !_showCurrent),
                  ),
                ),
                validator: (v) =>
                    (v == null || v.isEmpty) ? l10n.password : null,
              ),
              const SizedBox(height: 16),

              // New password
              TextFormField(
                controller: _newCtrl,
                obscureText: !_showNew,
                textInputAction: TextInputAction.done,
                onFieldSubmitted: (_) => _submit(),
                decoration: InputDecoration(
                  labelText: l10n.newPassword,
                  suffixIcon: IconButton(
                    icon: Icon(_showNew
                        ? Icons.visibility_off_outlined
                        : Icons.visibility_outlined),
                    onPressed: () => setState(() => _showNew = !_showNew),
                  ),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return l10n.password;
                  if (v.length < 8) return l10n.password;
                  return null;
                },
              ),
              const SizedBox(height: 8),

              // Error
              if (error != null) ...[
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    color: cs.errorContainer,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    error,
                    style: TextStyle(color: cs.onErrorContainer),
                  ),
                ),
              ],
              const SizedBox(height: 24),

              // Submit
              FilledButton(
                onPressed: isLoading ? null : _submit,
                child: isLoading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : Text(l10n.updatePassword),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
