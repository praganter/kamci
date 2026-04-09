import 'debt.dart';

class DebtSummary {
  const DebtSummary({
    required this.id,
    required this.name,
    required this.categoryId,
    this.installmentCount,
    required this.paidCount,
    required this.paymentPeriodType,
    required this.amount,
    required this.currency,
    required this.status,
    required this.paymentStatus,
    required this.autoPayment,
    required this.paidCurrentPeriod,
    required this.hasReminder,
  });

  final String id;
  final String name;
  final String categoryId;
  final int? installmentCount;
  final int paidCount;
  final PaymentPeriodType paymentPeriodType;
  final double amount;
  final String currency;
  final DebtStatus status;
  final PaymentStatus paymentStatus;
  final bool autoPayment;
  final bool paidCurrentPeriod;
  final bool hasReminder;
}
