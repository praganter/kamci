import 'debt_summary.dart';

class CategoryWithDebts {
  const CategoryWithDebts({
    required this.categoryId,
    required this.categoryName,
    required this.owned,
    this.ownerUserId,
    required this.debts,
  });

  final String categoryId;
  final String categoryName;
  final bool owned;
  final String? ownerUserId;
  final List<DebtSummary> debts;
}
