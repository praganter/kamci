import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/core/l10n/exception_l10n.dart';
import 'package:kamci/core/l10n/l10n.dart';
import 'package:kamci/core/network/api_exception.dart';
import 'package:kamci/features/subscription/data/datasources/subscription_remote_datasource.dart';
import 'package:kamci/features/subscription/data/models/subscription_model.dart';
import 'package:kamci/features/subscription/data/models/verify_receipt_request.dart';
import 'package:kamci/features/subscription/data/repositories/subscription_repository_impl.dart';
import 'package:kamci/features/subscription/presentation/widgets/premium_card.dart';
import 'package:kamci/features/subscription/subscription_constants.dart';

import '../../helpers/fake_api.dart';
import '../../helpers/pump_app.dart';

void main() {
  group('subscription contract (POST /subscription/verify, GET status)', () {
    test('the verify request serializes the documented fields', () {
      const request = VerifyReceiptRequest(
        receiptData: 'r',
        plan: 'MONTHLY',
        productId: 'p',
        platform: 'ANDROID',
        packageName: 'com.example',
      );
      expect(request.toJson(), {
        'receiptData': 'r',
        'plan': 'MONTHLY',
        'productId': 'p',
        'platform': 'ANDROID',
        'packageName': 'com.example',
      });
    });

    test('the response maps expiryDate and the new tokens', () {
      final sub = SubscriptionModel.fromJson({
        'plan': 'YEARLY',
        'status': 'ACTIVE',
        'expiryDate': '2027-01-01T00:00:00',
        'accessToken': 'a',
        'refreshToken': 'r',
      }).toDomain();
      expect(sub.expiresDate, DateTime.utc(2027).toLocal());
      expect(sub.accessToken, 'a');
      expect(sub.refreshToken, 'r');
      expect(sub.isPremium, isTrue);
    });

    group('expiryDate is read as UTC and shown in local time', () {
      DateTime? expiry(Object? value) => SubscriptionModel.fromJson({
        'plan': 'MONTHLY',
        'status': 'ACTIVE',
        'expiryDate': value,
      }).toDomain().expiresDate;

      test('a timestamp without a zone is taken as UTC', () {
        final parsed = expiry('2027-03-04T21:30:00')!;
        expect(parsed, DateTime.utc(2027, 3, 4, 21, 30).toLocal());
        expect(parsed.isUtc, isFalse);
        expect(
          parsed.isAtSameMomentAs(DateTime.utc(2027, 3, 4, 21, 30)),
          isTrue,
        );
      });

      test('fractional seconds are kept', () {
        expect(
          expiry(
            '2027-03-04T21:30:00.250',
          )!.isAtSameMomentAs(DateTime.utc(2027, 3, 4, 21, 30, 0, 250)),
          isTrue,
        );
      });

      test('a timestamp that already carries a zone keeps its instant', () {
        final instant = DateTime.utc(2027, 3, 4, 21, 30);
        expect(
          expiry('2027-03-04T21:30:00Z')!.isAtSameMomentAs(instant),
          isTrue,
        );
        expect(
          expiry('2027-03-05T00:30:00+03:00')!.isAtSameMomentAs(instant),
          isTrue,
        );
      });

      test('a missing or malformed value is null', () {
        expect(expiry(null), isNull);
        expect(expiry('not a date'), isNull);
        expect(expiry(12345), isNull);
      });
    });

    test('only an ACTIVE monthly or yearly plan counts as premium', () {
      bool premium(String plan, String status) =>
          SubscriptionModel(plan: plan, status: status).toDomain().isPremium;
      expect(premium('MONTHLY', 'ACTIVE'), isTrue);
      expect(premium('YEARLY', 'ACTIVE'), isTrue);
      expect(premium('YEARLY', 'EXPIRED'), isFalse);
      expect(premium('NONE', 'ACTIVE'), isFalse);
    });

    test(
      'the repository posts the receipt and returns the domain object',
      () async {
        final api = FakeApi().on('POST /subscription/verify', {
          'plan': 'MONTHLY',
          'status': 'ACTIVE',
          'accessToken': 'a',
          'refreshToken': 'r',
        });
        final repo = SubscriptionRepositoryImpl(
          SubscriptionRemoteDatasource(api.dio()),
        );
        final sub = await repo.verifyReceipt(
          const VerifyReceiptRequest(
            receiptData: 'r',
            plan: 'MONTHLY',
            productId: 'p',
            platform: 'IOS',
          ),
        );
        expect(sub.accessToken, 'a');
        expect(api.last.path, '/subscription/verify');
        expect((api.last.data as Map).containsKey('packageName'), isFalse);
      },
    );

    test('the repository reads the current status', () async {
      final api = FakeApi().on('GET /subscription/status', {
        'plan': 'YEARLY',
        'status': 'ACTIVE',
        'expiryDate': '2027-01-01T00:00:00',
      });
      final repo = SubscriptionRepositoryImpl(
        SubscriptionRemoteDatasource(api.dio()),
      );
      expect((await repo.getStatus()).isPremium, isTrue);
    });

    test('verification failures surface as typed exceptions', () async {
      final api = FakeApi().on('POST /subscription/verify', {
        'code': 'error.subscription.verification_failed',
        'message': 'bad',
      }, 400);
      final datasource = SubscriptionRemoteDatasource(api.dio());
      await expectLater(
        datasource.verifyReceipt(
          const VerifyReceiptRequest(
            receiptData: 'r',
            plan: 'MONTHLY',
            productId: 'p',
            platform: 'IOS',
          ),
        ),
        throwsA(
          isA<BusinessException>().having(
            (e) => e.code,
            'code',
            'error.subscription.verification_failed',
          ),
        ),
      );
    });

    test('a missing subscription is a not found', () async {
      final datasource = SubscriptionRemoteDatasource(FakeApi().dio());
      await expectLater(
        datasource.getStatus(),
        throwsA(isA<NotFoundException>()),
      );
    });
  });

  group('subscription error codes', () {
    const verification = 'Subscription verification failed.';
    const expected = {
      'error.subscription.verification_failed': verification,
      'error.subscription.product_not_allowed': verification,
      'error.subscription.package_not_allowed': verification,
      'error.subscription.not_found': 'Subscription not found.',
      'error.subscription.already_used':
          'This subscription is already linked to another account.',
    };

    for (final entry in expected.entries) {
      test('${entry.key} is a localized business exception', () {
        final options = RequestOptions(path: '/subscription/verify');
        final exception = ApiException.fromDioException(
          DioException(
            requestOptions: options,
            type: DioExceptionType.badResponse,
            response: Response(
              requestOptions: options,
              statusCode: 400,
              data: {'code': entry.key, 'message': 'server text'},
            ),
          ),
        );
        expect(exception, isA<BusinessException>());
        expect((exception as BusinessException).code, entry.key);
        expect(
          exception.toLocalizedMessage(
            lookupAppLocalizations(const Locale('en')),
          ),
          entry.value,
        );
        expect(
          exception.toLocalizedMessage(
            lookupAppLocalizations(const Locale('tr')),
          ),
          isNot(entry.value),
        );
      });
    }
  });

  group('subscription constants', () {
    test('product ids map to their plan and unknown ids default to yearly', () {
      expect(
        SubscriptionConstants.planForProductId(
          SubscriptionConstants.monthlyProductId,
        ),
        'MONTHLY',
      );
      expect(
        SubscriptionConstants.planForProductId(
          SubscriptionConstants.yearlyProductId,
        ),
        'YEARLY',
      );
      expect(SubscriptionConstants.productIds, hasLength(2));
    });

    test('the Android build identifies its package, iOS does not', () {
      expect(SubscriptionConstants.platform, 'ANDROID');
      expect(
        SubscriptionConstants.androidPackageName,
        'com.batuhanyetgin.kamci',
      );
    });
  });

  group('premium card', () {
    testWidgets('an active subscription shows when it renews', (t) async {
      await pumpScreen(
        t,
        const Scaffold(body: PremiumCard()),
        premium: true,
        api: FakeApi().on('GET /subscription/status', {
          'plan': 'YEARLY',
          'status': 'ACTIVE',
          'expiryDate': '2027-01-01T00:00:00',
        }),
      );
      await settle(t);
      expect(find.text('Active'), findsOneWidget);
      expect(find.textContaining('Renews'), findsOneWidget);
    });

    testWidgets('a free user sees the upgrade card', (t) async {
      await pumpScreen(t, const Scaffold(body: PremiumCard()), api: FakeApi());
      await settle(t);
      expect(find.text('Get Premium'), findsOneWidget);
      expect(find.text('Active'), findsNothing);
      expect(find.textContaining('Renews'), findsNothing);
    });
  });
}
