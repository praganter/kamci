import '../entities/category.dart';
import '../entities/member_embedded.dart';

abstract interface class CategoryRepository {
  Future<List<Category>> getAll();
  Future<Category> create(String name);
  Future<Category> update(String id, String name);
  Future<void> delete(String id);
  Future<Category> addMember(String categoryId, String userId, AccessLevel accessLevel);
  Future<Category> addMembersBulk(String categoryId, List<({String userId, AccessLevel accessLevel})> members);
  Future<Category> removeMember(String categoryId, String userId);
}
