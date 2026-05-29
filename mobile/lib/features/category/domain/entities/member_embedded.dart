enum AccessLevel {
  read,
  write,
  edit,
  admin;
}

extension AccessLevelPermissions on AccessLevel {
  bool get canWrite =>
      this == AccessLevel.write ||
      this == AccessLevel.edit ||
      this == AccessLevel.admin;
  bool get canEdit => this == AccessLevel.edit || this == AccessLevel.admin;
  bool get canDelete => this == AccessLevel.admin;
}

class MemberEmbedded {
  const MemberEmbedded({this.userId, required this.accessLevel, this.email});
  final String? userId;
  final AccessLevel accessLevel;
  final String? email;
}
