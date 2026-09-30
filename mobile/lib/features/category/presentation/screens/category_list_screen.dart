import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/router/route_names.dart';
import '../../../../shared/providers/current_user_provider.dart';
import '../../../../shared/widgets/app_error_widget.dart';
import '../../../../shared/widgets/confirm_dialog.dart';
import '../../../../shared/widgets/premium_required_sheet.dart';
import '../../../subscription/presentation/providers/subscription_providers.dart';
import '../../../subscription/subscription_constants.dart';
import '../../domain/entities/category.dart';
import '../providers/category_providers.dart';
import '../../domain/entities/member_embedded.dart';

import '../widgets/add_category_sheet.dart';
import '../widgets/bulk_share_sheet.dart';
import '../widgets/category_members_sheet.dart';

class CategoryListScreen extends ConsumerStatefulWidget {
  const CategoryListScreen({super.key});

  @override
  ConsumerState<CategoryListScreen> createState() => _CategoryListScreenState();
}

class _CategoryListScreenState extends ConsumerState<CategoryListScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _tabController.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _openAddSheet() async {
    final isPremium = ref.read(isPremiumProvider);
    if (!isPremium) {
      final categories = ref.read(categoryListProvider).valueOrNull ?? [];
      final currentUserId = ref.read(currentUserIdProvider).valueOrNull;
      final ownedCategories = categories
          .where((c) => c.ownerId == currentUserId)
          .length;
      if (ownedCategories >= SubscriptionConstants.freeCategoryLimit) {
        if (mounted) {
          PremiumRequiredSheet.show(
            context,
            featureName: context.l10n.categories.toLowerCase(),
            limitCount: SubscriptionConstants.freeCategoryLimit,
          );
        }
        return;
      }
    }
    if (!mounted) return;
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => const AddCategorySheet(),
    );
  }

  void _openSharePicker() {
    _CategorySharePickerSheet.show(context);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    final isMyTab = _tabController.index == 0;

    return Scaffold(
      appBar: AppBar(
        toolbarHeight: 0,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(56),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
            child: Container(
              height: 40,
              decoration: BoxDecoration(
                color: cs.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(20),
              ),
              child: TabBar(
                controller: _tabController,
                indicatorSize: TabBarIndicatorSize.tab,
                indicator: BoxDecoration(
                  color: cs.primaryContainer,
                  borderRadius: BorderRadius.circular(20),
                ),
                dividerColor: Colors.transparent,
                labelColor: cs.primary,
                unselectedLabelColor: cs.onSurfaceVariant,
                labelStyle: const TextStyle(fontWeight: FontWeight.w600),
                overlayColor: WidgetStateProperty.all(Colors.transparent),
                tabs: [
                  Tab(text: l10n.myCategories),
                  Tab(text: l10n.joinedCategories),
                ],
              ),
            ),
          ),
        ),
      ),
      floatingActionButton: isMyTab
          ? Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                FloatingActionButton.small(
                  heroTag: 'share_categories',
                  onPressed: _openSharePicker,
                  tooltip: l10n.bulkShare,
                  child: const Icon(Icons.share_outlined),
                ),
                const SizedBox(height: 8),
                Semantics(
                  container: true,
                  identifier: 'addCategoryFab',
                  child: FloatingActionButton(
                    heroTag: 'add_category',
                    onPressed: _openAddSheet,
                    child: const Icon(Icons.add),
                  ),
                ),
              ],
            )
          : null,
      body: TabBarView(
        controller: _tabController,
        children: const [
          _CategoryTabView(isOwner: true),
          _CategoryTabView(isOwner: false),
        ],
      ),
    );
  }
}

class _CategoryTabView extends ConsumerStatefulWidget {
  const _CategoryTabView({required this.isOwner});

  final bool isOwner;

  @override
  ConsumerState<_CategoryTabView> createState() => _CategoryTabViewState();
}

class _CategoryTabViewState extends ConsumerState<_CategoryTabView> {
  bool _selectionMode = false;
  final Set<String> _selectedIds = {};

  void _toggleSelection(String categoryId) {
    setState(() {
      if (_selectedIds.contains(categoryId)) {
        _selectedIds.remove(categoryId);
        if (_selectedIds.isEmpty) {
          _selectionMode = false;
        }
      } else {
        _selectedIds.add(categoryId);
        _selectionMode = true;
      }
    });
  }

  void _clearSelection() {
    setState(() {
      _selectedIds.clear();
      _selectionMode = false;
    });
  }

  void _showBulkShareSheet() {
    BulkShareSheet.show(context, categoryIds: _selectedIds.toList());
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final categoriesAsync = ref.watch(categoryListProvider);
    final currentUserAsync = ref.watch(currentUserIdProvider);
    debugPrint('🟡 _CategoryTabView(isOwner=${widget.isOwner}) build: cats=${categoriesAsync.runtimeType}, userId=${currentUserAsync.valueOrNull}');

    return categoriesAsync.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => AppErrorWidget(
        error: e,
        onRetry: () => ref.invalidate(categoryListProvider),
      ),
      data: (categories) {
        final accessLevels = ref
            .watch(categoryAccessLevelsProvider)
            .valueOrNull;
        final currentUserId = currentUserAsync.valueOrNull;
        final filtered = categories.where((c) {
          final owns = c.ownerId == currentUserId;
          return widget.isOwner ? owns : !owns;
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
                    widget.isOwner
                        ? Icons.folder_open_outlined
                        : Icons.group_outlined,
                    size: 32,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  widget.isOwner
                      ? l10n.noCategoriesYet
                      : l10n.noJoinedCategories,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 6),
                Text(
                  widget.isOwner
                      ? l10n.addCategoryHint
                      : l10n.waitingInvitation,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          );
        }

        return Stack(
          children: [
            RefreshIndicator(
              onRefresh: () => ref.refresh(categoryListProvider.future),
              child: ListView.builder(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 80),
                itemCount: filtered.length,
                itemBuilder: (context, index) {
                  final category = filtered[index];
                  final level = accessLevels?[category.id];

                  return _CategoryCard(
                    category: category,
                    isOwner: widget.isOwner,
                    isSelected: _selectedIds.contains(category.id),
                    selectionMode: _selectionMode,
                    onLongPress: widget.isOwner
                        ? () => _toggleSelection(category.id)
                        : null,
                    onSelectionChanged: widget.isOwner
                        ? (selected) {
                            if (selected ?? false) {
                              _toggleSelection(category.id);
                            }
                          }
                        : null,
                    onTap: _selectionMode
                        ? () => _toggleSelection(category.id)
                        : () => context.pushNamed(
                            RouteNames.categoryDetail,
                            pathParameters: {'id': category.id},
                          ),
                    onEdit: !_selectionMode &&
                            (category.ownerId == currentUserId ||
                                (level != null && level.canEdit))
                        ? () {
                            showModalBottomSheet(
                              context: context,
                              isScrollControlled: true,
                              useSafeArea: true,
                              shape: const RoundedRectangleBorder(
                                borderRadius: BorderRadius.vertical(
                                  top: Radius.circular(24),
                                ),
                              ),
                              builder: (_) => AddCategorySheet(
                                categoryId: category.id,
                                initialName: category.name,
                              ),
                            );
                          }
                        : null,
                    onDelete: !_selectionMode &&
                            (category.ownerId == currentUserId ||
                                (level != null && level.canDelete))
                        ? () async {
                            final confirmed = await showConfirmDialog(
                              context,
                              title: l10n.deleteCategory,
                              content: l10n.deleteCategoryConfirm(
                                category.name,
                              ),
                            );
                            if (confirmed && context.mounted) {
                              try {
                                await ref
                                    .read(categoryListProvider.notifier)
                                    .deleteCategory(category.id);
                              } catch (e) {
                                if (context.mounted) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(
                                      content: Text(
                                        context.l10n.errorWithMessage(
                                          e.toString(),
                                        ),
                                      ),
                                    ),
                                  );
                                }
                              }
                            }
                          }
                        : null,
                    onManageMembers: !_selectionMode &&
                            (category.ownerId == currentUserId ||
                                (level != null && level.canEdit))
                        ? () {
                            CategoryMembersSheet.show(
                              context,
                              category: category,
                            );
                          }
                        : null,
                    onLeaveCategory: !widget.isOwner && !_selectionMode
                        ? () async {
                            final confirmed = await showConfirmDialog(
                              context,
                              title: l10n.leaveCategory,
                              content: l10n.leaveCategoryConfirm(category.name),
                            );
                            if (confirmed && context.mounted) {
                              final currentUser = ref
                                  .read(currentUserIdProvider)
                                  .valueOrNull;
                              if (currentUser != null) {
                                try {
                                  await ref
                                      .read(
                                        categoryDetailProvider(
                                          category.id,
                                        ).notifier,
                                      )
                                      .removeMember(currentUser);
                                  if (context.mounted) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(
                                        content: Text(l10n.memberLeftCategory),
                                      ),
                                    );
                                  }
                                } catch (e) {
                                  if (context.mounted) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(
                                        content: Text(
                                          context.l10n.errorWithMessage(
                                            e.toString(),
                                          ),
                                        ),
                                      ),
                                    );
                                  }
                                }
                              }
                            }
                          }
                        : null,
                  );
                },
              ),
            ),
            if (_selectionMode)
              Positioned(
                bottom: 0,
                left: 0,
                right: 0,
                child: _BulkActionBar(
                  count: _selectedIds.length,
                  onClear: _clearSelection,
                  onShare: _showBulkShareSheet,
                ),
              ),
          ],
        );
      },
    );
  }
}

class _CategoryCard extends StatelessWidget {
  const _CategoryCard({
    required this.category,
    required this.isOwner,
    required this.isSelected,
    required this.selectionMode,
    required this.onTap,
    this.onLongPress,
    this.onSelectionChanged,
    this.onEdit,
    this.onDelete,
    this.onManageMembers,
    this.onLeaveCategory,
  });

  final Category category;
  final bool isOwner;
  final bool isSelected;
  final bool selectionMode;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;
  final ValueChanged<bool?>? onSelectionChanged;
  final VoidCallback? onEdit;
  final VoidCallback? onDelete;
  final VoidCallback? onManageMembers;
  final VoidCallback? onLeaveCategory;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      decoration: BoxDecoration(
        color: cs.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isSelected ? cs.primary : cs.outlineVariant,
          width: isSelected ? 2 : 1,
        ),
      ),
      child: Stack(
        children: [
          InkWell(
            onTap: onTap,
            onLongPress: onLongPress,
            borderRadius: BorderRadius.circular(16),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
              child: Row(
                children: [
                  Container(
                    width: 42,
                    height: 42,
                    decoration: BoxDecoration(
                      color: cs.primary,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(
                      isOwner ? Icons.folder_rounded : Icons.group_rounded,
                      size: 20,
                      color: cs.onPrimary,
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          category.name,
                          style: Theme.of(context).textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.w600),
                        ),
                        const SizedBox(height: 2),
                        ExcludeSemantics(
                          child: Text(
                            l10n.memberCount(category.members.length),
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (selectionMode)
                    Checkbox(
                      value: isSelected,
                      onChanged: (val) => onSelectionChanged?.call(val),
                    )
                  else if (onEdit != null ||
                      onDelete != null ||
                      onManageMembers != null ||
                      onLeaveCategory != null)
                    const SizedBox(width: 40)
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
          if (!selectionMode &&
              (onEdit != null ||
                  onDelete != null ||
                  onManageMembers != null ||
                  onLeaveCategory != null))
            Positioned(
              right: 4,
              top: 0,
              bottom: 0,
              child: Align(
                alignment: Alignment.center,
                child: Semantics(
                  container: true,
                  identifier: 'categoryMoreBtn',
                  child: Builder(
                    builder: (ctx) => IconButton(
                      icon: Icon(
                        Icons.more_vert,
                        size: 18,
                        color: cs.onSurfaceVariant,
                      ),
                      iconSize: 18,
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(
                        minWidth: 40,
                        minHeight: 40,
                      ),
                      onPressed: () async {
                        final box =
                            ctx.findRenderObject()! as RenderBox;
                        final overlay = Overlay.of(ctx)
                            .context
                            .findRenderObject()! as RenderBox;
                        final position = RelativeRect.fromRect(
                          box.localToGlobal(
                                Offset.zero,
                                ancestor: overlay,
                              ) &
                              box.size,
                          Offset.zero & overlay.size,
                        );
                        final result = await showMenu<String>(
                          context: ctx,
                          position: position,
                          items: [
                            if (onManageMembers != null)
                              PopupMenuItem(
                                value: 'manage',
                                child: Text(l10n.manageMembers),
                              ),
                            if (onEdit != null)
                              PopupMenuItem(
                                value: 'edit',
                                child: Text(l10n.edit),
                              ),
                            if (onDelete != null)
                              PopupMenuItem(
                                value: 'delete',
                                child: Text(
                                  l10n.delete,
                                  style: const TextStyle(
                                    color: Colors.red,
                                  ),
                                ),
                              ),
                            if (onLeaveCategory != null)
                              PopupMenuItem(
                                value: 'leave',
                                child: Text(
                                  l10n.leaveCategory,
                                  style: const TextStyle(
                                    color: Colors.red,
                                  ),
                                ),
                              ),
                          ],
                        );
                        if (result == 'manage') onManageMembers?.call();
                        if (result == 'edit') onEdit?.call();
                        if (result == 'delete') onDelete?.call();
                        if (result == 'leave') onLeaveCategory?.call();
                      },
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _BulkActionBar extends StatelessWidget {
  const _BulkActionBar({
    required this.count,
    required this.onClear,
    required this.onShare,
  });

  final int count;
  final VoidCallback onClear;
  final VoidCallback onShare;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;

    return Container(
      decoration: BoxDecoration(
        color: cs.surface,
        border: Border(top: BorderSide(color: cs.outlineVariant)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          TextButton.icon(
            onPressed: onClear,
            icon: const Icon(Icons.clear),
            label: Text(l10n.clearSelection),
          ),
          FilledButton.icon(
            onPressed: onShare,
            icon: const Icon(Icons.share_outlined),
            label: Text(
              count == 1 ? l10n.shareCategory : l10n.shareAllSelected(count),
            ),
          ),
        ],
      ),
    );
  }
}

// ── Category Share Picker Sheet ───────────────────────────────────────────────

class _CategorySharePickerSheet extends ConsumerStatefulWidget {
  const _CategorySharePickerSheet();

  static Future<void> show(BuildContext context) => showModalBottomSheet(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (_) => const _CategorySharePickerSheet(),
  );

  @override
  ConsumerState<_CategorySharePickerSheet> createState() =>
      _CategorySharePickerSheetState();
}

class _CategorySharePickerSheetState
    extends ConsumerState<_CategorySharePickerSheet> {
  final Set<String> _selectedIds = {};

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    final categoriesAsync = ref.watch(categoryListProvider);
    final currentUserId = ref.watch(currentUserIdProvider).valueOrNull;

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
            l10n.selectCategoriesToShare,
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 4),
          Text(
            _selectedIds.isEmpty
                ? l10n.categoriesSelected(0)
                : l10n.categoriesSelected(_selectedIds.length),
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: _selectedIds.isEmpty
                  ? cs.onSurfaceVariant.withValues(alpha: 0.6)
                  : cs.primary,
            ),
          ),
          const SizedBox(height: 16),
          categoriesAsync.when(
            loading: () => const Padding(
              padding: EdgeInsets.symmetric(vertical: 32),
              child: Center(child: CircularProgressIndicator()),
            ),
            error: (e, _) => Text(e.toString()),
            data: (categories) {
              final owned = categories
                  .where((c) => c.ownerId == currentUserId)
                  .toList();
              if (owned.isEmpty) {
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 32),
                  child: Center(child: Text(l10n.noCategoriesYet)),
                );
              }
              return ConstrainedBox(
                constraints: BoxConstraints(
                  maxHeight: MediaQuery.of(context).size.height * 0.5,
                ),
                child: ListView.builder(
                  shrinkWrap: true,
                  itemCount: owned.length,
                  itemBuilder: (context, i) {
                    final cat = owned[i];
                    final isSelected = _selectedIds.contains(cat.id);
                    return CheckboxListTile(
                      value: isSelected,
                      onChanged: (val) => setState(() {
                        if (val == true) {
                          _selectedIds.add(cat.id);
                        } else {
                          _selectedIds.remove(cat.id);
                        }
                      }),
                      title: Text(
                        cat.name,
                        style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      subtitle: Text(l10n.memberCount(cat.members.length)),
                      secondary: Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: isSelected ? cs.primary : cs.primaryContainer,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Icon(
                          Icons.folder_rounded,
                          size: 20,
                          color: isSelected
                              ? cs.onPrimary
                              : cs.onPrimaryContainer,
                        ),
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 4,
                        vertical: 2,
                      ),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    );
                  },
                ),
              );
            },
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: _selectedIds.isEmpty
                ? null
                : () {
                    final ids = _selectedIds.toList();
                    Navigator.of(context).pop();
                    BulkShareSheet.show(context, categoryIds: ids);
                  },
            icon: const Icon(Icons.share_outlined, size: 18),
            label: Text(
              _selectedIds.isEmpty
                  ? l10n.selectCategoriesToShare
                  : _selectedIds.length == 1
                  ? l10n.shareCategory
                  : l10n.shareAllSelected(_selectedIds.length),
            ),
          ),
        ],
      ),
    );
  }
}
