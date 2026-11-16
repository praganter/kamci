class UserDetail {
  const UserDetail({
    required this.email,
    required this.premium,
    required this.createdAt,
  });

  final String email;
  final bool premium;
  final DateTime createdAt;
}
