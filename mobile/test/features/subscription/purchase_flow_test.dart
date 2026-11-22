import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:in_app_purchase/in_app_purchase.dart';
import 'package:kamci/core/network/api_exception.dart';
import 'package:kamci/core/network/dio_client.dart';
import 'package:kamci/core/storage/secure_storage_service.dart';
import 'package:kamci/features/auth/domain/entities/auth_tokens.dart';
import 'package:kamci/features/auth/presentation/providers/auth_provider.dart';
import 'package:kamci/features/auth/presentation/providers/auth_state.dart';
import 'package:kamci/features/subscription/data/store/purchase_store.dart';
import 'package:kamci/features/subscription/presentation/providers/purchase_flow.dart';
import 'package:kamci/features/subscription/subscription_constants.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/fake_store.dart';
import '../../helpers/fakes.dart';

class _Authed extends AuthNotifier {
  @override
  AuthState build() => AuthState.authenticated(
    AuthTokens(accessToken: fakeJwt(), refreshToken: 'old-refresh'),
  );
}

class _Anonymous extends AuthNotifier {
  @override
  AuthState build() => const AuthState.unauthenticated();
}

class _ThrowingStore extends FakePurchaseStore {
  @override
  Stream<List<PurchaseDetails>> get purchaseStream =>
      throw UnsupportedError('no store on this platform');

  @override
  Future<void> restore() async => throw StateError('store down');
}

final _verified = {
  'plan': 'YEARLY',
  'status': 'ACTIVE',
  'expiryDate': '2027-01-01T00:00:00',
  'accessToken': fakeJwt(premium: true),
  'refreshToken': 'new-refresh',
};

class _Setup {
  _Setup({bool authenticated = true}) {
    container = ProviderContainer(
      overrides: [
        purchaseStoreProvider.overrideWithValue(store),
        dioClientProvider.overrideWithValue(api.dio()),
        refreshDioProvider.overrideWithValue(api.dio()),
        secureStorageServiceProvider.overrideWithValue(storage),
        authNotifierProvider.overrideWith(
          () => authenticated ? _Authed() : _Anonymous(),
        ),
      ],
    );
    addTearDown(container.dispose);
    container.read(purchaseFlowProvider);
  }

  final api = FakeApi();
  final storage = FakeStorage();
  final store = FakePurchaseStore();
  late final ProviderContainer container;

  PurchaseState get state => container.read(purchaseFlowProvider);

  RecordedRequest get verifyRequest =>
      api.requests.firstWhere((r) => r.path == '/subscription/verify');

  Future<void> until(PurchasePhase phase) async {
    for (var i = 0; i < 200 && state.phase != phase; i++) {
      await Future<void>.delayed(const Duration(milliseconds: 5));
    }
    expect(state.phase, phase);
  }
}

final _yearly = SubscriptionConstants.yearlyProductId;

void main() {
  group('purchase → backend verify', () {
    test(
      'a purchase is verified with the documented body and tokens adopted',
      () async {
        final s = _Setup()..api.on('POST /subscription/verify', _verified);
        s.store.emit(fakePurchase(_yearly, PurchaseStatus.purchased));
        await s.until(PurchasePhase.success);

        expect(s.verifyRequest.data, {
          'receiptData': 'receipt-token-1',
          'plan': 'YEARLY',
          'productId': _yearly,
          'platform': SubscriptionConstants.platform,
          'packageName': SubscriptionConstants.androidPackageName,
        });
        expect(s.storage.values['access'], _verified['accessToken']);
        expect(s.storage.values['refresh'], 'new-refresh');
        final auth = s.container.read(authNotifierProvider);
        expect(auth, isA<AuthStateAuthenticated>());
        expect((auth as AuthStateAuthenticated).tokens.isPremium, isTrue);
        expect(s.store.completed, hasLength(1));
      },
    );

    test('a monthly product is sent as the MONTHLY plan', () async {
      final s = _Setup()..api.on('POST /subscription/verify', _verified);
      s.store.emit(
        fakePurchase(
          SubscriptionConstants.monthlyProductId,
          PurchaseStatus.restored,
        ),
      );
      await s.until(PurchasePhase.success);
      expect((s.verifyRequest.data as Map)['plan'], 'MONTHLY');
    });

    test(
      'a verification the backend rejects is finished, not retried',
      () async {
        final s = _Setup()
          ..api.on('POST /subscription/verify', {
            'code': 'error.subscription.verification_failed',
            'message': 'bad receipt',
          }, 400);
        s.store.emit(fakePurchase(_yearly, PurchaseStatus.purchased));
        await s.until(PurchasePhase.failed);
        expect(s.state.error, isA<BusinessException>());
        expect(s.store.completed, hasLength(1));
      },
    );

    test(
      'a receipt owned by another account is finished as rejected',
      () async {
        final s = _Setup()
          ..api.on('POST /subscription/verify', {
            'code': 'error.subscription.already_used',
            'message': 'used',
          }, 409);
        s.store.emit(fakePurchase(_yearly, PurchaseStatus.purchased));
        await s.until(PurchasePhase.failed);
        expect(s.store.completed, hasLength(1));
      },
    );

    test(
      'a server error leaves the purchase unfinished so it is retried',
      () async {
        final s = _Setup()
          ..api.on('POST /subscription/verify', {'message': 'boom'}, 500);
        s.store.emit(fakePurchase(_yearly, PurchaseStatus.purchased));
        await s.until(PurchasePhase.failed);
        expect(s.state.error, isA<ServerException>());
        expect(s.store.completed, isEmpty);
      },
    );

    test('nothing is verified or finished while signed out', () async {
      final s = _Setup(authenticated: false);
      s.store.emit(fakePurchase(_yearly, PurchaseStatus.purchased));
      await Future<void>.delayed(const Duration(milliseconds: 50));
      expect(s.api.requests, isEmpty);
      expect(s.store.completed, isEmpty);
      expect(s.state.phase, PurchasePhase.idle);
    });

    test(
      'an unknown product is finished without calling the backend',
      () async {
        final s = _Setup();
        s.store.emit(
          fakePurchase('some.other.product', PurchaseStatus.purchased),
        );
        await Future<void>.delayed(const Duration(milliseconds: 50));
        expect(s.api.requests, isEmpty);
        expect(s.store.completed, hasLength(1));
      },
    );

    test('a response without tokens falls back to a token refresh', () async {
      final s = _Setup()
        ..api.on('POST /subscription/verify', {
          'plan': 'YEARLY',
          'status': 'ACTIVE',
        })
        ..api.on('POST /auth/refresh-token', {
          'token': fakeJwt(premium: true),
          'refreshToken': 'refreshed',
        });
      s.storage.values['refresh'] = 'old-refresh';
      s.store.emit(fakePurchase(_yearly, PurchaseStatus.purchased));
      await s.until(PurchasePhase.success);
      expect(
        s.api.requests.map((r) => r.path),
        contains('/auth/refresh-token'),
      );
    });
  });

  group('store outcomes', () {
    test(
      'a cancelled purchase is finished and reported as cancelled',
      () async {
        final s = _Setup();
        s.store.emit(fakePurchase(_yearly, PurchaseStatus.canceled));
        await s.until(PurchasePhase.canceled);
        expect(s.store.completed, hasLength(1));
        expect(s.api.requests, isEmpty);
      },
    );

    test('a store error is finished and reported as failed', () async {
      final s = _Setup();
      s.store.emit(
        fakePurchase(
          _yearly,
          PurchaseStatus.error,
          error: IAPError(source: 'test', code: 'x', message: 'store said no'),
        ),
      );
      await s.until(PurchasePhase.failed);
      expect(s.state.error, 'store said no');
      expect(s.store.completed, hasLength(1));
    });

    test('a pending purchase waits and is not finished', () async {
      final s = _Setup();
      s.store.emit(fakePurchase(_yearly, PurchaseStatus.pending));
      await s.until(PurchasePhase.pending);
      expect(s.store.completed, isEmpty);
    });

    test('a purchase approved later is verified when it arrives', () async {
      final s = _Setup()..api.on('POST /subscription/verify', _verified);
      s.store.emit(fakePurchase(_yearly, PurchaseStatus.pending));
      await s.until(PurchasePhase.pending);
      s.store.emit(fakePurchase(_yearly, PurchaseStatus.purchased));
      await s.until(PurchasePhase.success);
    });
  });

  group('starting a purchase', () {
    test('buy hands the product to the store', () async {
      final s = _Setup();
      final product = s.store.products.first;
      await s.container.read(purchaseFlowProvider.notifier).buy(product);
      expect(s.store.bought, [product.id]);
      expect(s.state.phase, PurchasePhase.purchasing);
      expect(s.state.busy, isTrue);
    });

    test('buy fails when the store refuses to start it', () async {
      final s = _Setup();
      s.store.buyResult = false;
      await s.container
          .read(purchaseFlowProvider.notifier)
          .buy(s.store.products.first);
      expect(s.state.phase, PurchasePhase.failed);
    });

    test('buy fails when the store throws', () async {
      final s = _Setup();
      s.store.buyError = StateError('already owned');
      await s.container
          .read(purchaseFlowProvider.notifier)
          .buy(s.store.products.first);
      expect(s.state.phase, PurchasePhase.failed);
      expect(s.state.error, isA<StateError>());
    });
  });

  group('restore', () {
    testWidgets('with nothing to restore it gives up after the timeout', (
      tester,
    ) async {
      final s = _Setup();
      await s.container.read(purchaseFlowProvider.notifier).restore();
      expect(s.store.restores, 1);
      expect(s.state.phase, PurchasePhase.restoring);
      await tester.pump(const Duration(seconds: 11));
      expect(s.state.phase, PurchasePhase.nothingToRestore);
    });

    testWidgets('a restored purchase is verified and cancels the timeout', (
      tester,
    ) async {
      final s = _Setup()..api.on('POST /subscription/verify', _verified);
      await s.container.read(purchaseFlowProvider.notifier).restore();
      s.store.emit(fakePurchase(_yearly, PurchaseStatus.restored));
      for (var i = 0; i < 10; i++) {
        await tester.pump(const Duration(milliseconds: 100));
      }
      expect(s.state.phase, PurchasePhase.success);
      await tester.pump(const Duration(seconds: 11));
      expect(s.state.phase, PurchasePhase.success);
    });

    test('a failing restore is reported as failed', () async {
      final c = ProviderContainer(
        overrides: [purchaseStoreProvider.overrideWithValue(_ThrowingStore())],
      );
      addTearDown(c.dispose);
      await c.read(purchaseFlowProvider.notifier).restore();
      expect(c.read(purchaseFlowProvider).phase, PurchasePhase.failed);
    });
  });

  test('without a usable store the flow stays idle', () {
    final c = ProviderContainer(
      overrides: [purchaseStoreProvider.overrideWithValue(_ThrowingStore())],
    );
    addTearDown(c.dispose);
    expect(c.read(purchaseFlowProvider).phase, PurchasePhase.idle);
  });
}
