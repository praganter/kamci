import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:purchases_flutter/purchases_flutter.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../auth/domain/entities/user_detail.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../../auth/presentation/providers/auth_state.dart';
import '../../../../core/storage/secure_storage_service.dart';
import '../../subscription_constants.dart';

part 'subscription_providers.g.dart';

/// RevenueCat CustomerInfo'su. RevenueCat kullanıcısı oturumdaki backend
/// kullanıcısıyla eşlenir (appUserID = JWT userId) — webhook premium'u bu
/// id ile backend'e yazar. RevenueCat yapılandırılmamışsa hep null.
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
        // Aynı id ile logIn SDK'da no-op; token refresh'lerde tekrar çağrılır.
        if (userId != null) {
          state = (await Purchases.logIn(userId)).customerInfo;
        }
      } else if (auth is AuthStateUnauthenticated) {
        if (!await Purchases.isAnonymous) state = await Purchases.logOut();
      }
    } catch (_) {
      // Offline / SDK hatası — premium backend'den de okunuyor.
    }
  }
}

/// Son bilinen premium durumunu storage'dan okur.
/// userDetail yüklenirken flicker'ı önler.
@riverpod
Future<bool> cachedPremium(Ref ref) =>
    ref.read(secureStorageServiceProvider).getCachedPremiumStatus();

/// RevenueCat entitlement'ı veya backend'in premium bayrağı.
/// Backend, RevenueCat webhook'u ile birkaç saniye gecikmeli güncellenir;
/// entitlement satın alma anında aktif olduğundan önce o okunur.
@riverpod
bool isPremium(Ref ref) {
  final authState = ref.watch(authNotifierProvider);
  final userDetailAsync = ref.watch(userDetailProvider);

  // userDetail yüklenince storage'a kaydet
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

  // userDetail yanıtı varsa en güncel değer
  if (userDetailAsync.valueOrNull != null) {
    return userDetailAsync.valueOrNull!.premium;
  }

  // userDetail yüklenirken JWT'deki premium claim'ini kullan (anında mevcut)
  if (authState case AuthStateAuthenticated(:final tokens)) {
    return tokens.isPremium;
  }

  // Authenticated değilse son storage cache'i
  return ref.watch(cachedPremiumProvider).valueOrNull ?? false;
}
