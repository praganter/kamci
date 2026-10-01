import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../shared/widgets/confirm_dialog.dart';
import '../../../category/data/repositories/category_repository_impl.dart';
import '../../../category/domain/entities/member_embedded.dart';
import '../../../category/presentation/providers/category_providers.dart';
import '../providers/debt_providers.dart';

enum _SheetPage { main, editName, addMember }

class CategoryManagementSheet extends ConsumerStatefulWidget {
  const CategoryManagementSheet({
    super.key,
    required this.categoryId,
    required this.categoryName,
    required this.owned,
  });

  final String categoryId;
  final String categoryName;
  final bool owned;

  @override
  ConsumerState<CategoryManagementSheet> createState() =>
      _CategoryManagementSheetState();
}

class _CategoryManagementSheetState
    extends ConsumerState<CategoryManagementSheet> {
  _SheetPage _page = _SheetPage.main;
  late final TextEditingController _nameCtrl;
  final _emailCtrl = TextEditingController();
  AccessLevel _accessLevel = AccessLevel.read;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _nameCtrl = TextEditingController(text: widget.categoryName);
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _emailCtrl.dispose();
    super.dispose();
  }

  Future<void> _saveName() async {
    final name = _nameCtrl.text.trim();
    if (name.isEmpty) return;
    setState(() => _isLoading = true);
    try {
      await ref
          .read(categoryRepositoryProvider)
          .update(widget.categoryId, name);
      ref.invalidate(categoryListProvider);
      ref.invalidate(debtGroupedProvider);
      if (mounted) Navigator.of(context).pop();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _addMember() async {
    final email = _emailCtrl.text.trim();
    if (email.isEmpty) return;
    setState(() => _isLoading = true);
    try {
      await ref
          .read(categoryDetailProvider(widget.categoryId).notifier)
          .addMember(email, _accessLevel);
      ref.invalidate(debtGroupedProvider);
      if (mounted) {
        _emailCtrl.clear();
        setState(() {
          _page = _SheetPage.main;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(e.toString())));
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _removeMember(String userId) async {
    try {
      await ref
          .read(categoryDetailProvider(widget.categoryId).notifier)
          .removeMember(userId);
      ref.invalidate(debtGroupedProvider);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
  }

  Future<void> _delete() async {
    final l10n = context.l10n;
    final confirmed = await showConfirmDialog(
      context,
      title: l10n.deleteCategory,
      content: l10n.deleteCategoryConfirm(widget.categoryName),
    );
    if (!confirmed || !mounted) return;
    try {
      await ref
          .read(categoryListProvider.notifier)
          .deleteCategory(widget.categoryId);
      ref.invalidate(debtGroupedProvider);
      if (mounted) Navigator.of(context).pop();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Padding(
      padding: EdgeInsets.fromLTRB(
        24,
        24,
        24,
        MediaQuery.viewInsetsOf(context).bottom + 24,
      ),
      child: switch (_page) {
        _SheetPage.main => _buildMain(context, cs),
        _SheetPage.editName => _buildEditName(context),
        _SheetPage.addMember => _buildAddMember(context),
      },
    );
  }

  Widget _buildMain(BuildContext context, ColorScheme cs) {
    final l10n = context.l10n;
    final categoryAsync = ref.watch(categoryDetailProvider(widget.categoryId));

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          widget.categoryName,
          style: Theme.of(context).textTheme.titleLarge,
        ),
        const SizedBox(height: 8),
        if (widget.owned) ...[
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Icon(Icons.edit_outlined, color: cs.primary),
            title: Text(l10n.edit),
            onTap: () => setState(() => _page = _SheetPage.editName),
          ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Icon(Icons.person_add_outlined, color: cs.primary),
            title: Text(l10n.addMember),
            onTap: () => setState(() => _page = _SheetPage.addMember),
          ),
        ],
        categoryAsync.when(
          loading: () => const Padding(
            padding: EdgeInsets.symmetric(vertical: 8),
            child: LinearProgressIndicator(),
          ),
          error: (e, s) => const SizedBox.shrink(),
          data: (category) {
            if (category.members.isEmpty) return const SizedBox.shrink();
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                const Divider(),
                Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: Text(
                    l10n.memberCountTitle(category.members.length),
                    style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      color: cs.onSurfaceVariant,
                    ),
                  ),
                ),
                ...category.members.map(
                  (m) => ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.person_outline),
                    title: Text(
                      m.email ?? m.userId ?? '',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    subtitle: Text(switch (m.accessLevel) {
                      AccessLevel.read =>
                        '${l10n.accessRead} — ${l10n.accessReadDesc}',
                      AccessLevel.write =>
                        '${l10n.accessWrite} — ${l10n.accessWriteDesc}',
                      AccessLevel.edit =>
                        '${l10n.accessEdit} — ${l10n.accessEditDesc}',
                      AccessLevel.admin =>
                        '${l10n.accessDelete} — ${l10n.accessDeleteDesc}',
                    }),
                    trailing: widget.owned
                        ? IconButton(
                            icon: const Icon(
                              Icons.person_remove_outlined,
                              color: Colors.red,
                            ),
                            onPressed: () =>
                                _removeMember(m.userId ?? m.email ?? ''),
                          )
                        : null,
                  ),
                ),
              ],
            );
          },
        ),
        if (widget.owned) ...[
          const Divider(),
          TextButton.icon(
            onPressed: _delete,
            icon: const Icon(Icons.delete_outlined, color: Colors.red),
            label: Text(
              l10n.deleteCategory,
              style: const TextStyle(color: Colors.red),
            ),
            style: TextButton.styleFrom(alignment: Alignment.centerLeft),
          ),
        ],
      ],
    );
  }

  Widget _buildEditName(BuildContext context) {
    final l10n = context.l10n;
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            IconButton(
              icon: const Icon(Icons.arrow_back),
              onPressed: () => setState(() => _page = _SheetPage.main),
            ),
            Text(
              l10n.editCategory,
              style: Theme.of(context).textTheme.titleLarge,
            ),
          ],
        ),
        const SizedBox(height: 16),
        TextField(
          controller: _nameCtrl,
          autofocus: true,
          decoration: InputDecoration(
            labelText: l10n.categoryName,
            prefixIcon: const Icon(Icons.folder_outlined),
          ),
        ),
        const SizedBox(height: 20),
        ElevatedButton(
          onPressed: _isLoading ? null : _saveName,
          child: _isLoading
              ? const SizedBox(
                  height: 20,
                  width: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Text(l10n.save),
        ),
      ],
    );
  }

  Widget _buildAddMember(BuildContext context) {
    final l10n = context.l10n;
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            IconButton(
              icon: const Icon(Icons.arrow_back),
              onPressed: () => setState(() => _page = _SheetPage.main),
            ),
            Text(l10n.addMember, style: Theme.of(context).textTheme.titleLarge),
          ],
        ),
        const SizedBox(height: 16),
        TextField(
          controller: _emailCtrl,
          autofocus: true,
          keyboardType: TextInputType.emailAddress,
          decoration: InputDecoration(
            labelText: l10n.memberEmail,
            prefixIcon: const Icon(Icons.email_outlined),
          ),
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<AccessLevel>(
          initialValue: _accessLevel,
          decoration: InputDecoration(
            labelText: l10n.accessLevel,
            prefixIcon: const Icon(Icons.security_outlined),
          ),
          items: AccessLevel.values.map((a) {
            final label = switch (a) {
              AccessLevel.read => l10n.accessRead,
              AccessLevel.write => l10n.accessWrite,
              AccessLevel.edit => l10n.accessEdit,
              AccessLevel.admin => l10n.accessDelete,
            };
            final desc = switch (a) {
              AccessLevel.read => l10n.accessReadDesc,
              AccessLevel.write => l10n.accessWriteDesc,
              AccessLevel.edit => l10n.accessEditDesc,
              AccessLevel.admin => l10n.accessDeleteDesc,
            };
            return DropdownMenuItem(
              value: a,
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
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            );
          }).toList(),
          onChanged: (v) => setState(() => _accessLevel = v!),
        ),
        const SizedBox(height: 20),
        ElevatedButton(
          onPressed: _isLoading ? null : _addMember,
          child: _isLoading
              ? const SizedBox(
                  height: 20,
                  width: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Text(l10n.addMember),
        ),
      ],
    );
  }
}
