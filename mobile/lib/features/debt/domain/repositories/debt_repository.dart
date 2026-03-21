import '../entities/debt.dart';
import '../entities/payment_embedded.dart';
import '../entities/reminder_embedded.dart';

abstract interface class DebtRepository {
  Future<List<Debt>> getAll();
  Future<Debt> create(CreateDebtParams params);
  Future<Debt> update(String id, CreateDebtParams params);
  Future<void> delete(String id);
}

class CreateDebtParams {
  const CreateDebtParams({
    this.id,
    required this.name,
    this.categoryId,
    this.description,
    required this.startDate,
    this.endDate,
    required this.paymentPeriodType,
    required this.amount,
    this.status = DebtStatus.active,
    this.payments = const [],
    this.reminder,
  });

  final String? id;
  final String name;
  final String? categoryId;
  final String? description;
  final DateTime startDate;
  final DateTime? endDate;
  final PaymentPeriodType paymentPeriodType;
  final double amount;
  final DebtStatus status;
  final List<PaymentEmbedded> payments;
  final ReminderEmbedded? reminder;
}
