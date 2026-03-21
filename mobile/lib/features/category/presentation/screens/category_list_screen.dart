import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/router/route_names.dart';
import '../../../../shared/providers/current_user_provider.dart';
import '../../../../shared/widgets/app_error_widget.dart';
import '../../../../shared/widgets/confirm_dialog.dart';
import '../../domain/entities/category.dart';
import '../providers/category_providers.dart';

class CategoryListScreen extends ConsumerWidget {
  const CategoryListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: Text(l10n.categories),
          bottom: TabBar(
            tabs: [
              Tab(text: l10n.myCategories),
              Tab(text: l10n.joinedCategories),
            ],
          ),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () => context.pushNamed(RouteNames.categoryNew),
          child: const Icon(Icons.add),
        ),
        body: const TabBarView(
          children: [
            _CategoryTabView(isOwner: true),
            _CategoryTabView(isOwner: false),
          ],
        ),
      ),
    );
  }
}

class _CategoryTabView extends ConsumerWidget {
  const _CategoryTabView({required this.isOwner});

  final bool isOwner;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final categoriesAsync = ref.watch(categoryListProvider);
    final currentUserAsync = ref.watch(currentUserIdProvider);

    return categoriesAsync.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => AppErrorWidget(
        message: e.toString(),
        onRetry: () => ref.invalidate(categoryListProvider),
      ),
      data: (categories) {
        final currentUserId = currentUserAsync.valueOrNull;
        final filtered = categories.where((c) {
          final owns = c.ownerId == currentUserId;
          return isOwner ? owns : !owns;
        }).toList();

        if (filtered.isEmpty) {
          return Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.primaryContainer,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    isOwner
                        ? Icons.folder_open_outlined
                        : Icons.group_outlined,
                    size: 32,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  isOwner ? l10n.noCategoriesYet : l10n.noJoinedCategories,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 6),
                Text(
                  isOwner ? l10n.addCategoryHint : l10n.waitingInvitation,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          );
        }

        return RefreshIndicator(
          onRefresh: () => ref.refresh(categoryListProvider.future),
          child: ListView.builder(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 80),
            itemCount: filtered.length,
            itemBuilder: (context, index) {
              final category = filtered[index];
              return _CategoryCard(
                category: category,
                isOwner: isOwner,
                onTap: () => context.pushNamed(
                  RouteNames.categoryDetail,
                  pathParameters: {'id': category.id},
                ),
                onEdit: isOwner
                    ? () => context.pushNamed(
                          RouteNames.categoryEdit,
                          pathParameters: {'id': category.id},
                        )
                    : null,
                onDelete: isOwner
                    ? () async {
                        final confirmed = await showConfirmDialog(
                          context,
                          title: l10n.deleteCategory,
                          content: l10n.deleteCategoryConfirm(category.name),
                        );
                        if (confirmed && context.mounted) {
                          await ref
                              .read(categoryListProvider.notifier)
                              .deleteCategory(category.id);
                        }
                      }
                    : null,
              );
            },
          ),
        );
      },
    );
  }
}

class _CategoryCard extends StatelessWidget {
  const _CategoryCard({
    required this.category,
    required this.isOwner,
    required this.onTap,
    this.onEdit,
    this.onDelete,
  });

  final Category category;
  final bool isOwner;
  final VoidCallback onTap;
  final VoidCallback? onEdit;
  final VoidCallback? onDelete;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      decoration: BoxDecoration(
        color: cs.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: cs.outlineVariant),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: cs.primaryContainer,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  isOwner ? Icons.folder_rounded : Icons.group_rounded,
                  size: 20,
                  color: cs.primary,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      category.name,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      l10n.memberCount(category.members.length),
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              if (onEdit != null || onDelete != null)
                PopupMenuButton(
                  icon: Icon(Icons.more_vert,
                      size: 18, color: cs.onSurfaceVariant),
                  itemBuilder: (_) => [
                    if (onEdit != null)
                      PopupMenuItem(value: 'edit', child: Text(l10n.edit)),
                    if (onDelete != null)
                      PopupMenuItem(
                        value: 'delete',
                        child: Text(l10n.delete,
                            style: const TextStyle(color: Colors.red)),
                      ),
                  ],
                  onSelected: (value) {
                    if (value == 'edit') onEdit?.call();
                    if (value == 'delete') onDelete?.call();
                  },
                )
              else
                Icon(
                  Icons.chevron_right_rounded,
                  color: cs.onSurfaceVariant,
                  size: 20,
                ),
            ],
          ),
        ),
      ),
    );
  }
}
