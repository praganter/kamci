import '../entities/debt.dart';
import '../entities/reminder_embedded.dart';

abstract interface class DebtRepository {
  Future<List<Debt>> getAll();
  Future<Debt> getById(String id);
  Future<Debt> create(CreateDebtParams params);
  Future<Debt> update(String id, CreateDebtParams params);
  Future<Debt> pay(String id);
  Future<void> delete(String id);
}

class CreateDebtParams {
  const CreateDebtParams({
    this.id,
    required this.name,
    this.categoryId,
    this.description,
    required this.firstPaymentDate,
    this.lastPaymentDate,
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
  final DateTime? lastPaymentDate;
  final PaymentPeriodType paymentPeriodType;
  final double amount;
  final bool autoPayment;
  final ReminderEmbedded? reminder;
  final String? currency;
}
