class Subscription {
  const Subscription({
    required this.plan,
    required this.status,
    this.expiresDate,
    this.accessToken,
    this.refreshToken,
  });

  final String plan;
  final String status;
  final DateTime? expiresDate;
  final String? accessToken;
  final String? refreshToken;

  bool get isPremium =>
      (plan == 'MONTHLY' || plan == 'YEARLY') && status == 'ACTIVE';
}
