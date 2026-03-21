import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/utils/validators.dart';
import '../providers/category_providers.dart';

class CategoryFormScreen extends ConsumerStatefulWidget {
  const CategoryFormScreen({super.key, this.categoryId});
  final String? categoryId;

  @override
  ConsumerState<CategoryFormScreen> createState() =>
      _CategoryFormScreenState();
}

class _CategoryFormScreenState extends ConsumerState<CategoryFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameCtrl = TextEditingController();
  bool _initialized = false;

  @override
  void dispose() {
    _nameCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    await ref.read(categoryFormProvider.notifier).submit(
          _nameCtrl.text.trim(),
          categoryId: widget.categoryId,
        );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;

    if (widget.categoryId != null && !_initialized) {
      final categoryAsync =
          ref.watch(categoryDetailProvider(widget.categoryId!));
      categoryAsync.whenData((category) {
        if (!_initialized) {
          _nameCtrl.text = category.name;
          _initialized = true;
        }
      });
    }

    ref.listen(categoryFormProvider, (_, state) {
      if (state.isSuccess) {
        context.pop();
      } else if (state.error != null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(state.error!)),
        );
      }
    });

    final formState = ref.watch(categoryFormProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.categoryId == null
            ? l10n.newCategory
            : l10n.editCategory),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                controller: _nameCtrl,
                decoration: InputDecoration(
                  labelText: l10n.categoryName,
                  prefixIcon: const Icon(Icons.folder_outlined),
                ),
                textInputAction: TextInputAction.done,
                onFieldSubmitted: (_) => _submit(),
                validator: (v) =>
                    Validators.required(v, fieldName: l10n.categoryName),
              ),
              const SizedBox(height: 32),
              ElevatedButton(
                onPressed: formState.isLoading ? null : _submit,
                child: formState.isLoading
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : Text(widget.categoryId == null
                        ? l10n.create
                        : l10n.update),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
