import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../domain/entities/reminder_embedded.dart';
import '../../../../core/l10n/l10n.dart';
import '../../../../core/l10n/enum_l10n.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../shared/widgets/pro_badge.dart';
import '../../../../core/utils/validators.dart';
import '../../../../features/category/presentation/providers/category_providers.dart';
import '../../../../features/category/presentation/widgets/add_category_sheet.dart';
import '../../../../shared/providers/current_user_provider.dart';
import '../../../../shared/widgets/platform_date_field.dart';
import '../../../../shared/widgets/platform_select_field.dart';
import '../../../../shared/widgets/platform_time_field.dart';
import '../../../../shared/widgets/premium_required_sheet.dart';
import '../../../subscription/presentation/providers/subscription_providers.dart';
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
  final _installmentCtrl = TextEditingController();
  DateTime? _firstPaymentDate;
  PaymentPeriodType? _periodType;
  String? _selectedCategoryId;
  String? _selectedCurrency;
  bool _autoPayment = false;
  bool _reminderEnabled = false;
  List<String> _reminderChannels = [];
  DateTime? _reminderDate;
  TimeOfDay? _reminderTime;
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
      _installmentCtrl.text = debt.installmentCount != null
          ? debt.installmentCount.toString()
          : '';
      _periodType = debt.paymentPeriodType;
      _selectedCategoryId = debt.categoryId;
      _selectedCurrency = debt.currency;
      _autoPayment = debt.autoPayment;
      _reminderEnabled = debt.reminder?.enabled ?? false;
      _reminderChannels = debt.reminder?.channels?.toList() ?? [];
      _reminderDate = debt.reminder?.reminderDate;
      final timeStr = debt.reminder?.reminderTime;
      if (timeStr != null) {
        final parts = timeStr.split(':');
        _reminderTime = TimeOfDay(
          hour: int.tryParse(parts[0]) ?? 0,
          minute: int.tryParse(parts[1]) ?? 0,
        );
      }
      _initialized = true;
    });
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _amountCtrl.dispose();
    _descCtrl.dispose();
    _installmentCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final l10n = context.l10n;
    if (_reminderEnabled) {
      if (_reminderChannels.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.reminderChannelRequired)),
        );
        return;
      }
      if (_reminderDate == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.reminderDateRequired)),
        );
        return;
      }
      if (_reminderTime == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.reminderTimeRequired)),
        );
        return;
      }
    }

    final params = CreateDebtParams(
      name: _nameCtrl.text.trim(),
      categoryId: _selectedCategoryId,
      description: _descCtrl.text.trim().isEmpty ? null : _descCtrl.text.trim(),
      firstPaymentDate: _firstPaymentDate!,
      installmentCount: _periodType == PaymentPeriodType.oneTime
          ? 1
          : int.tryParse(_installmentCtrl.text.trim()),
      paymentPeriodType: _periodType!,
      amount: double.parse(_amountCtrl.text.replaceAll(',', '.')),
      autoPayment: _autoPayment,
      currency: _selectedCurrency,
      reminder: _reminderEnabled
          ? ReminderEmbedded(
              enabled: true,
              channels: _reminderChannels,
              reminderDate: _reminderDate,
              reminderTime: _reminderTime != null
                  ? '${_reminderTime!.hour.toString().padLeft(2, '0')}:${_reminderTime!.minute.toString().padLeft(2, '0')}:00'
                  : null,
            )
          : null,
    );

    final repo = ref.read(debtRepositoryProvider);
    try {
      if (widget.debtId != null) {
        await repo.update(widget.debtId!, params);
      } else {
        await repo.create(params);
      }
      ref.invalidate(debtListProvider);
      ref.invalidate(debtGroupedProvider(true));
      ref.invalidate(debtGroupedProvider(false));
      if (widget.debtId != null) {
        ref.invalidate(debtDetailProvider(widget.debtId!));
      }
      if (mounted) context.pop();
    } on PremiumLimitException {
      if (mounted) {
        PremiumRequiredSheet.show(
          context,
          featureName: context.l10n.debts.toLowerCase(),
          limitCount: 5,
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
  }

  Future<void> _openAddCategorySheet() async {
    final isPremium = ref.read(isPremiumProvider);
    if (!isPremium) {
      final categories = ref.read(categoryListProvider).valueOrNull ?? [];
      final currentUserId = ref.read(currentUserIdProvider).valueOrNull;
      final ownedCategories = categories
          .where((c) => c.ownerId == currentUserId)
          .length;

      if (ownedCategories >= 2) {
        PremiumRequiredSheet.show(
          context,
          featureName: context.l10n.categories.toLowerCase(),
          limitCount: 2,
        );
        return;
      }
    }

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
    final currenciesAsync = ref.watch(currencyListProvider);

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
              // ── Kategori ────────────────────────────────────────────
              categoriesAsync.when(
                loading: () => const LinearProgressIndicator(),
                error: (e, _) => const SizedBox.shrink(),
                data: (categories) => Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: PlatformSelectField<String>(
                        label: l10n.category,
                        prefixIcon: const Icon(Icons.folder_outlined),
                        value: _selectedCategoryId,
                        hint: l10n.select,
                        items: categories
                            .map(
                              (c) => PlatformSelectItem(
                                value: c.id,
                                label: c.name,
                              ),
                            )
                            .toList(),
                        onChanged: (v) =>
                            setState(() => _selectedCategoryId = v),
                        validator: (v) => v == null
                            ? Validators.required(
                                null,
                                fieldName: l10n.category,
                              )
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
                            color: Theme.of(
                              context,
                            ).colorScheme.onPrimaryContainer,
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
                textCapitalization: TextCapitalization.words,
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
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                validator: Validators.amount,
              ),
              const SizedBox(height: 16),

              // ── Para Birimi ─────────────────────────────────────────
              currenciesAsync.when(
                loading: () => const LinearProgressIndicator(),
                error: (e, s) => const SizedBox.shrink(),
                data: (currencies) {
                  final sorted = [...currencies]..sort((a, b) {
                      if (a.code == 'TRY') return -1;
                      if (b.code == 'TRY') return 1;
                      return a.code.compareTo(b.code);
                    });
                  return PlatformSelectField<String>(
                    label: l10n.currency,
                    prefixIcon: const Icon(Icons.attach_money),
                    value: _selectedCurrency,
                    hint: l10n.select,
                    items: sorted
                        .map(
                          (c) => PlatformSelectItem(
                            value: c.code,
                            label: '${c.code} ${c.symbol}',
                          ),
                        )
                        .toList(),
                    onChanged: (v) => setState(() => _selectedCurrency = v),
                    validator: (v) => v == null
                        ? Validators.required(null, fieldName: l10n.currency)
                        : null,
                  );
                },
              ),
              const SizedBox(height: 16),

              // ── Ödeme Periyodu ──────────────────────────────────────
              PlatformSelectField<PaymentPeriodType>(
                label: l10n.paymentPeriod,
                prefixIcon: const Icon(Icons.repeat),
                value: _periodType,
                hint: l10n.select,
                items: PaymentPeriodType.values
                    .map(
                      (t) => PlatformSelectItem(
                        value: t,
                        label: t.l10nLabel(l10n),
                      ),
                    )
                    .toList(),
                onChanged: (v) => setState(() => _periodType = v),
                validator: (v) => v == null
                    ? Validators.required(null, fieldName: l10n.paymentPeriod)
                    : null,
              ),
              const SizedBox(height: 16),

              // ── İlk Ödeme Tarihi ────────────────────────────────────
              PlatformDateField(
                label: l10n.firstPaymentDate,
                prefixIcon: const Icon(Icons.calendar_today),
                value: _firstPaymentDate,
                hint: l10n.select,
                firstDate: DateTime(2000),
                lastDate: DateTime(2100),
                onChanged: (date) =>
                    setState(() => _firstPaymentDate = date),
                validator: (v) => v == null
                    ? Validators.required(
                        null,
                        fieldName: l10n.firstPaymentDate,
                      )
                    : null,
              ),
              const SizedBox(height: 16),

              // ── Taksit Sayısı ────────────────────────────────────────
              if (_periodType != PaymentPeriodType.oneTime) ...[
                TextFormField(
                  controller: _installmentCtrl,
                  decoration: InputDecoration(
                    labelText: l10n.installmentCountOptional,
                    prefixIcon: const Icon(Icons.format_list_numbered),
                  ),
                  keyboardType: TextInputType.number,
                  validator: (v) {
                    if (v == null || v.trim().isEmpty) return null;
                    final n = int.tryParse(v.trim());
                    if (n == null || n < 1) return '≥ 1';
                    return null;
                  },
                ),
                const SizedBox(height: 16),
              ],


              // ── Açıklama ────────────────────────────────────────────
              TextFormField(
                controller: _descCtrl,
                textCapitalization: TextCapitalization.words,
                decoration: InputDecoration(
                  labelText: l10n.descriptionOptional,
                  prefixIcon: const Icon(Icons.notes),
                ),
                maxLines: 3,
              ),
              const SizedBox(height: 8),

              // ── Otomatik Ödeme ──────────────────────────────────────
              Theme(
                data: Theme.of(context).copyWith(
                  splashFactory: NoSplash.splashFactory,
                  highlightColor: Colors.transparent,
                  hoverColor: Colors.transparent,
                ),
                child: CheckboxListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(l10n.autoPayment),
                  value: _autoPayment,
                  onChanged: (v) => setState(() => _autoPayment = v ?? false),
                  controlAffinity: ListTileControlAffinity.trailing,
                ),
              ),

              // ── Hatırlatma ──────────────────────────────────────────
              Theme(
                data: Theme.of(context).copyWith(
                  splashFactory: NoSplash.splashFactory,
                  highlightColor: Colors.transparent,
                  hoverColor: Colors.transparent,
                ),
                child: CheckboxListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Row(
                    children: [
                      Text(l10n.reminder),
                      const SizedBox(width: 6),
                      const ProBadge(),
                    ],
                  ),
                  value: _reminderEnabled,
                  onChanged: (v) {
                    final isPremium = ref.read(isPremiumProvider);
                    if (!isPremium) {
                      PremiumRequiredSheet.show(
                        context,
                        featureName: l10n.reminder.toLowerCase(),
                        limitCount: 0,
                        message: l10n.premiumFeatureRequired(l10n.reminder),
                      );
                      return;
                    }
                    setState(() => _reminderEnabled = v ?? false);
                  },
                  controlAffinity: ListTileControlAffinity.trailing,
                ),
              ),
              if (_reminderEnabled) ...[
                const SizedBox(height: 8),
                Text(
                  l10n.reminderChannels,
                  style: Theme.of(context).textTheme.labelMedium,
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Expanded(
                      child: CheckboxListTile(
                        contentPadding: EdgeInsets.zero,
                        title: Text(l10n.channelEmail),
                        value: _reminderChannels.contains('EMAIL'),
                        onChanged: (v) => setState(() {
                          if (v == true) {
                            _reminderChannels = [..._reminderChannels, 'EMAIL'];
                          } else {
                            _reminderChannels = _reminderChannels
                                .where((c) => c != 'EMAIL')
                                .toList();
                          }
                        }),
                      ),
                    ),
                    Expanded(
                      child: CheckboxListTile(
                        contentPadding: EdgeInsets.zero,
                        title: Text(l10n.channelInApp),
                        value: _reminderChannels.contains('IN_APP'),
                        onChanged: (v) => setState(() {
                          if (v == true) {
                            _reminderChannels = [
                              ..._reminderChannels,
                              'IN_APP',
                            ];
                          } else {
                            _reminderChannels = _reminderChannels
                                .where((c) => c != 'IN_APP')
                                .toList();
                          }
                        }),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: PlatformDateField(
                        label: l10n.reminderDate,
                        prefixIcon: const Icon(Icons.calendar_month_outlined),
                        value: _reminderDate,
                        hint: l10n.select,
                        firstDate: DateTime.now(),
                        lastDate: DateTime(2100),
                        onChanged: (date) =>
                            setState(() => _reminderDate = date),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: PlatformTimeField(
                        label: l10n.reminderTime,
                        prefixIcon: const Icon(Icons.access_time),
                        value: _reminderTime,
                        hint: l10n.select,
                        onChanged: (time) =>
                            setState(() => _reminderTime = time),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
              ],
              const SizedBox(height: 24),

              ElevatedButton(
                onPressed: _submit,
                child: Text(widget.debtId == null ? l10n.create : l10n.update),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
