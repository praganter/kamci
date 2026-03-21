enum AccessLevel {
  read,
  write,
  admin;

  String get label => switch (this) {
    AccessLevel.read => 'Okuma',
    AccessLevel.write => 'Yazma',
    AccessLevel.admin => 'Admin',
  };
}

class MemberEmbedded {
  const MemberEmbedded({required this.userId, required this.accessLevel});
  final String userId;
  final AccessLevel accessLevel;
}
