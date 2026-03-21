import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/l10n/enum_l10n.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../core/utils/validators.dart';
import '../../data/repositories/debt_repository_impl.dart';
import '../../domain/entities/debt.dart';
import '../../domain/repositories/debt_repository.dart';
import '../providers/debt_providers.dart';
import '../../../../features/category/presentation/providers/category_providers.dart';

class DebtFormScreen extends ConsumerStatefulWidget {
  const DebtFormScreen({super.key, this.debtId});
  final String? debtId;

  @override
  ConsumerState<DebtFormScreen> createState() => _DebtFormScreenState();
}

class _DebtFormScreenState extends ConsumerState<DebtFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameCtrl = TextEditingController();
  final _amountCtrl = TextEditingController();
  final _descCtrl = TextEditingController();
  DateTime? _startDate;
  DateTime? _endDate;
  PaymentPeriodType _periodType = PaymentPeriodType.monthly;
  DebtStatus _status = DebtStatus.active;
  String? _selectedCategoryId;
  bool _initialized = false;

  @override
  void initState() {
    super.initState();
    if (widget.debtId != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        final current = ref.read(debtDetailProvider(widget.debtId!));
        current.whenData(_prefillFields);

        ref.listenManual<AsyncValue<Debt>>(
          debtDetailProvider(widget.debtId!),
          (_, next) => next.whenData(_prefillFields),
        );
      });
    }
  }

  void _prefillFields(Debt debt) {
    if (_initialized || !mounted) return;
    setState(() {
      _nameCtrl.text = debt.name;
      _amountCtrl.text = debt.amount.toString();
      _descCtrl.text = debt.description ?? '';
      _startDate = debt.startDate;
      _endDate = debt.endDate;
      _periodType = debt.paymentPeriodType;
      _status = debt.status;
      _selectedCategoryId = debt.categoryId;
      _initialized = true;
    });
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _amountCtrl.dispose();
    _descCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickDate(bool isStart) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: (isStart ? _startDate : _endDate) ?? DateTime.now(),
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );
    if (picked != null) {
      setState(() {
        if (isStart) {
          _startDate = picked;
        } else {
          _endDate = picked;
        }
      });
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final l10n = context.l10n;
    if (_startDate == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.selectStartDate)),
      );
      return;
    }

    final params = CreateDebtParams(
      name: _nameCtrl.text.trim(),
      categoryId: _selectedCategoryId,
      description:
          _descCtrl.text.trim().isEmpty ? null : _descCtrl.text.trim(),
      startDate: _startDate!,
      endDate: _endDate,
      paymentPeriodType: _periodType,
      amount: double.parse(_amountCtrl.text.replaceAll(',', '.')),
      status: _status,
    );

    final repo = ref.read(debtRepositoryProvider);
    try {
      if (widget.debtId != null) {
        await repo.update(widget.debtId!, params);
      } else {
        await repo.create(params);
      }
      ref.invalidate(debtListProvider);
      if (mounted) context.pop();
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
    final categoriesAsync = ref.watch(categoryListProvider);

    return Scaffold(
      appBar: AppBar(
        title:
            Text(widget.debtId == null ? l10n.newDebt : l10n.editDebt),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                controller: _nameCtrl,
                decoration: InputDecoration(
                  labelText: l10n.debtName,
                  prefixIcon: const Icon(Icons.label_outline),
                ),
                validator: (v) =>
                    Validators.required(v, fieldName: l10n.debtName),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _amountCtrl,
                decoration: InputDecoration(
                  labelText: l10n.amount,
                  prefixIcon: const Icon(Icons.attach_money),
                  hintText: '0.00',
                ),
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                validator: Validators.amount,
              ),
              const SizedBox(height: 16),
              // ignore: deprecated_member_use
              DropdownButtonFormField<PaymentPeriodType>(
                // ignore: deprecated_member_use
                value: _periodType,
                decoration: InputDecoration(
                  labelText: l10n.paymentPeriod,
                  prefixIcon: const Icon(Icons.repeat),
                ),
                items: PaymentPeriodType.values
                    .map((t) => DropdownMenuItem(
                        value: t, child: Text(t.l10nLabel(l10n))))
                    .toList(),
                onChanged: (v) => setState(() => _periodType = v!),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: InkWell(
                      onTap: () => _pickDate(true),
                      child: InputDecorator(
                        decoration: InputDecoration(
                          labelText: l10n.startDate,
                          prefixIcon: const Icon(Icons.calendar_today),
                        ),
                        child: Text(
                          _startDate != null
                              ? DateFormatter.formatDate(_startDate!)
                              : l10n.select,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: InkWell(
                      onTap: () => _pickDate(false),
                      child: InputDecorator(
                        decoration: InputDecoration(
                          labelText: l10n.endDate,
                          prefixIcon: const Icon(Icons.event),
                        ),
                        child: Text(
                          _endDate != null
                              ? DateFormatter.formatDate(_endDate!)
                              : l10n.select,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              categoriesAsync.when(
                loading: () => const LinearProgressIndicator(),
                error: (e, _) => const SizedBox.shrink(),
                data: (categories) =>
                    // ignore: deprecated_member_use
                    DropdownButtonFormField<String?>(
                  // ignore: deprecated_member_use
                  value: _selectedCategoryId,
                  decoration: InputDecoration(
                    labelText: l10n.categoryOptional,
                    prefixIcon: const Icon(Icons.folder_outlined),
                  ),
                  items: [
                    DropdownMenuItem(
                        value: null, child: Text(l10n.noCategory)),
                    ...categories.map(
                      (c) =>
                          DropdownMenuItem(value: c.id, child: Text(c.name)),
                    ),
                  ],
                  onChanged: (v) => setState(() => _selectedCategoryId = v),
                ),
              ),
              const SizedBox(height: 16),
              // ignore: deprecated_member_use
              DropdownButtonFormField<DebtStatus>(
                // ignore: deprecated_member_use
                value: _status,
                decoration: InputDecoration(
                  labelText: l10n.status,
                  prefixIcon: const Icon(Icons.info_outline),
                ),
                items: DebtStatus.values
                    .map((s) => DropdownMenuItem(
                        value: s, child: Text(s.l10nLabel(l10n))))
                    .toList(),
                onChanged: (v) => setState(() => _status = v!),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _descCtrl,
                decoration: InputDecoration(
                  labelText: l10n.descriptionOptional,
                  prefixIcon: const Icon(Icons.notes),
                ),
                maxLines: 3,
              ),
              const SizedBox(height: 32),
              ElevatedButton(
                onPressed: _submit,
                child: Text(
                    widget.debtId == null ? l10n.create : l10n.update),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
