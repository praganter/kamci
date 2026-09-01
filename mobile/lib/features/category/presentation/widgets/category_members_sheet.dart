import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../shared/widgets/confirm_dialog.dart';
import '../../../../shared/widgets/premium_required_sheet.dart';
import '../../domain/entities/category.dart';
import '../../domain/entities/member_embedded.dart';
import '../providers/category_providers.dart';
import 'access_level_badge.dart';
import 'add_member_dialog.dart';

class CategoryMembersSheet extends ConsumerWidget {
  const CategoryMembersSheet({super.key, required this.category});

  final Category category;

  static Future<void> show(
    BuildContext context, {
    required Category category,
  }) => showModalBottomSheet(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (context) => CategoryMembersSheet(category: category),
  );

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final categoryAsync = ref.watch(categoryDetailProvider(category.id));
    final liveCategory = categoryAsync.valueOrNull ?? category;

    return DraggableScrollableSheet(
      initialChildSize: 0.5,
      maxChildSize: 0.9,
      minChildSize: 0.3,
      builder: (context, scrollController) => Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(24, 16, 24, 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  liveCategory.name,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 4),
                Text(
                  l10n.memberCountTitle(liveCategory.members.length),
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Theme.of(
                      context,
                    ).textTheme.bodySmall?.color?.withValues(alpha: 0.6),
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: liveCategory.members.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.people_outline,
                          size: 48,
                          color: Theme.of(
                            context,
                          ).textTheme.bodySmall?.color?.withValues(alpha: 0.4),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          l10n.noMembersYet,
                          style: Theme.of(context).textTheme.bodyMedium
                              ?.copyWith(
                                color: Theme.of(context)
                                    .textTheme
                                    .bodySmall
                                    ?.color
                                    ?.withValues(alpha: 0.6),
                              ),
                        ),
                      ],
                    ),
                  )
                : ListView.builder(
                    controller: scrollController,
                    itemCount: liveCategory.members.length,
                    itemBuilder: (context, index) => _MemberListTile(
                      member: liveCategory.members[index],
                      categoryId: liveCategory.id,
                      onRemove: () => _removeMember(
                        context,
                        ref,
                        liveCategory.id,
                        liveCategory.members[index],
                      ),
                    ),
                  ),
          ),
          Padding(
            padding: EdgeInsets.fromLTRB(
              24,
              16,
              24,
              MediaQuery.viewInsetsOf(context).bottom + 24,
            ),
            child: FilledButton(
              onPressed: () => _showAddMemberDialog(context, ref, category.id),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.add, size: 20),
                  const SizedBox(width: 8),
                  Text(l10n.addMember),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _showAddMemberDialog(
    BuildContext context,
    WidgetRef ref,
    String categoryId,
  ) async {
    final result = await showDialog<({String email, AccessLevel accessLevel})>(
      context: context,
      builder: (context) => const AddMemberDialog(),
    );

    if (result == null) return;

    try {
      await ref
          .read(categoryDetailProvider(categoryId).notifier)
          .addMember(result.email, result.accessLevel);

      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(context.l10n.memberAddedSuccess(result.email)),
          ),
        );
      }
    } on PremiumLimitException catch (e) {
      // Backend gated this on premium (error.category.premium_required) —
      // show the upsell sheet instead of a plain error snackbar.
      if (context.mounted) {
        PremiumRequiredSheet.show(
          context,
          featureName: context.l10n.shareCategory.toLowerCase(),
          limitCount: 0,
          message: e.message,
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(context.l10n.errorWithMessage(e.toString()))),
        );
      }
    }
  }

  Future<void> _removeMember(
    BuildContext context,
    WidgetRef ref,
    String categoryId,
    MemberEmbedded member,
  ) async {
    final confirmed = await showConfirmDialog(
      context,
      title: context.l10n.removeMember,
      content: context.l10n.removeMemberConfirm,
      confirmText: context.l10n.delete,
    );

    if (!confirmed) return;

    try {
      await ref
          .read(categoryDetailProvider(categoryId).notifier)
          .removeMember(member.userId ?? member.email ?? '');

      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(context.l10n.memberRemovedSuccess)),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(context.l10n.errorWithMessage(e.toString()))),
        );
      }
    }
  }
}

class _MemberListTile extends StatelessWidget {
  const _MemberListTile({
    required this.member,
    required this.categoryId,
    required this.onRemove,
  });

  final MemberEmbedded member;
  final String categoryId;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final displayName = member.email ?? member.userId ?? '';
    final initials = displayName.isNotEmpty
        ? displayName[0].toUpperCase()
        : '?';

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        children: [
          CircleAvatar(
            radius: 20,
            child: Text(
              initials,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  displayName,
                  style: Theme.of(context).textTheme.bodyMedium,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                AccessLevelBadge(level: member.accessLevel),
              ],
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            icon: const Icon(Icons.person_remove, size: 20),
            onPressed: onRemove,
            tooltip: context.l10n.removeMember,
          ),
        ],
      ),
    );
  }
}
