import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:purchases_flutter/purchases_flutter.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../auth/domain/entities/user_detail.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../../auth/presentation/providers/auth_state.dart';
import '../../../../core/storage/secure_storage_service.dart';
import '../../subscription_constants.dart';

part 'subscription_providers.g.dart';

@Riverpod(keepAlive: true)
class RevenueCatCustomer extends _$RevenueCatCustomer {
  @override
  CustomerInfo? build() {
    if (SubscriptionConstants.revenueCatApiKey.isEmpty) return null;
    void listener(CustomerInfo info) => state = info;
    Purchases.addCustomerInfoUpdateListener(listener);
    ref.onDispose(() => Purchases.removeCustomerInfoUpdateListener(listener));
    ref.listen(authNotifierProvider, (_, next) => _syncUser(next),
        fireImmediately: true);
    return null;
  }

  Future<void> _syncUser(AuthState auth) async {
    try {
      if (auth case AuthStateAuthenticated(:final tokens)) {
        final userId = tokens.userId;
        if (userId != null) {
          state = (await Purchases.logIn(userId)).customerInfo;
        }
      } else if (auth is AuthStateUnauthenticated) {
        if (!await Purchases.isAnonymous) state = await Purchases.logOut();
      }
    } catch (_) {
    }
  }
}

@riverpod
Future<bool> cachedPremium(Ref ref) =>
    ref.read(secureStorageServiceProvider).getCachedPremiumStatus();

@riverpod
bool isPremium(Ref ref) {
  final authState = ref.watch(authNotifierProvider);
  final userDetailAsync = ref.watch(userDetailProvider);

  ref.listen<AsyncValue<UserDetail>>(userDetailProvider, (_, next) {
    if (next case AsyncData(:final value)) {
      ref.read(secureStorageServiceProvider).savePremiumStatus(value.premium);
    }
  });

  final entitlementActive =
      ref
          .watch(revenueCatCustomerProvider)
          ?.entitlements
          .active
          .containsKey(SubscriptionConstants.entitlementId) ??
      false;
  if (entitlementActive) return true;

  if (userDetailAsync.valueOrNull != null) {
    return userDetailAsync.valueOrNull!.premium;
  }

  if (authState case AuthStateAuthenticated(:final tokens)) {
    return tokens.isPremium;
  }

  return ref.watch(cachedPremiumProvider).valueOrNull ?? false;
}
