import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/l10n/exception_l10n.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/utils/validators.dart';
import '../providers/category_providers.dart';

class AddCategorySheet extends ConsumerStatefulWidget {
  const AddCategorySheet({super.key, this.categoryId, this.initialName});

  final String? categoryId;
  final String? initialName;

  @override
  ConsumerState<AddCategorySheet> createState() => _AddCategorySheetState();
}

class _AddCategorySheetState extends ConsumerState<AddCategorySheet> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameCtrl;

  @override
  void initState() {
    super.initState();
    _nameCtrl = TextEditingController(text: widget.initialName);
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    await ref
        .read(categoryFormProvider.notifier)
        .submit(_nameCtrl.text.trim(), categoryId: widget.categoryId);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final isEdit = widget.categoryId != null;

    ref.listen(categoryFormProvider, (_, state) {
      if (!context.mounted) return;
      if (state.isSuccess) Navigator.of(context).pop(state.createdId);
      if (state.error != null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(state.error!.toLocalizedMessage(context.l10n)),
          ),
        );
      }
    });

    final formState = ref.watch(categoryFormProvider);

    return Padding(
      padding: EdgeInsets.fromLTRB(
        24,
        24,
        24,
        MediaQuery.viewInsetsOf(context).bottom + 24,
      ),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              isEdit ? l10n.editCategory : l10n.newCategory,
              style: Theme.of(context).textTheme.titleLarge,
            ),
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
                  Validators.required(l10n, v, fieldName: l10n.categoryName),
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
                  : Text(isEdit ? l10n.update : l10n.create),
            ),
          ],
        ),
      ),
    );
  }
}
