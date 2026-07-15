import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../auth/domain/entities/user_detail.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../../auth/presentation/providers/auth_state.dart';
import '../../../../core/storage/secure_storage_service.dart';
import '../../data/models/verify_receipt_request.dart';
import '../../data/repositories/subscription_repository_impl.dart';
import '../../domain/entities/subscription.dart';

part 'subscription_providers.g.dart';

/// Mevcut subscription durumunu backend'den çeker.
@riverpod
Future<Subscription> subscriptionStatus(Ref ref) =>
    ref.watch(subscriptionRepositoryProvider).getStatus();

/// Son bilinen premium durumunu storage'dan okur.
/// userDetail yüklenirken flicker'ı önler.
@riverpod
Future<bool> cachedPremium(Ref ref) =>
    ref.read(secureStorageServiceProvider).getCachedPremiumStatus();

/// userDetail.premium üzerinden türetilen premium flag.
/// JWT token hemen mevcutsa oradan okur; sonra userDetail ile doğrular.
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

/// Receipt doğrulama aksiyonu.
/// verifyReceipt() sonrası userDetail ve subscriptionStatus invalidate edilir.
@riverpod
class SubscriptionNotifier extends _$SubscriptionNotifier {
  @override
  AsyncValue<Subscription?> build() => const AsyncValue.data(null);

  Future<void> verifyReceipt(VerifyReceiptRequest request) async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      final sub = await ref
          .read(subscriptionRepositoryProvider)
          .verifyReceipt(request);
      if (sub.accessToken != null && sub.refreshToken != null) {
        await ref
            .read(secureStorageServiceProvider)
            .saveTokens(
              accessToken: sub.accessToken!,
              refreshToken: sub.refreshToken!,
            );
      }
      // Force token refresh so the new JWT carries the updated premium claim.
      await ref.read(authNotifierProvider.notifier).refreshTokens();
      ref.invalidate(userDetailProvider);
      ref.invalidate(subscriptionStatusProvider);
      return sub;
    });
  }
}
