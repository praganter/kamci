import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/utils/validators.dart';
import '../providers/category_providers.dart';

class AddCategorySheet extends ConsumerStatefulWidget {
  const AddCategorySheet({super.key});

  @override
  ConsumerState<AddCategorySheet> createState() => _AddCategorySheetState();
}

class _AddCategorySheetState extends ConsumerState<AddCategorySheet> {
  final _formKey = GlobalKey<FormState>();
  final _nameCtrl = TextEditingController();

  @override
  void dispose() {
    _nameCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    await ref
        .read(categoryFormProvider.notifier)
        .submit(_nameCtrl.text.trim());
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;

    ref.listen(categoryFormProvider, (_, state) {
      if (state.isSuccess) Navigator.of(context).pop(state.createdId);
      if (state.error != null) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(state.error!)));
      }
    });

    final formState = ref.watch(categoryFormProvider);

    return Padding(
      padding: EdgeInsets.fromLTRB(
          24, 24, 24, MediaQuery.viewInsetsOf(context).bottom + 24),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(l10n.newCategory,
                style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 20),
            TextFormField(
              controller: _nameCtrl,
              autofocus: true,
              decoration: InputDecoration(
                labelText: l10n.categoryName,
                prefixIcon: const Icon(Icons.folder_outlined),
              ),
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _submit(),
              validator: (v) =>
                  Validators.required(v, fieldName: l10n.categoryName),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: formState.isLoading ? null : _submit,
              child: formState.isLoading
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Text(l10n.create),
            ),
          ],
        ),
      ),
    );
  }
}
