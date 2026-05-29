import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/utils/validators.dart';
import '../../data/repositories/category_repository_impl.dart';
import '../../domain/entities/member_embedded.dart';
import '../providers/category_providers.dart';

class BulkShareSheet extends ConsumerStatefulWidget {
  const BulkShareSheet({
    super.key,
    required this.categoryIds,
    required this.categoryCount,
  });

  final List<String> categoryIds;
  final int categoryCount;

  static Future<void> show(
    BuildContext context, {
    required List<String> categoryIds,
  }) => showModalBottomSheet(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (context) => BulkShareSheet(
      categoryIds: categoryIds,
      categoryCount: categoryIds.length,
    ),
  );

  @override
  ConsumerState<BulkShareSheet> createState() => _BulkShareSheetState();
}

class _BulkShareSheetState extends ConsumerState<BulkShareSheet> {
  final _emailCtrl = TextEditingController();
  final List<String> _emails = [];
  AccessLevel _selectedLevel = AccessLevel.read;
  bool _isLoading = false;

  @override
  void dispose() {
    _emailCtrl.dispose();
    super.dispose();
  }

  void _addEmail() {
    final email = _emailCtrl.text.trim();
    if (Validators.email(email) != null) return;
    if (_emails.contains(email)) {
      _emailCtrl.clear();
      return;
    }
    setState(() {
      _emails.add(email);
      _emailCtrl.clear();
    });
  }

  void _removeEmail(String email) {
    setState(() => _emails.remove(email));
  }

  Future<void> _submit() async {
    _addEmail(); // flush any pending input
    if (_emails.isEmpty) return;

    setState(() => _isLoading = true);

    try {
      final repo = ref.read(categoryRepositoryProvider);
      await Future.wait(
        _emails.map(
          (email) => repo.addMembersBulk(
            widget.categoryIds,
            email,
            _selectedLevel,
          ),
        ),
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              context.l10n.bulkShareSuccess(
                _emails.join(', '),
                widget.categoryCount,
              ),
            ),
          ),
        );
        ref.invalidate(categoryListProvider);
        Navigator.of(context).pop();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(context.l10n.errorWithMessage(e.toString()))),
        );
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;

    return Padding(
      padding: EdgeInsets.fromLTRB(
        24,
        24,
        24,
        MediaQuery.viewInsetsOf(context).bottom + 24,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            widget.categoryCount == 1
                ? l10n.shareCategory
                : l10n.shareAllSelected(widget.categoryCount),
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 8),
          Text(
            l10n.categoriesSelected(widget.categoryCount),
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(
                context,
              ).textTheme.bodySmall?.color?.withValues(alpha: 0.6),
            ),
          ),
          const SizedBox(height: 24),
          // ── Email chip input ──────────────────────────────────────
          if (_emails.isNotEmpty) ...[
            Wrap(
              spacing: 8,
              runSpacing: 4,
              children: _emails
                  .map(
                    (email) => Chip(
                      label: Text(
                        email,
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                      deleteIcon: const Icon(Icons.close, size: 16),
                      onDeleted: () => _removeEmail(email),
                      backgroundColor: cs.primaryContainer,
                      labelStyle: TextStyle(color: cs.onPrimaryContainer),
                      deleteIconColor: cs.onPrimaryContainer,
                      side: BorderSide.none,
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                    ),
                  )
                  .toList(),
            ),
            const SizedBox(height: 12),
          ],
          TextField(
            controller: _emailCtrl,
            decoration: InputDecoration(
              labelText: l10n.email,
              hintText: 'user@example.com',
              prefixIcon: const Icon(Icons.email_outlined),
              suffixIcon: IconButton(
                icon: const Icon(Icons.add_circle_outline),
                onPressed: _addEmail,
                tooltip: l10n.add,
              ),
            ),
            keyboardType: TextInputType.emailAddress,
            textInputAction: TextInputAction.done,
            onSubmitted: (_) => _addEmail(),
          ),
          const SizedBox(height: 16),
          DropdownButtonFormField<AccessLevel>(
            initialValue: _selectedLevel,
            decoration: InputDecoration(labelText: l10n.accessLevel),
            items: AccessLevel.values.map((level) {
              final label = switch (level) {
                AccessLevel.read => l10n.accessRead,
                AccessLevel.write => l10n.accessWrite,
                AccessLevel.edit => l10n.accessEdit,
                AccessLevel.admin => l10n.accessDelete,
              };
              final desc = switch (level) {
                AccessLevel.read => l10n.accessReadDesc,
                AccessLevel.write => l10n.accessWriteDesc,
                AccessLevel.edit => l10n.accessEditDesc,
                AccessLevel.admin => l10n.accessDeleteDesc,
              };
              return DropdownMenuItem(
                value: level,
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
          const SizedBox(height: 24),
          FilledButton(
            onPressed: _isLoading ? null : _submit,
            child: _isLoading
                ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation(Colors.white),
                    ),
                  )
                : Text(
                    _emails.isEmpty
                        ? l10n.addToCategories(widget.categoryCount)
                        : '${l10n.addToCategories(widget.categoryCount)} (${l10n.personCount(_emails.length)})',
                  ),
          ),
        ],
      ),
    );
  }
}
