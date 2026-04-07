import 'payment_embedded.dart';
import 'reminder_embedded.dart';

enum PaymentPeriodType {
  daily,
  weekly,
  biWeekly,
  monthly,
  quarterly,
  halfYearly,
  yearly;

  String get label => switch (this) {
    PaymentPeriodType.daily => 'Günlük',
    PaymentPeriodType.weekly => 'Haftalık',
    PaymentPeriodType.biWeekly => 'İki Haftalık',
    PaymentPeriodType.monthly => 'Aylık',
    PaymentPeriodType.quarterly => 'Üç Aylık',
    PaymentPeriodType.halfYearly => 'Altı Aylık',
    PaymentPeriodType.yearly => 'Yıllık',
  };
}

enum DebtStatusFilter { all, active, completed }

enum DebtStatus {
  active,
  completed;

  String get label => switch (this) {
    DebtStatus.active => 'Aktif',
    DebtStatus.completed => 'Tamamlandı',
  };
}

enum PaymentStatus { paid, unpaid, overdue, partiallyPaid }

class Debt {
  const Debt({
    required this.id,
    required this.name,
    required this.ownerId,
    this.categoryId,
    this.description,
    required this.firstPaymentDate,
    this.installmentCount,
    this.paidInstallmentCount,
    required this.paymentPeriodType,
    required this.amount,
    required this.status,
    required this.autoPayment,
    required this.paymentStatus,
    required this.payments,
    this.currency,
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
  final int? installmentCount;
  final int? paidInstallmentCount;
  final PaymentPeriodType paymentPeriodType;
  final double amount;
  final String? currency;
  final DebtStatus status;
  final bool autoPayment;
  final PaymentStatus paymentStatus;
  final List<PaymentEmbedded> payments;
  final ReminderEmbedded? reminder;
  final DateTime? nextReminderDate;
  final DateTime? createdAt;
  final DateTime? updatedAt;
}
