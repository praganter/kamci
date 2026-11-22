import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../auth/domain/entities/user_detail.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../../auth/presentation/providers/auth_state.dart';
import '../../../../core/storage/secure_storage_service.dart';
import '../../data/repositories/subscription_repository_impl.dart';
import '../../domain/entities/subscription.dart';

part 'subscription_providers.g.dart';

@riverpod
Future<Subscription> subscriptionStatus(Ref ref) =>
    ref.watch(subscriptionRepositoryProvider).getStatus();

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

  if (userDetailAsync.valueOrNull != null) {
    return userDetailAsync.valueOrNull!.premium;
  }

  if (authState case AuthStateAuthenticated(:final tokens)) {
    return tokens.isPremium;
  }

  return ref.watch(cachedPremiumProvider).valueOrNull ?? false;
}
