import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/l10n/enum_l10n.dart';
import '../../../../core/utils/date_formatter.dart';
import '../../../../core/utils/validators.dart';
import '../../../../features/category/presentation/providers/category_providers.dart';
import '../../../../features/category/presentation/widgets/add_category_sheet.dart';
import '../../data/repositories/debt_repository_impl.dart';
import '../../domain/entities/debt.dart';
import '../../domain/repositories/debt_repository.dart';
import '../providers/debt_providers.dart';

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
  DateTime? _firstPaymentDate;
  DateTime? _lastPaymentDate;
  PaymentPeriodType _periodType = PaymentPeriodType.monthly;
  String? _selectedCategoryId;
  bool _autoPayment = false;
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
      _firstPaymentDate = debt.firstPaymentDate;
      _lastPaymentDate = debt.lastPaymentDate;
      _periodType = debt.paymentPeriodType;
      _selectedCategoryId = debt.categoryId;
      _autoPayment = debt.autoPayment;
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
      initialDate:
          (isStart ? _firstPaymentDate : _lastPaymentDate) ?? DateTime.now(),
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );
    if (picked != null) {
      setState(() {
        if (isStart) {
          _firstPaymentDate = picked;
        } else {
          _lastPaymentDate = picked;
        }
      });
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final l10n = context.l10n;
    if (_selectedCategoryId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.category)),
      );
      return;
    }
    if (_firstPaymentDate == null) {
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
      firstPaymentDate: _firstPaymentDate!,
      lastPaymentDate: _lastPaymentDate,
      paymentPeriodType: _periodType,
      amount: double.parse(_amountCtrl.text.replaceAll(',', '.')),
      autoPayment: _autoPayment,
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

  Future<void> _openAddCategorySheet() async {
    final createdId = await showModalBottomSheet<String?>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => const AddCategorySheet(),
    );
    if (createdId != null && mounted) {
      await ref.read(categoryListProvider.future);
      if (mounted) setState(() => _selectedCategoryId = createdId);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final categoriesAsync = ref.watch(categoryListProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.debtId == null ? l10n.newDebt : l10n.editDebt),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // ── Kategori (zorunlu) ──────────────────────────────────
              categoriesAsync.when(
                loading: () => const LinearProgressIndicator(),
                error: (e, _) => const SizedBox.shrink(),
                data: (categories) => Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      // ignore: deprecated_member_use
                      child: DropdownButtonFormField<String>(
                        // ignore: deprecated_member_use
                        value: _selectedCategoryId,
                        decoration: InputDecoration(
                          labelText: l10n.category,
                          prefixIcon: const Icon(Icons.folder_outlined),
                        ),
                        hint: Text(l10n.select),
                        items: categories
                            .map((c) => DropdownMenuItem(
                                value: c.id, child: Text(c.name)))
                            .toList(),
                        onChanged: (v) =>
                            setState(() => _selectedCategoryId = v),
                        validator: (v) => v == null
                            ? Validators.required(null,
                                fieldName: l10n.category)
                            : null,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Material(
                      color: Theme.of(context).colorScheme.primaryContainer,
                      borderRadius: BorderRadius.circular(16),
                      child: InkWell(
                        onTap: _openAddCategorySheet,
                        borderRadius: BorderRadius.circular(16),
                        child: SizedBox(
                          width: 56,
                          height: 56,
                          child: Icon(
                            Icons.add,
                            color:
                                Theme.of(context).colorScheme.onPrimaryContainer,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // ── Borç Adı ────────────────────────────────────────────
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

              // ── Tutar ───────────────────────────────────────────────
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

              // ── Ödeme Periyodu ──────────────────────────────────────
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

              // ── Tarihler ────────────────────────────────────────────
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
                          _firstPaymentDate != null
                              ? DateFormatter.formatDate(_firstPaymentDate!)
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
                          _lastPaymentDate != null
                              ? DateFormatter.formatDate(_lastPaymentDate!)
                              : l10n.select,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // ── Açıklama ────────────────────────────────────────────
              TextFormField(
                controller: _descCtrl,
                decoration: InputDecoration(
                  labelText: l10n.descriptionOptional,
                  prefixIcon: const Icon(Icons.notes),
                ),
                maxLines: 3,
              ),
              const SizedBox(height: 8),

              // ── Otomatik Ödeme ──────────────────────────────────────
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(l10n.autoPayment),
                value: _autoPayment,
                onChanged: (v) => setState(() => _autoPayment = v),
              ),
              const SizedBox(height: 24),

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
