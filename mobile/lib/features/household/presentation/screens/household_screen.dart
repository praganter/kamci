import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/utils/snackbar_utils.dart';
import '../../../../shared/widgets/confirm_dialog.dart';
import '../../domain/entities/household.dart';
import '../providers/household_providers.dart';

class HouseholdScreen extends ConsumerWidget {
  const HouseholdScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final householdAsync = ref.watch(householdCurrentProvider);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.household)),
      body: householdAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (household) => household == null
            ? _NoHouseholdView(onInvited: () {})
            : _HouseholdView(household: household),
      ),
    );
  }
}

class _NoHouseholdView extends ConsumerStatefulWidget {
  const _NoHouseholdView({required this.onInvited});
  final VoidCallback onInvited;

  @override
  ConsumerState<_NoHouseholdView> createState() => _NoHouseholdViewState();
}

class _NoHouseholdViewState extends ConsumerState<_NoHouseholdView> {
  final _emailController = TextEditingController();
  bool _isSubmitting = false;

  @override
  void dispose() {
    _emailController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final email = _emailController.text.trim();
    if (email.isEmpty) return;
    setState(() => _isSubmitting = true);
    try {
      await ref.read(householdCurrentProvider.notifier).invite(email);
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(context.l10n.householdInviteSent)));
      }
    } catch (e) {
      if (mounted) showErrorSnackBar(context, e);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;

    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        Icon(Icons.family_restroom, size: 56, color: cs.primary),
        const SizedBox(height: 16),
        Text(
          l10n.householdNoHousehold,
          style: Theme.of(context).textTheme.titleMedium,
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          l10n.householdDesc,
          style: Theme.of(
            context,
          ).textTheme.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 24),
        Text(
          l10n.householdInviteTitle,
          style: Theme.of(context).textTheme.titleSmall,
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _emailController,
          keyboardType: TextInputType.emailAddress,
          decoration: InputDecoration(
            labelText: l10n.householdInviteEmailLabel,
            border: const OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 16),
        FilledButton(
          onPressed: _isSubmitting ? null : _submit,
          child: _isSubmitting
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Text(l10n.householdInviteButton),
        ),
      ],
    );
  }
}

class _HouseholdView extends ConsumerWidget {
  const _HouseholdView({required this.household});
  final Household household;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    final partnerEmail = household.partnerEmailForCurrentUser;

    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        Icon(Icons.family_restroom, size: 56, color: cs.primary),
        const SizedBox(height: 16),
        if (household.status == HouseholdStatus.accepted) ...[
          Text(
            l10n.householdActiveWith(partnerEmail),
            style: Theme.of(context).textTheme.titleMedium,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 24),
          OutlinedButton(
            onPressed: () => _leave(context, ref),
            style: OutlinedButton.styleFrom(foregroundColor: cs.error),
            child: Text(l10n.householdLeave),
          ),
        ] else ...[
          Text(
            household.isOwner
                ? l10n.householdPendingAsOwner(partnerEmail)
                : l10n.householdPendingAsPartner(partnerEmail),
            style: Theme.of(context).textTheme.titleMedium,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 24),
          if (!household.isOwner) ...[
            Row(
              children: [
                Expanded(
                  child: FilledButton(
                    onPressed: () => _accept(context, ref),
                    child: Text(l10n.householdAccept),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => _decline(context, ref),
                    style: OutlinedButton.styleFrom(foregroundColor: cs.error),
                    child: Text(l10n.householdDecline),
                  ),
                ),
              ],
            ),
          ] else
            OutlinedButton(
              onPressed: () => _leave(context, ref),
              style: OutlinedButton.styleFrom(foregroundColor: cs.error),
              child: Text(l10n.householdLeave),
            ),
        ],
      ],
    );
  }

  Future<void> _accept(BuildContext context, WidgetRef ref) async {
    try {
      await ref.read(householdCurrentProvider.notifier).accept(household.id);
    } catch (e) {
      if (context.mounted) showErrorSnackBar(context, e);
    }
  }

  Future<void> _decline(BuildContext context, WidgetRef ref) async {
    try {
      await ref.read(householdCurrentProvider.notifier).decline(household.id);
    } catch (e) {
      if (context.mounted) showErrorSnackBar(context, e);
    }
  }

  Future<void> _leave(BuildContext context, WidgetRef ref) async {
    final confirmed = await showConfirmDialog(
      context,
      title: context.l10n.householdLeaveConfirmTitle,
      content: context.l10n.householdLeaveConfirmBody,
      confirmText: context.l10n.householdLeave,
    );
    if (!confirmed) return;
    try {
      await ref.read(householdCurrentProvider.notifier).leave();
    } catch (e) {
      if (context.mounted) showErrorSnackBar(context, e);
    }
  }
}
