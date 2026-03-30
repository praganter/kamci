class Subscription {
  const Subscription({
    required this.plan,
    required this.status,
    this.expiresDate,
  });

  final String plan;
  final String status;
  final DateTime? expiresDate;

  bool get isPremium => plan == 'PREMIUM' && status == 'ACTIVE';
}
