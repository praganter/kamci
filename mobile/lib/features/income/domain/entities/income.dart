class Income {
  const Income({
    required this.id,
    required this.amount,
    required this.currency,
    required this.effectiveFrom,
    this.note,
    this.createdAt,
  });

  final String id;
  final double amount;
  final String currency;
  final DateTime effectiveFrom;
  final String? note;
  final DateTime? createdAt;
}
