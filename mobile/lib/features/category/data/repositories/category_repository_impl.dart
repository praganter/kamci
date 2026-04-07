import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../domain/entities/category.dart';
import '../../domain/entities/member_embedded.dart';
import '../../domain/repositories/category_repository.dart';
import '../datasources/category_remote_datasource.dart';
import '../models/category_model.dart';

part 'category_repository_impl.g.dart';

@riverpod
CategoryRepository categoryRepository(Ref ref) =>
    CategoryRepositoryImpl(ref.watch(categoryRemoteDatasourceProvider));

class CategoryRepositoryImpl implements CategoryRepository {
  const CategoryRepositoryImpl(this._datasource);
  final CategoryRemoteDatasource _datasource;

  @override
  Future<List<Category>> getAll() async {
    final models = await _datasource.getAll();
    return models.map((m) => m.toDomain()).toList();
  }

  @override
  Future<Category> create(String name) async {
    final model = await _datasource.create(name);
    return model.toDomain();
  }

  @override
  Future<Category> update(String id, String name) async {
    final model = await _datasource.update(id, name);
    return model.toDomain();
  }

  @override
  Future<void> delete(String id) => _datasource.delete(id);

  @override
  Future<void> addMember(
    String categoryId,
    String email,
    AccessLevel accessLevel,
  ) => _datasource.addMember(categoryId, email, accessLevel);

  @override
  Future<void> addMembersBulk(
    List<String> categoryIds,
    String email,
    AccessLevel accessLevel,
  ) => _datasource.addMembersBulk(categoryIds, email, accessLevel);

  @override
  Future<void> removeMember(String categoryId, String userId) =>
      _datasource.removeMember(categoryId, userId);
}
