import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../shared/widgets/app_error_widget.dart';
import '../../../../shared/widgets/confirm_dialog.dart';
import '../../../../shared/providers/current_user_provider.dart';
import '../providers/category_providers.dart';
import '../../domain/entities/member_embedded.dart';
import '../widgets/add_category_sheet.dart';
import '../widgets/add_member_dialog.dart';

class CategoryDetailScreen extends ConsumerWidget {
  const CategoryDetailScreen({super.key, required this.id});
  final String id;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final categoryAsync = ref.watch(categoryDetailProvider(id));
    final currentUserAsync = ref.watch(currentUserIdProvider);
    final currentUserId = currentUserAsync.valueOrNull;

    return categoryAsync.when(
      loading: () =>
          const Scaffold(body: Center(child: CircularProgressIndicator())),
      error: (e, _) => Scaffold(
        appBar: AppBar(),
        body: AppErrorWidget(
          error: e,
          onRetry: () => ref.invalidate(categoryDetailProvider(id)),
        ),
      ),
      data: (category) {
        final isOwner = currentUserId == category.ownerId;

        // Group members by access level
        final grouped = <AccessLevel, List<MemberEmbedded>>{};
        for (final level in AccessLevel.values) {
          final members =
              category.members.where((m) => m.accessLevel == level).toList();
          if (members.isNotEmpty) grouped[level] = members;
        }

        return Scaffold(
          appBar: AppBar(
            title: Text(category.name),
            actions: [
              if (isOwner) ...[
                Semantics(
                  identifier: 'editCategoryBtn',
                  child: IconButton(
                    icon: const Icon(Icons.edit_outlined),
                    onPressed: () => showModalBottomSheet(
                      context: context,
                      isScrollControlled: true,
                      useSafeArea: true,
                      shape: const RoundedRectangleBorder(
                        borderRadius: BorderRadius.vertical(
                          top: Radius.circular(24),
                        ),
                      ),
                      builder: (_) => AddCategorySheet(
                        categoryId: id,
                        initialName: category.name,
                      ),
                    ),
                  ),
                ),
                Semantics(
                  identifier: 'deleteCategoryBtn',
                  child: IconButton(
                    icon: const Icon(Icons.delete_outline),
                    onPressed: () async {
                      final confirmed = await showConfirmDialog(
                        context,
                        title: l10n.deleteCategory,
                        content: l10n.deleteCategoryConfirm(category.name),
                      );
                      if (confirmed && context.mounted) {
                        await ref
                            .read(categoryListProvider.notifier)
                            .deleteCategory(id);
                        if (context.mounted) Navigator.of(context).pop();
                      }
                    },
                  ),
                ),
              ],
            ],
          ),
          floatingActionButton: isOwner
              ? FloatingActionButton.extended(
                  onPressed: () async {
                    final result =
                        await showDialog<
                          ({String email, AccessLevel accessLevel})
                        >(
                          context: context,
                          builder: (_) => const AddMemberDialog(),
                        );
                    if (result != null && context.mounted) {
                      await ref
                          .read(categoryDetailProvider(id).notifier)
                          .addMember(result.email, result.accessLevel);
                    }
                  },
                  icon: const Icon(Icons.person_add_outlined),
                  label: Text(l10n.addMember),
                )
              : null,
          body: category.members.isEmpty
              ? Center(child: Text(l10n.noMembersYet))
              : ListView(
                  padding: const EdgeInsets.only(bottom: 88),
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                      child: Text(
                        l10n.memberCountTitle(category.members.length),
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ),
                    for (final level in AccessLevel.values) ...[
                      if (grouped[level] != null) ...[
                        _LevelSection(
                          level: level,
                          members: grouped[level]!,
                          isOwner: isOwner,
                          onRemove: (member) async {
                            final confirmed = await showConfirmDialog(
                              context,
                              title: l10n.removeMember,
                              content: l10n.removeMemberConfirm,
                              confirmText: l10n.remove,
                            );
                            if (confirmed && context.mounted) {
                              await ref
                                  .read(categoryDetailProvider(id).notifier)
                                  .removeMember(member.userId ?? member.email ?? '');
                            }
                          },
                        ),
                      ],
                    ],
                  ],
                ),
        );
      },
    );
  }
}

class _LevelSection extends StatelessWidget {
  const _LevelSection({
    required this.level,
    required this.members,
    required this.isOwner,
    required this.onRemove,
  });

  final AccessLevel level;
  final List<MemberEmbedded> members;
  final bool isOwner;
  final void Function(MemberEmbedded) onRemove;

  Color _levelColor() => switch (level) {
    AccessLevel.read => AppColors.accessRead,
    AccessLevel.write => AppColors.accessWrite,
    AccessLevel.edit => AppColors.accessEdit,
    AccessLevel.admin => AppColors.accessDelete,
  };

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    final color = _levelColor();

    final levelName = switch (level) {
      AccessLevel.read => l10n.accessRead,
      AccessLevel.write => l10n.accessWrite,
      AccessLevel.edit => l10n.accessEdit,
      AccessLevel.admin => l10n.accessDelete,
    };
    final levelDesc = switch (level) {
      AccessLevel.read => l10n.accessReadDesc,
      AccessLevel.write => l10n.accessWriteDesc,
      AccessLevel.edit => l10n.accessEditDesc,
      AccessLevel.admin => l10n.accessDeleteDesc,
    };

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          margin: const EdgeInsets.fromLTRB(16, 12, 16, 4),
          padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.08),
            borderRadius: BorderRadius.circular(12),
            border: Border(left: BorderSide(color: color, width: 3)),
          ),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      levelName.toUpperCase(),
                      style: TextStyle(
                        color: color,
                        fontWeight: FontWeight.w700,
                        fontSize: 12,
                        letterSpacing: 0.8,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      levelDesc,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: cs.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  '${members.length}',
                  style: TextStyle(
                    color: color,
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                  ),
                ),
              ),
            ],
          ),
        ),
        for (final member in members)
          ListTile(
            contentPadding: const EdgeInsets.fromLTRB(28, 0, 8, 0),
            leading: CircleAvatar(
              radius: 18,
              backgroundColor: color.withValues(alpha: 0.12),
              child: Text(
                (member.email ?? member.userId ?? '').isNotEmpty
                    ? (member.email ?? member.userId ?? '')[0].toUpperCase()
                    : '?',
                style: TextStyle(
                  color: color,
                  fontWeight: FontWeight.w600,
                  fontSize: 14,
                ),
              ),
            ),
            title: Text(
              member.email ?? member.userId ?? '',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            trailing: isOwner
                ? Semantics(
                    identifier: 'removeMemberBtn',
                    // container: true şart — ListTile satırı semantics'i
                    // birleştirdiğinden identifier, butonun kendi node'u yerine
                    // tüm satırı kaplayan birleşik node'a taşınıyor ve
                    // dokunuş satırın ortasına, boşluğa düşüyordu.
                    container: true,
                    child: IconButton(
                      icon: Icon(
                        Icons.person_remove_outlined,
                        color: cs.error,
                        size: 20,
                      ),
                      // Tooltip olmadan buton a11y ağacında label'sız kalıyordu:
                      // ikon tek başına anlam taşımıyor, ekran okuyucu boş bir
                      // düğme okuyordu.
                      tooltip: l10n.removeMember,
                      onPressed: () => onRemove(member),
                    ),
                  )
                : null,
          ),
        const Divider(height: 1, indent: 16, endIndent: 16),
      ],
    );
  }
}
