import 'package:dio/dio.dart';
import 'package:dt_mobile/core/network/api_exception.dart';
import 'package:dt_mobile/features/category/data/datasources/category_remote_datasource.dart';
import 'package:dt_mobile/features/category/domain/entities/member_embedded.dart';
import 'package:dt_mobile/features/debt/data/datasources/debt_remote_datasource.dart';
import 'package:dt_mobile/features/debt/data/models/create_debt_request.dart';
import 'package:dt_mobile/features/debt/data/models/debt_model.dart';
import 'package:flutter_test/flutter_test.dart';

import 'helpers/test_credentials.dart';

/// C2 turu — backend (debt-service v0.0.20) artık 5 stabil `ErrorResponse.code`
/// dönüyor. Bu suite mobile `ApiException.fromDioException` katmanının onları
/// doğru tip + `code`'a çevirdiğini CANLI backend'e karşı doğrular.
///
/// PREMIUM bir hesap gerekir (cannot_add_self kategori paylaşımını, dolayısıyla
/// premium'u zorunlu kılıyor; ayrıca free 2-kategori limiti name_conflict'i
/// gölgeler) — kalıcı "owner" hesabı TEST_A_*. Çalıştırma: bkz.
/// `test/integration/run.sh`
const _baseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://localhost:8080',
);
const _email = TestAccounts.emailA;
const _password = TestAccounts.passwordA;

void main() {
  if (!TestAccounts.hasA) {
    test('C2 contract (skipped)', () {}, skip: TestAccounts.missingMessage);
    return;
  }

  late Dio dio;
  late DebtRemoteDatasource debtDs;
  late CategoryRemoteDatasource catDs;
  late String categoryId;

  Dio buildDio() => Dio(BaseOptions(
        baseUrl: _baseUrl,
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 30),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'Accept-Language': 'tr',
        },
      ));

  setUpAll(() async {
    final login = await buildDio()
        .post('/auth/login', data: {'email': _email, 'password': _password});
    final token = login.data['token'] as String;
    dio = buildDio()..options.headers['Authorization'] = 'Bearer $token';
    debtDs = DebtRemoteDatasource(dio);
    catDs = CategoryRemoteDatasource(dio);

    // Kendi bıraktığı test kategorilerini temizle (yabancılara dokunma).
    for (final c in await catDs.getAll()) {
      if (c.name.startsWith('__C2 ') || c.name.startsWith('C2 ')) {
        await catDs.delete(c.id).catchError((_) {});
      }
    }
    final cat = await catDs.create('__C2 ${DateTime.now().millisecondsSinceEpoch}__');
    categoryId = cat.id;
  });

  tearDownAll(() async {
    await catDs.delete(categoryId).catchError((_) {});
  });

  CreateDebtRequest oneTime({double amount = 250.0}) => CreateDebtRequest(
        name: 'C2 Borç',
        firstPaymentDate: '2026-01-15T00:00:00',
        paymentPeriodType: PaymentPeriodTypeJson.oneTime,
        amount: amount,
        currency: 'TRY',
        categoryId: categoryId,
      );

  Matcher throwsBusinessCode(String code) => throwsA(
        isA<BusinessException>().having((e) => e.code, 'code', code),
      );

  group('debt', () {
    test('ONE_TIME ikinci ödeme → error.debt.already_completed (409)', () async {
      final debt = await debtDs.create(oneTime(amount: 100.0));
      addTearDown(() => debtDs.delete(debt.id).catchError((_) {}));

      await debtDs.pay(debt.id, amountPaid: 100.0);
      await expectLater(
        debtDs.pay(debt.id, amountPaid: 100.0),
        throwsBusinessCode('error.debt.already_completed'),
      );
    });

    test('kalan tutarı aşan ödeme → error.debt.overpayment (400)', () async {
      final debt = await debtDs.create(oneTime(amount: 250.0));
      addTearDown(() => debtDs.delete(debt.id).catchError((_) {}));

      await expectLater(
        debtDs.pay(debt.id, amountPaid: 999999.0),
        throwsBusinessCode('error.debt.overpayment'),
      );
    });
  });

  group('category', () {
    test('aynı isimde kategori → error.category.name_conflict (409)', () async {
      final name = 'C2 Çakışma ${DateTime.now().millisecondsSinceEpoch}';
      final first = await catDs.create(name);
      addTearDown(() => catDs.delete(first.id).catchError((_) {}));

      await expectLater(
        catDs.create(name),
        throwsBusinessCode('error.category.name_conflict'),
      );
    });

    test('owner kendini üye ekliyor → error.category.cannot_add_self (409)', () async {
      await expectLater(
        catDs.addMember(categoryId, _email, AccessLevel.read),
        throwsBusinessCode('error.category.cannot_add_self'),
      );
    });

    test('var olmayan üyeyi silme → error.category.member_not_found (404)', () async {
      await expectLater(
        catDs.removeMember(categoryId, '000000000000000000000000'),
        throwsBusinessCode('error.category.member_not_found'),
      );
    });
  });
}
