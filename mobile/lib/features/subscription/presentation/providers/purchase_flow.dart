import 'dart:async';

import 'package:in_app_purchase/in_app_purchase.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../../core/network/api_exception.dart';
import '../../../auth/domain/entities/auth_tokens.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../../auth/presentation/providers/auth_state.dart';
import '../../data/models/verify_receipt_request.dart';
import '../../data/repositories/subscription_repository_impl.dart';
import '../../data/store/purchase_store.dart';
import '../../subscription_constants.dart';
import 'subscription_providers.dart';

part 'purchase_flow.g.dart';

enum PurchasePhase {
  idle,
  purchasing,
  restoring,
  pending,
  verifying,
  success,
  canceled,
  nothingToRestore,
  failed,
}

class PurchaseState {
  const PurchaseState(this.phase, [this.error]);

  final PurchasePhase phase;
  final Object? error;

  bool get busy =>
      phase == PurchasePhase.purchasing ||
      phase == PurchasePhase.restoring ||
      phase == PurchasePhase.verifying;
}

const _restoreTimeout = Duration(seconds: 10);

const _definitiveCodes = {
  'error.subscription.verification_failed',
  'error.subscription.product_not_allowed',
  'error.subscription.package_not_allowed',
  'error.subscription.already_used',
};

@Riverpod(keepAlive: true)
class PurchaseFlow extends _$PurchaseFlow {
  Future<void> _tail = Future.value();
  Timer? _restoreTimer;

  @override
  PurchaseState build() {
    final store = ref.watch(purchaseStoreProvider);
    ref.onDispose(() => _restoreTimer?.cancel());
    try {
      final sub = store.purchaseStream.listen(
        (purchases) => _tail = _tail.then((_) => _handle(purchases)).catchError(
          (Object e) {
            state = PurchaseState(PurchasePhase.failed, e);
          },
        ),
        onError: (Object e) => state = PurchaseState(PurchasePhase.failed, e),
      );
      ref.onDispose(sub.cancel);
    } catch (_) {}
    return const PurchaseState(PurchasePhase.idle);
  }

  Future<void> buy(ProductDetails product) async {
    state = const PurchaseState(PurchasePhase.purchasing);
    try {
      final started = await ref.read(purchaseStoreProvider).buy(product);
      if (!started) state = const PurchaseState(PurchasePhase.failed);
    } catch (e) {
      state = PurchaseState(PurchasePhase.failed, e);
    }
  }

  Future<void> restore() async {
    state = const PurchaseState(PurchasePhase.restoring);
    try {
      await ref.read(purchaseStoreProvider).restore();
    } catch (e) {
      state = PurchaseState(PurchasePhase.failed, e);
      return;
    }
    _restoreTimer?.cancel();
    _restoreTimer = Timer(_restoreTimeout, () {
      if (state.phase == PurchasePhase.restoring) {
        state = const PurchaseState(PurchasePhase.nothingToRestore);
      }
    });
  }

  Future<void> _handle(List<PurchaseDetails> purchases) async {
    for (final p in purchases) {
      switch (p.status) {
        case PurchaseStatus.pending:
          state = const PurchaseState(PurchasePhase.pending);
        case PurchaseStatus.canceled:
          await _complete(p);
          state = const PurchaseState(PurchasePhase.canceled);
        case PurchaseStatus.error:
          await _complete(p);
          state = PurchaseState(PurchasePhase.failed, p.error?.message);
        case PurchaseStatus.purchased:
        case PurchaseStatus.restored:
          await _verify(p);
      }
    }
  }

  Future<void> _verify(PurchaseDetails p) async {
    if (!SubscriptionConstants.productIds.contains(p.productID)) {
      await _complete(p);
      return;
    }
    if (ref.read(authNotifierProvider) is! AuthStateAuthenticated) return;

    _restoreTimer?.cancel();
    state = const PurchaseState(PurchasePhase.verifying);
    try {
      final sub = await ref
          .read(subscriptionRepositoryProvider)
          .verifyReceipt(
            VerifyReceiptRequest(
              receiptData: p.verificationData.serverVerificationData,
              plan: SubscriptionConstants.planForProductId(p.productID),
              productId: p.productID,
              platform: SubscriptionConstants.platform,
              packageName: SubscriptionConstants.androidPackageName,
            ),
          );
      final auth = ref.read(authNotifierProvider.notifier);
      final access = sub.accessToken;
      final refresh = sub.refreshToken;
      if (access != null && refresh != null) {
        await auth.adoptTokens(
          AuthTokens(accessToken: access, refreshToken: refresh),
        );
      } else {
        await auth.refreshTokens();
      }
      ref.invalidate(userDetailProvider);
      ref.invalidate(subscriptionStatusProvider);
      await _complete(p);
      state = const PurchaseState(PurchasePhase.success);
    } on ApiException catch (e) {
      if (e is BusinessException && _definitiveCodes.contains(e.code)) {
        await _complete(p);
      }
      state = PurchaseState(PurchasePhase.failed, e);
    } catch (e) {
      state = PurchaseState(PurchasePhase.failed, e);
    }
  }

  Future<void> _complete(PurchaseDetails p) async {
    if (!p.pendingCompletePurchase) return;
    try {
      await ref.read(purchaseStoreProvider).complete(p);
    } catch (_) {}
  }
}
