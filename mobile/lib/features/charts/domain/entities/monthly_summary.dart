class MonthlySummary {
  const MonthlySummary({
    required this.month,
    this.baseCurrency,
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

  /// Currency the aggregate amounts are converted to (user's income currency).
  /// Null when the backend predates the field.
  final String? baseCurrency;
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
  final List<MonthlyDebtItem> debts;
  final List<TrendPoint> trend;
  final List<CategoryBreakdownItem> categoryBreakdown;
  final List<DebtProgressItem> debtProgress;
}

class MonthlyDebtItem {
  const MonthlyDebtItem({
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
}

class TrendPoint {
  const TrendPoint({
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
}

class CategoryBreakdownItem {
  const CategoryBreakdownItem({
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
}

class DebtProgressItem {
  const DebtProgressItem({
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

  bool get isOpenEnded => totalInstallments == null;
  bool get isCompleted => status == 'COMPLETED';
}
