import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart' hide TextDirection;
import '../../../../core/l10n/l10n.dart';
import '../../../../core/services/notification_routing.dart';
import '../../domain/entities/notification_item.dart';
import '../providers/notification_providers.dart';

class NotificationListScreen extends ConsumerWidget {
  const NotificationListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final listAsync = ref.watch(notificationListProvider);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.notifications)),
      body: listAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (items) => items.isEmpty
            ? Center(
                child: Text(
                  l10n.notificationsEmpty,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              )
            : RefreshIndicator.adaptive(
                onRefresh: () => ref.refresh(notificationListProvider.future),
                child: ListView.separated(
                  itemCount: items.length,
                  separatorBuilder: (_, _) => const Divider(height: 1),
                  itemBuilder: (context, i) =>
                      _NotificationTile(item: items[i]),
                ),
              ),
      ),
    );
  }
}

class _NotificationTile extends ConsumerWidget {
  const _NotificationTile({required this.item});
  final NotificationItem item;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cs = Theme.of(context).colorScheme;

    return ListTile(
      leading: CircleAvatar(
        backgroundColor: item.read
            ? cs.surfaceContainerHighest
            : cs.primaryContainer,
        child: Icon(
          _iconFor(item.type),
          size: 20,
          color: item.read ? cs.onSurfaceVariant : cs.primary,
        ),
      ),
      title: Text(
        item.title,
        style: TextStyle(
          fontWeight: item.read ? FontWeight.normal : FontWeight.w700,
        ),
      ),
      subtitle: Text(item.body),
      trailing: item.createdAt != null
          ? Text(
              DateFormat('d MMM, HH:mm').format(item.createdAt!),
              style: Theme.of(
                context,
              ).textTheme.labelSmall?.copyWith(color: cs.onSurfaceVariant),
            )
          : null,
      onTap: () {
        if (!item.read) {
          ref.read(notificationListProvider.notifier).markRead(item.id);
        }
        final target = notificationTarget(
          type: item.type,
          entityId: item.entityId,
        );
        if (target == null) return;
        if (target.isTab) {
          context.goNamed(target.name, pathParameters: target.pathParameters);
        } else {
          context.pushNamed(target.name, pathParameters: target.pathParameters);
        }
      },
    );
  }

  IconData _iconFor(String type) => switch (type) {
    'HOUSEHOLD_INVITE' => Icons.family_restroom,
    'DEBT_REMINDER' => Icons.notifications_active_outlined,
    'CATEGORY_MEMBER_ADDED' => Icons.group_add_outlined,
    'WEEKLY_DIGEST' => Icons.summarize_outlined,
    _ => Icons.notifications_outlined,
  };
}
