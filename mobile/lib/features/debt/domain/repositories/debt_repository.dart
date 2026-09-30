import '../entities/bulk_payment_entry.dart';
import '../entities/category_with_debts.dart';
import '../entities/debt.dart';
import '../entities/debt_summary.dart';
import '../entities/reminder_embedded.dart';

abstract interface class DebtRepository {
  Future<List<DebtSummary>> getAll(String month, bool isOwner);
  Future<List<CategoryWithDebts>> getGrouped(String month, bool isOwner);
  Future<Debt> getById(String id);
  Future<Debt> create(CreateDebtParams params);
  Future<Debt> update(String id, CreateDebtParams params);
  Future<Debt> pay(String id, {double? amountPaid, DateTime? paidTime});
  Future<Debt> skip(String id);
  Future<Debt> setPaidInstallments(String id, int count);
  Future<Debt> bulkPay(String id, List<BulkPaymentEntry> payments);
  Future<void> delete(String id);
}

class CreateDebtParams {
  const CreateDebtParams({
    this.id,
    required this.name,
    this.categoryId,
    this.description,
    required this.firstPaymentDate,
    this.installmentCount,
    required this.paymentPeriodType,
    required this.amount,
    this.autoPayment = false,
    this.reminder,
    this.currency,
  });

  final String? id;
  final String name;
  final String? categoryId;
  final String? description;
  final DateTime firstPaymentDate;
  final int? installmentCount;
  final PaymentPeriodType paymentPeriodType;
  final double amount;
  final bool autoPayment;
  final ReminderEmbedded? reminder;
  final String? currency;
}
