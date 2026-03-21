import 'debt.dart';

class CategoryWithDebts {
  const CategoryWithDebts({
    required this.categoryId,
    required this.categoryName,
    required this.owned,
    required this.debts,
  });

  final String categoryId;
  final String categoryName;
  final bool owned;
  final List<Debt> debts;
}
