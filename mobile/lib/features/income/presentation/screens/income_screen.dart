import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../core/utils/validators.dart';
import '../../../../shared/widgets/app_error_widget.dart';
import '../../../../features/debt/presentation/providers/debt_providers.dart';
import '../../data/models/save_income_request.dart';
import '../../data/repositories/income_repository_impl.dart';
import '../../domain/entities/income.dart';
import '../providers/income_providers.dart';

class IncomeScreen extends ConsumerWidget {
  const IncomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final historyAsync = ref.watch(incomeHistoryProvider);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.incomeHistory)),
      body: historyAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => AppErrorWidget(
          message: e.toString(),
          onRetry: () => ref.invalidate(incomeHistoryProvider),
        ),
        data: (incomes) {
          if (incomes.isEmpty) {
            return Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.savings_outlined,
                    size: 64,
                    color: Theme.of(context).colorScheme.primary.withValues(alpha: 0.4),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    l10n.noIncomeYet,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ],
              ),
            );
          }
          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(incomeHistoryProvider),
            child: ListView.builder(
              padding: const EdgeInsets.only(bottom: 88),
              itemCount: incomes.length,
              itemBuilder: (context, i) =>
                  _IncomeTile(income: incomes[i]),
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddIncomeSheet(context, ref),
        child: const Icon(Icons.add),
      ),
    );
  }

  void _showAddIncomeSheet(BuildContext context, WidgetRef ref) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => _AddIncomeSheet(),
    );
  }
}

class _IncomeTile extends ConsumerWidget {
  const _IncomeTile({required this.income});
  final Income income;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final cs = Theme.of(context).colorScheme;
    final currenciesAsync = ref.watch(currencyListProvider);
    final symbol = currenciesAsync.maybeWhen(
      data: (list) {
        final match = list.where((c) => c.code == income.currency).firstOrNull;
        return match?.symbol ?? income.currency;
      },
      orElse: () => income.currency,
    );

    return Dismissible(
      key: ValueKey(income.id),
      direction: DismissDirection.endToStart,
      background: Container(
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: 24),
        color: cs.error,
        child: Icon(Icons.delete_outline, color: cs.onError),
      ),
      confirmDismiss: (_) async {
        return await showDialog<bool>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: Text(l10n.deleteIncome),
            content: Text(l10n.deleteIncomeConfirm),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx, false),
                child: Text(l10n.cancel),
              ),
              TextButton(
                onPressed: () => Navigator.pop(ctx, true),
                child: Text(l10n.delete,
                    style: TextStyle(color: cs.error)),
              ),
            ],
          ),
        );
      },
      onDismissed: (_) async {
        try {
          await ref.read(incomeRepositoryProvider).delete(income.id);
          ref.invalidate(incomeHistoryProvider);
          ref.invalidate(currentIncomeProvider);
        } catch (e) {
          if (context.mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(e.toString())),
            );
          }
        }
      },
      child: ListTile(
        leading: Container(
          width: 44,
          height: 44,
          decoration: BoxDecoration(
            color: cs.primaryContainer,
            shape: BoxShape.circle,
          ),
          child: Icon(Icons.trending_up, color: cs.onPrimaryContainer, size: 22),
        ),
        title: Text(
          NumberFormat.currency(
            locale: 'tr_TR',
            symbol: symbol,
            decimalDigits: 2,
          ).format(income.amount),
          style: const TextStyle(fontWeight: FontWeight.w700),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(DateFormatter.formatDate(income.effectiveFrom)),
            if (income.note != null && income.note!.isNotEmpty)
              Text(
                income.note!,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: cs.onSurfaceVariant,
                    ),
              ),
          ],
        ),
        isThreeLine: income.note != null && income.note!.isNotEmpty,
        trailing: const Icon(Icons.chevron_right, size: 16),
      ),
    );
  }
}

class _AddIncomeSheet extends ConsumerStatefulWidget {
  @override
  ConsumerState<_AddIncomeSheet> createState() => _AddIncomeSheetState();
}

class _AddIncomeSheetState extends ConsumerState<_AddIncomeSheet> {
  final _formKey = GlobalKey<FormState>();
  final _amountCtrl = TextEditingController();
  final _noteCtrl = TextEditingController();
  String? _selectedCurrency;
  DateTime? _effectiveFrom;

  @override
  void dispose() {
    _amountCtrl.dispose();
    _noteCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final l10n = context.l10n;
    if (_effectiveFrom == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.effectiveFrom)),
      );
      return;
    }

    try {
      final request = SaveIncomeRequest(
        amount: double.parse(_amountCtrl.text.replaceAll(',', '.')),
        currency: _selectedCurrency!,
        effectiveFrom: DateFormatter.toApiDateTimeFormat(_effectiveFrom!),
        note: _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim(),
      );
      await ref.read(incomeRepositoryProvider).save(request);
      ref.invalidate(incomeHistoryProvider);
      ref.invalidate(currentIncomeProvider);
      if (mounted) Navigator.pop(context);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString())),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final currenciesAsync = ref.watch(currencyListProvider);

    return Padding(
      padding: EdgeInsets.only(
        left: 24,
        right: 24,
        top: 24,
        bottom: MediaQuery.of(context).viewInsets.bottom + 24,
      ),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              l10n.addIncome,
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 20),

            // Amount
            TextFormField(
              controller: _amountCtrl,
              decoration: InputDecoration(
                labelText: l10n.incomeAmount,
                prefixIcon: const Icon(Icons.savings_outlined),
              ),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              validator: Validators.amount,
              autofocus: true,
            ),
            const SizedBox(height: 16),

            // Currency
            currenciesAsync.when(
              loading: () => const LinearProgressIndicator(),
              error: (_, e) => const SizedBox.shrink(),
              data: (currencies) => DropdownButtonFormField<String>(
                initialValue: _selectedCurrency,
                decoration: InputDecoration(
                  labelText: l10n.currency,
                  prefixIcon: const Icon(Icons.currency_exchange),
                ),
                hint: Text(l10n.select),
                items: currencies
                    .map((c) => DropdownMenuItem(
                          value: c.code,
                          child: Text('${c.code}  ${c.symbol}'),
                        ))
                    .toList(),
                onChanged: (v) => setState(() => _selectedCurrency = v),
                validator: (v) => v == null
                    ? Validators.required(null, fieldName: l10n.currency)
                    : null,
              ),
            ),
            const SizedBox(height: 16),

            // Effective From
            InkWell(
              onTap: () async {
                final picked = await showDatePicker(
                  context: context,
                  initialDate: _effectiveFrom ?? DateTime.now(),
                  firstDate: DateTime(2000),
                  lastDate: DateTime(2100),
                );
                if (picked != null) setState(() => _effectiveFrom = picked);
              },
              child: InputDecorator(
                decoration: InputDecoration(
                  labelText: l10n.effectiveFrom,
                  prefixIcon: const Icon(Icons.calendar_today),
                ),
                child: Text(
                  _effectiveFrom != null
                      ? DateFormatter.formatDate(_effectiveFrom!)
                      : l10n.select,
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Note
            TextFormField(
              controller: _noteCtrl,
              decoration: InputDecoration(
                labelText: l10n.incomeNote,
                prefixIcon: const Icon(Icons.notes),
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 24),

            ElevatedButton(
              onPressed: _submit,
              child: Text(l10n.save),
            ),
          ],
        ),
      ),
    );
  }
}
