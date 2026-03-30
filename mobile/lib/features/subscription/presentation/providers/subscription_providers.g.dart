// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'subscription_providers.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$subscriptionStatusHash() =>
    r'03940533c2cb578c902f02778d7eb12eebca2fe5';

/// Mevcut subscription durumunu backend'den çeker.
///
/// Copied from [subscriptionStatus].
@ProviderFor(subscriptionStatus)
final subscriptionStatusProvider =
    AutoDisposeFutureProvider<Subscription>.internal(
      subscriptionStatus,
      name: r'subscriptionStatusProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$subscriptionStatusHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef SubscriptionStatusRef = AutoDisposeFutureProviderRef<Subscription>;
String _$isPremiumHash() => r'97746eb35123698e542275095c11b520a18d8441';

/// userDetail.premium üzerinden türetilen basit premium flag.
/// Login/refresh sonrası otomatik güncellenir.
///
/// Copied from [isPremium].
@ProviderFor(isPremium)
final isPremiumProvider = AutoDisposeProvider<bool>.internal(
  isPremium,
  name: r'isPremiumProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$isPremiumHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef IsPremiumRef = AutoDisposeProviderRef<bool>;
String _$subscriptionNotifierHash() =>
    r'4e60ef1f287c32e6be9d53f3994ea931357e7862';

/// Receipt doğrulama aksiyonu.
/// verifyReceipt() sonrası userDetail ve subscriptionStatus invalidate edilir.
///
/// Copied from [SubscriptionNotifier].
@ProviderFor(SubscriptionNotifier)
final subscriptionNotifierProvider =
    AutoDisposeNotifierProvider<
      SubscriptionNotifier,
      AsyncValue<Subscription?>
    >.internal(
      SubscriptionNotifier.new,
      name: r'subscriptionNotifierProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$subscriptionNotifierHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$SubscriptionNotifier = AutoDisposeNotifier<AsyncValue<Subscription?>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
