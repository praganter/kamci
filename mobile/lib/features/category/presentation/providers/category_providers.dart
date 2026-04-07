import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../data/repositories/category_repository_impl.dart';
import '../../domain/entities/category.dart';
import '../../domain/entities/member_embedded.dart';

part 'category_providers.g.dart';

@riverpod
class CategoryList extends _$CategoryList {
  @override
  Future<List<Category>> build() async {
    return ref.watch(categoryRepositoryProvider).getAll();
  }

  Future<void> deleteCategory(String id) async {
    await ref.read(categoryRepositoryProvider).delete(id);
    ref.invalidateSelf();
  }
}

@riverpod
class CategoryDetail extends _$CategoryDetail {
  @override
  Future<Category> build(String id) async {
    final categories = await ref.watch(categoryListProvider.future);
    return categories.firstWhere(
      (c) => c.id == id,
      orElse: () => throw Exception('Kategori bulunamadı'),
    );
  }

  Future<void> addMember(String email, AccessLevel accessLevel) async {
    final category = await future;
    await ref
        .read(categoryRepositoryProvider)
        .addMember(category.id, email, accessLevel);
    ref.invalidate(categoryListProvider);
  }

  Future<void> removeMember(String userId) async {
    final category = await future;
    await ref
        .read(categoryRepositoryProvider)
        .removeMember(category.id, userId);
    ref.invalidate(categoryListProvider);
  }
}

@riverpod
class CategoryForm extends _$CategoryForm {
  @override
  ({bool isLoading, String? error, bool isSuccess, String? createdId})
  build() => (isLoading: false, error: null, isSuccess: false, createdId: null);

  Future<void> submit(String name, {String? categoryId}) async {
    state = (isLoading: true, error: null, isSuccess: false, createdId: null);
    try {
      final repo = ref.read(categoryRepositoryProvider);
      String? createdId;
      if (categoryId != null) {
        await repo.update(categoryId, name);
      } else {
        final created = await repo.create(name);
        createdId = created.id;
      }
      ref.invalidate(categoryListProvider);
      state = (
        isLoading: false,
        error: null,
        isSuccess: true,
        createdId: createdId,
      );
    } catch (e) {
      state = (
        isLoading: false,
        error: e.toString(),
        isSuccess: false,
        createdId: null,
      );
    }
  }
}
