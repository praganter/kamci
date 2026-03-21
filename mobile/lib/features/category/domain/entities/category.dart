import 'member_embedded.dart';

class Category {
  const Category({
    required this.id,
    required this.ownerId,
    required this.name,
    required this.members,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String ownerId;
  final String name;
  final List<MemberEmbedded> members;
  final DateTime? createdAt;
  final DateTime? updatedAt;
}
