import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/storage/secure_storage_service.dart';
import '../../domain/entities/category.dart';
import '../../domain/entities/member_embedded.dart';
import '../../domain/repositories/category_repository.dart';
import '../datasources/category_remote_datasource.dart';
import '../models/category_model.dart';

part 'category_repository_impl.g.dart';

@riverpod
CategoryRepository categoryRepository(Ref ref) => CategoryRepositoryImpl(
  ref.watch(categoryRemoteDatasourceProvider),
  ref.watch(secureStorageServiceProvider),
);

class CategoryRepositoryImpl implements CategoryRepository {
  const CategoryRepositoryImpl(this._datasource, this._storage);
  final CategoryRemoteDatasource _datasource;
  final SecureStorageService _storage;

  @override
  Future<List<Category>> getAll() async {
    final models = await _datasource.getAll();
    return models.map((m) => m.toDomain()).toList();
  }

  @override
  Future<Category> create(String name) async {
    final model = await _datasource.create(name);
    return _withOwnerIdFallback(model.toDomain());
  }

  @override
  Future<Category> update(String id, String name) async {
    final model = await _datasource.update(id, name);
    return _withOwnerIdFallback(model.toDomain());
  }

  /// Backend populates `ownerId` on save/update as of 2026-09-01 — this is a
  /// safety net for rollout/legacy responses that still omit it (creator is
  /// always the owner for save; update never changes ownership).
  Future<Category> _withOwnerIdFallback(Category domain) async {
    if (domain.ownerId.isNotEmpty) return domain;
    final ownerId = await _storage.getUserId() ?? '';
    return Category(
      id: domain.id,
      ownerId: ownerId,
      name: domain.name,
      members: domain.members,
      createdAt: domain.createdAt,
      updatedAt: domain.updatedAt,
    );
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

  @override
  Future<int> migrateAndDelete(String id, String targetCategoryId) =>
      _datasource.migrateAndDelete(id, targetCategoryId);
}
