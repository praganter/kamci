// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'category_providers.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$categoryListHash() => r'1873d90d77e6ac21300afbbc3054dc7c6c9b504f';

/// See also [CategoryList].
@ProviderFor(CategoryList)
final categoryListProvider =
    AutoDisposeAsyncNotifierProvider<CategoryList, List<Category>>.internal(
      CategoryList.new,
      name: r'categoryListProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$categoryListHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$CategoryList = AutoDisposeAsyncNotifier<List<Category>>;
String _$categoryDetailHash() => r'70bb19493c99cf1942e7e5c646bfccf30c49b568';

/// Copied from Dart SDK
class _SystemHash {
  _SystemHash._();

  static int combine(int hash, int value) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + value);
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x0007ffff & hash) << 10));
    return hash ^ (hash >> 6);
  }

  static int finish(int hash) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x03ffffff & hash) << 3));
    // ignore: parameter_assignments
    hash = hash ^ (hash >> 11);
    return 0x1fffffff & (hash + ((0x00003fff & hash) << 15));
  }
}

abstract class _$CategoryDetail
    extends BuildlessAutoDisposeAsyncNotifier<Category> {
  late final String id;

  FutureOr<Category> build(String id);
}

/// See also [CategoryDetail].
@ProviderFor(CategoryDetail)
const categoryDetailProvider = CategoryDetailFamily();

/// See also [CategoryDetail].
class CategoryDetailFamily extends Family<AsyncValue<Category>> {
  /// See also [CategoryDetail].
  const CategoryDetailFamily();

  /// See also [CategoryDetail].
  CategoryDetailProvider call(String id) {
    return CategoryDetailProvider(id);
  }

  @override
  CategoryDetailProvider getProviderOverride(
    covariant CategoryDetailProvider provider,
  ) {
    return call(provider.id);
  }

  static const Iterable<ProviderOrFamily>? _dependencies = null;

  @override
  Iterable<ProviderOrFamily>? get dependencies => _dependencies;

  static const Iterable<ProviderOrFamily>? _allTransitiveDependencies = null;

  @override
  Iterable<ProviderOrFamily>? get allTransitiveDependencies =>
      _allTransitiveDependencies;

  @override
  String? get name => r'categoryDetailProvider';
}

/// See also [CategoryDetail].
class CategoryDetailProvider
    extends AutoDisposeAsyncNotifierProviderImpl<CategoryDetail, Category> {
  /// See also [CategoryDetail].
  CategoryDetailProvider(String id)
    : this._internal(
        () => CategoryDetail()..id = id,
        from: categoryDetailProvider,
        name: r'categoryDetailProvider',
        debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
            ? null
            : _$categoryDetailHash,
        dependencies: CategoryDetailFamily._dependencies,
        allTransitiveDependencies:
            CategoryDetailFamily._allTransitiveDependencies,
        id: id,
      );

  CategoryDetailProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.id,
  }) : super.internal();

  final String id;

  @override
  FutureOr<Category> runNotifierBuild(covariant CategoryDetail notifier) {
    return notifier.build(id);
  }

  @override
  Override overrideWith(CategoryDetail Function() create) {
    return ProviderOverride(
      origin: this,
      override: CategoryDetailProvider._internal(
        () => create()..id = id,
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        id: id,
      ),
    );
  }

  @override
  AutoDisposeAsyncNotifierProviderElement<CategoryDetail, Category>
  createElement() {
    return _CategoryDetailProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is CategoryDetailProvider && other.id == id;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, id.hashCode);

    return _SystemHash.finish(hash);
  }
}

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
mixin CategoryDetailRef on AutoDisposeAsyncNotifierProviderRef<Category> {
  /// The parameter `id` of this provider.
  String get id;
}

class _CategoryDetailProviderElement
    extends AutoDisposeAsyncNotifierProviderElement<CategoryDetail, Category>
    with CategoryDetailRef {
  _CategoryDetailProviderElement(super.provider);

  @override
  String get id => (origin as CategoryDetailProvider).id;
}

String _$categoryFormHash() => r'7bf55d88175fe7a26209a91055e6e55bf7befd40';

/// See also [CategoryForm].
@ProviderFor(CategoryForm)
final categoryFormProvider =
    AutoDisposeNotifierProvider<
      CategoryForm,
      ({bool isLoading, String? error, bool isSuccess, String? createdId})
    >.internal(
      CategoryForm.new,
      name: r'categoryFormProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$categoryFormHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$CategoryForm =
    AutoDisposeNotifier<
      ({bool isLoading, String? error, bool isSuccess, String? createdId})
    >;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
