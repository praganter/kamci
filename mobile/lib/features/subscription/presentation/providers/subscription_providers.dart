import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
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

/// userDetail.premium üzerinden türetilen basit premium flag.
/// Login/refresh sonrası otomatik güncellenir.
@riverpod
bool isPremium(Ref ref) {
  final userDetailAsync = ref.watch(userDetailProvider);
  return userDetailAsync.valueOrNull?.premium ?? false;
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
        await ref.read(secureStorageServiceProvider).saveTokens(
          accessToken: sub.accessToken!,
          refreshToken: sub.refreshToken!,
        );
      }
      ref.invalidate(userDetailProvider);
      ref.invalidate(subscriptionStatusProvider);
      return sub;
    });
  }
}
