import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../shared/widgets/app_error_widget.dart';
import '../../../../shared/widgets/confirm_dialog.dart';
import '../../../../shared/providers/current_user_provider.dart';
import '../providers/category_providers.dart';
import '../widgets/access_level_badge.dart';
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
      loading: () => const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => Scaffold(
        appBar: AppBar(),
        body: AppErrorWidget(
          message: e.toString(),
          onRetry: () => ref.invalidate(categoryDetailProvider(id)),
        ),
      ),
      data: (category) {
        final isOwner = currentUserId == category.ownerId;

        return Scaffold(
          appBar: AppBar(
            title: Text(category.name),
            actions: [
              if (isOwner)
                IconButton(
                  icon: const Icon(Icons.edit_outlined),
                  onPressed: () => showModalBottomSheet(
                    context: context,
                    isScrollControlled: true,
                    useSafeArea: true,
                    shape: const RoundedRectangleBorder(
                      borderRadius:
                          BorderRadius.vertical(top: Radius.circular(24)),
                    ),
                    builder: (_) => AddCategorySheet(
                      categoryId: id,
                      initialName: category.name,
                    ),
                  ),
                ),
            ],
          ),
          floatingActionButton: isOwner
              ? FloatingActionButton.extended(
                  onPressed: () async {
                    final result = await showDialog<
                        ({String userId, dynamic accessLevel})>(
                      context: context,
                      builder: (_) => const AddMemberDialog(),
                    );
                    if (result != null && context.mounted) {
                      await ref
                          .read(categoryDetailProvider(id).notifier)
                          .addMember(result.userId, result.accessLevel);
                    }
                  },
                  icon: const Icon(Icons.person_add_outlined),
                  label: Text(l10n.addMember),
                )
              : null,
          body: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.all(16),
                child: Text(
                  l10n.memberCountTitle(category.members.length),
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              Expanded(
                child: category.members.isEmpty
                    ? Center(child: Text(l10n.noMembersYet))
                    : ListView.builder(
                        itemCount: category.members.length,
                        itemBuilder: (context, index) {
                          final member = category.members[index];
                          return ListTile(
                            leading: CircleAvatar(
                              backgroundColor: Theme.of(context)
                                  .colorScheme
                                  .primaryContainer,
                              child: Icon(
                                Icons.person_outline,
                                color: Theme.of(context).colorScheme.primary,
                                size: 20,
                              ),
                            ),
                            title: Text(member.userId),
                            trailing: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                AccessLevelBadge(level: member.accessLevel),
                                if (isOwner) ...[
                                  const SizedBox(width: 8),
                                  IconButton(
                                    icon: Icon(
                                      Icons.remove_circle_outline,
                                      color:
                                          Theme.of(context).colorScheme.error,
                                    ),
                                    onPressed: () async {
                                      final confirmed = await showConfirmDialog(
                                        context,
                                        title: l10n.removeMember,
                                        content: l10n.removeMemberConfirm,
                                        confirmText: l10n.remove,
                                      );
                                      if (confirmed && context.mounted) {
                                        await ref
                                            .read(categoryDetailProvider(id)
                                                .notifier)
                                            .removeMember(member.userId);
                                      }
                                    },
                                  ),
                                ],
                              ],
                            ),
                          );
                        },
                      ),
              ),
            ],
          ),
        );
      },
    );
  }
}
