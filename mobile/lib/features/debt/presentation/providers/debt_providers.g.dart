// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'debt_providers.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$currencyListHash() => r'ec624278587af5026f0aa2b7aa5e44b93b3354b8';

/// See also [currencyList].
@ProviderFor(currencyList)
final currencyListProvider =
    AutoDisposeFutureProvider<List<CurrencyModel>>.internal(
      currencyList,
      name: r'currencyListProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$currencyListHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef CurrencyListRef = AutoDisposeFutureProviderRef<List<CurrencyModel>>;
String _$debtListHash() => r'4d14051afbd2c64962ee59f7484d2e80f83a3e0d';

/// See also [DebtList].
@ProviderFor(DebtList)
final debtListProvider =
    AutoDisposeAsyncNotifierProvider<DebtList, List<Debt>>.internal(
      DebtList.new,
      name: r'debtListProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$debtListHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$DebtList = AutoDisposeAsyncNotifier<List<Debt>>;
String _$debtDetailHash() => r'2b3933efd17a64fa8ba2711f3673bb4f391c1221';

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

abstract class _$DebtDetail extends BuildlessAutoDisposeAsyncNotifier<Debt> {
  late final String id;

  FutureOr<Debt> build(String id);
}

/// See also [DebtDetail].
@ProviderFor(DebtDetail)
const debtDetailProvider = DebtDetailFamily();

/// See also [DebtDetail].
class DebtDetailFamily extends Family<AsyncValue<Debt>> {
  /// See also [DebtDetail].
  const DebtDetailFamily();

  /// See also [DebtDetail].
  DebtDetailProvider call(String id) {
    return DebtDetailProvider(id);
  }

  @override
  DebtDetailProvider getProviderOverride(
    covariant DebtDetailProvider provider,
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
  String? get name => r'debtDetailProvider';
}

/// See also [DebtDetail].
class DebtDetailProvider
    extends AutoDisposeAsyncNotifierProviderImpl<DebtDetail, Debt> {
  /// See also [DebtDetail].
  DebtDetailProvider(String id)
    : this._internal(
        () => DebtDetail()..id = id,
        from: debtDetailProvider,
        name: r'debtDetailProvider',
        debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
            ? null
            : _$debtDetailHash,
        dependencies: DebtDetailFamily._dependencies,
        allTransitiveDependencies: DebtDetailFamily._allTransitiveDependencies,
        id: id,
      );

  DebtDetailProvider._internal(
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
  FutureOr<Debt> runNotifierBuild(covariant DebtDetail notifier) {
    return notifier.build(id);
  }

  @override
  Override overrideWith(DebtDetail Function() create) {
    return ProviderOverride(
      origin: this,
      override: DebtDetailProvider._internal(
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
  AutoDisposeAsyncNotifierProviderElement<DebtDetail, Debt> createElement() {
    return _DebtDetailProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is DebtDetailProvider && other.id == id;
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
mixin DebtDetailRef on AutoDisposeAsyncNotifierProviderRef<Debt> {
  /// The parameter `id` of this provider.
  String get id;
}

class _DebtDetailProviderElement
    extends AutoDisposeAsyncNotifierProviderElement<DebtDetail, Debt>
    with DebtDetailRef {
  _DebtDetailProviderElement(super.provider);

  @override
  String get id => (origin as DebtDetailProvider).id;
}

String _$debtGroupedHash() => r'b50172a1849f288cdb3bf82293a265f502a893dd';

/// See also [DebtGrouped].
@ProviderFor(DebtGrouped)
final debtGroupedProvider =
    AutoDisposeAsyncNotifierProvider<
      DebtGrouped,
      List<CategoryWithDebts>
    >.internal(
      DebtGrouped.new,
      name: r'debtGroupedProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$debtGroupedHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$DebtGrouped = AutoDisposeAsyncNotifier<List<CategoryWithDebts>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
