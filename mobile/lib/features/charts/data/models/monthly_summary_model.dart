import '../../domain/entities/monthly_summary.dart';

double _toDouble(dynamic v) {
  if (v == null) return 0.0;
  if (v is num) return v.toDouble();
  if (v is String) return double.tryParse(v) ?? 0.0;
  return 0.0;
}

double? _toDoubleOrNull(dynamic v) {
  if (v == null) return null;
  if (v is num) return v.toDouble();
  if (v is String) return double.tryParse(v);
  return null;
}

class MonthlySummaryModel {
  const MonthlySummaryModel({
    required this.month,
    required this.totalPaid,
    required this.paidCount,
    required this.skippedCount,
    required this.skippedAmount,
    required this.pendingCount,
    required this.pendingAmount,
    this.incomeAtMonth,
    this.debtToIncomeRatio,
    this.household = false,
    this.householdIncomeAtMonth,
    this.householdDebtToIncomeRatio,
    required this.debts,
    required this.trend,
    required this.categoryBreakdown,
    required this.debtProgress,
  });

  final String month;
  final double totalPaid;
  final int paidCount;
  final int skippedCount;
  final double skippedAmount;
  final int pendingCount;
  final double pendingAmount;
  final double? incomeAtMonth;
  final double? debtToIncomeRatio;
  final bool household;
  final double? householdIncomeAtMonth;
  final double? householdDebtToIncomeRatio;
  final List<MonthlyDebtItemModel> debts;
  final List<TrendPointModel> trend;
  final List<CategoryBreakdownItemModel> categoryBreakdown;
  final List<DebtProgressItemModel> debtProgress;

  factory MonthlySummaryModel.fromJson(Map<String, dynamic> json) {
    return MonthlySummaryModel(
      month: json['month'] as String? ?? '',
      totalPaid: _toDouble(json['totalPaid']),
      paidCount: (json['paidCount'] as num?)?.toInt() ?? 0,
      skippedCount: (json['skippedCount'] as num?)?.toInt() ?? 0,
      skippedAmount: _toDouble(json['skippedAmount']),
      pendingCount: (json['pendingCount'] as num?)?.toInt() ?? 0,
      pendingAmount: _toDouble(json['pendingAmount']),
      incomeAtMonth: _toDoubleOrNull(json['incomeAtMonth']),
      debtToIncomeRatio: _toDoubleOrNull(json['debtToIncomeRatio']),
      household: json['household'] as bool? ?? false,
      householdIncomeAtMonth: _toDoubleOrNull(json['householdIncomeAtMonth']),
      householdDebtToIncomeRatio: _toDoubleOrNull(
        json['householdDebtToIncomeRatio'],
      ),
      debts: (json['debts'] as List<dynamic>? ?? [])
          .map((e) => MonthlyDebtItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      trend: (json['trend'] as List<dynamic>? ?? [])
          .map((e) => TrendPointModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      categoryBreakdown: (json['categoryBreakdown'] as List<dynamic>? ?? [])
          .map(
            (e) =>
                CategoryBreakdownItemModel.fromJson(e as Map<String, dynamic>),
          )
          .toList(),
      debtProgress: (json['debtProgress'] as List<dynamic>? ?? [])
          .map((e) => DebtProgressItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  MonthlySummary toDomain() => MonthlySummary(
    month: month,
    totalPaid: totalPaid,
    paidCount: paidCount,
    skippedCount: skippedCount,
    skippedAmount: skippedAmount,
    pendingCount: pendingCount,
    pendingAmount: pendingAmount,
    incomeAtMonth: incomeAtMonth,
    debtToIncomeRatio: debtToIncomeRatio,
    household: household,
    householdIncomeAtMonth: householdIncomeAtMonth,
    householdDebtToIncomeRatio: householdDebtToIncomeRatio,
    debts: debts.map((e) => e.toDomain()).toList(),
    trend: trend.map((e) => e.toDomain()).toList(),
    categoryBreakdown: categoryBreakdown.map((e) => e.toDomain()).toList(),
    debtProgress: debtProgress.map((e) => e.toDomain()).toList(),
  );
}

class MonthlyDebtItemModel {
  const MonthlyDebtItemModel({
    required this.debtId,
    required this.debtName,
    required this.currency,
    this.amountPaid,
    required this.paid,
    required this.skipped,
    required this.pending,
    this.paidTime,
  });

  final String debtId;
  final String debtName;
  final String currency;
  final double? amountPaid;
  final bool paid;
  final bool skipped;
  final bool pending;
  final DateTime? paidTime;

  factory MonthlyDebtItemModel.fromJson(Map<String, dynamic> json) =>
      MonthlyDebtItemModel(
        debtId: json['debtId'] as String? ?? '',
        debtName: json['debtName'] as String? ?? '',
        currency: json['currency'] as String? ?? 'TRY',
        amountPaid: _toDoubleOrNull(json['amountPaid']),
        paid: json['paid'] as bool? ?? false,
        skipped: json['skipped'] as bool? ?? false,
        pending: json['pending'] as bool? ?? false,
        paidTime: json['paidTime'] != null
            ? DateTime.tryParse(json['paidTime'] as String)
            : null,
      );

  MonthlyDebtItem toDomain() => MonthlyDebtItem(
    debtId: debtId,
    debtName: debtName,
    currency: currency,
    amountPaid: amountPaid,
    paid: paid,
    skipped: skipped,
    pending: pending,
    paidTime: paidTime,
  );
}

class TrendPointModel {
  const TrendPointModel({
    required this.month,
    required this.totalPaid,
    required this.paidCount,
    required this.skippedCount,
    required this.pendingCount,
    this.debtToIncomeRatio,
  });

  final String month;
  final double totalPaid;
  final int paidCount;
  final int skippedCount;
  final int pendingCount;
  final double? debtToIncomeRatio;

  factory TrendPointModel.fromJson(Map<String, dynamic> json) =>
      TrendPointModel(
        month: json['month'] as String? ?? '',
        totalPaid: _toDouble(json['totalPaid']),
        paidCount: (json['paidCount'] as num?)?.toInt() ?? 0,
        skippedCount: (json['skippedCount'] as num?)?.toInt() ?? 0,
        pendingCount: (json['pendingCount'] as num?)?.toInt() ?? 0,
        debtToIncomeRatio: _toDoubleOrNull(json['debtToIncomeRatio']),
      );

  TrendPoint toDomain() => TrendPoint(
    month: month,
    totalPaid: totalPaid,
    paidCount: paidCount,
    skippedCount: skippedCount,
    pendingCount: pendingCount,
    debtToIncomeRatio: debtToIncomeRatio,
  );
}

class CategoryBreakdownItemModel {
  const CategoryBreakdownItemModel({
    required this.categoryId,
    required this.categoryName,
    required this.totalPaid,
    required this.debtCount,
    required this.percentage,
  });

  final String categoryId;
  final String categoryName;
  final double totalPaid;
  final int debtCount;
  final double percentage;

  factory CategoryBreakdownItemModel.fromJson(Map<String, dynamic> json) =>
      CategoryBreakdownItemModel(
        categoryId: json['categoryId'] as String? ?? '',
        categoryName: json['categoryName'] as String? ?? '',
        totalPaid: _toDouble(json['totalPaid']),
        debtCount: (json['debtCount'] as num?)?.toInt() ?? 0,
        percentage: _toDouble(json['percentage']),
      );

  CategoryBreakdownItem toDomain() => CategoryBreakdownItem(
    categoryId: categoryId,
    categoryName: categoryName,
    totalPaid: totalPaid,
    debtCount: debtCount,
    percentage: percentage,
  );
}

class DebtProgressItemModel {
  const DebtProgressItemModel({
    required this.debtId,
    required this.debtName,
    required this.currency,
    required this.totalAmount,
    required this.totalPaid,
    this.remainingAmount,
    required this.paidInstallments,
    this.totalInstallments,
    this.completionPercentage,
    required this.status,
  });

  final String debtId;
  final String debtName;
  final String currency;
  final double totalAmount;
  final double totalPaid;
  final double? remainingAmount;
  final int paidInstallments;
  final int? totalInstallments;
  final double? completionPercentage;
  final String status;

  factory DebtProgressItemModel.fromJson(Map<String, dynamic> json) =>
      DebtProgressItemModel(
        debtId: json['debtId'] as String? ?? '',
        debtName: json['debtName'] as String? ?? '',
        currency: json['currency'] as String? ?? 'TRY',
        totalAmount: _toDouble(json['totalAmount']),
        totalPaid: _toDouble(json['totalPaid']),
        remainingAmount: _toDoubleOrNull(json['remainingAmount']),
        paidInstallments: (json['paidInstallments'] as num?)?.toInt() ?? 0,
        totalInstallments: (json['totalInstallments'] as num?)?.toInt(),
        completionPercentage: _toDoubleOrNull(json['completionPercentage']),
        status: json['status'] as String? ?? 'ACTIVE',
      );

  DebtProgressItem toDomain() => DebtProgressItem(
    debtId: debtId,
    debtName: debtName,
    currency: currency,
    totalAmount: totalAmount,
    totalPaid: totalPaid,
    remainingAmount: remainingAmount,
    paidInstallments: paidInstallments,
    totalInstallments: totalInstallments,
    completionPercentage: completionPercentage,
    status: status,
  );
}
