import 'payment_embedded.dart';
import 'reminder_embedded.dart';

enum PaymentPeriodType {
  daily,
  weekly,
  monthly,
  quarterly,
  yearly;

  String get label => switch (this) {
    PaymentPeriodType.daily => 'Günlük',
    PaymentPeriodType.weekly => 'Haftalık',
    PaymentPeriodType.monthly => 'Aylık',
    PaymentPeriodType.quarterly => 'Üç Aylık',
    PaymentPeriodType.yearly => 'Yıllık',
  };
}

enum DebtStatusFilter { all, active, passive }

enum DebtStatus {
  active,
  passive;

  String get label => switch (this) {
    DebtStatus.active => 'Aktif',
    DebtStatus.passive => 'Pasif',
  };
}

class Debt {
  const Debt({
    required this.id,
    required this.name,
    required this.ownerId,
    this.categoryId,
    this.description,
    required this.firstPaymentDate,
    this.lastPaymentDate,
    required this.paymentPeriodType,
    required this.amount,
    required this.status,
    required this.autoPayment,
    required this.payments,
    this.reminder,
    this.nextReminderDate,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String name;
  final String ownerId;
  final String? categoryId;
  final String? description;
  final DateTime firstPaymentDate;
  final DateTime? lastPaymentDate;
  final PaymentPeriodType paymentPeriodType;
  final double amount;
  final DebtStatus status;
  final bool autoPayment;
  final List<PaymentEmbedded> payments;
  final ReminderEmbedded? reminder;
  final DateTime? nextReminderDate;
  final DateTime? createdAt;
  final DateTime? updatedAt;
}
