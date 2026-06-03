import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../auth/domain/entities/user_detail.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
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
/// API yanıtı gelene kadar storage cache'i kullanır.
@riverpod
bool isPremium(Ref ref) {
  final userDetailAsync = ref.watch(userDetailProvider);

  // Yeni değer gelince storage'a yaz
  ref.listen<AsyncValue<UserDetail>>(userDetailProvider, (_, next) {
    if (next case AsyncData(:final value)) {
      ref.read(secureStorageServiceProvider).savePremiumStatus(value.premium);
    }
  });

  // API yanıtı varsa onu kullan
  if (userDetailAsync.valueOrNull != null) {
    return userDetailAsync.valueOrNull!.premium;
  }
  // Yüklenirken son cache'i kullan
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
