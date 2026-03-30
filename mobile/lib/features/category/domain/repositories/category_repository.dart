import '../entities/category.dart';
import '../entities/member_embedded.dart';

abstract interface class CategoryRepository {
  Future<List<Category>> getAll();
  Future<Category> create(String name);
  Future<Category> update(String id, String name);
  Future<void> delete(String id);
  Future<void> addMember(String categoryId, String email, AccessLevel accessLevel);
  Future<void> addMembersBulk(String categoryId, List<({String email, AccessLevel accessLevel})> members);
  Future<void> removeMember(String categoryId, String userId);
}
