import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../shared/providers/current_user_provider.dart';
import '../../data/repositories/category_repository_impl.dart';
import '../../domain/entities/category.dart';
import '../../domain/entities/member_embedded.dart';

part 'category_providers.g.dart';

@riverpod
Future<Map<String, AccessLevel>> categoryAccessLevels(Ref ref) async {
  final categories = await ref.watch(categoryListProvider.future);
  final currentUserId = ref.watch(currentUserIdProvider).valueOrNull;

  return {
    for (final c in categories)
      c.id: (c.ownerId == currentUserId)
          ? AccessLevel.admin
          : c.members
                .firstWhere(
                  (m) => m.userId == currentUserId,
                  orElse: () => const MemberEmbedded(
                    userId: '',
                    accessLevel: AccessLevel.read,
                  ),
                )
                .accessLevel,
  };
}

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

  Future<int> transferDebtsAndDelete(
    String categoryId,
    String targetCategoryId,
  ) async {
    final count = await ref
        .read(categoryRepositoryProvider)
        .migrateAndDelete(categoryId, targetCategoryId);
    ref.invalidateSelf();
    return count;
  }
}

@riverpod
class CategoryDetail extends _$CategoryDetail {
  @override
  Future<Category> build(String id) async {
    final categories = await ref.watch(categoryListProvider.future);
    return categories.firstWhere(
      (c) => c.id == id,
      orElse: () => throw Exception('Category not found: $id'),
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
