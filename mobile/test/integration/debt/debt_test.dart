import 'package:dt_mobile/core/network/api_exception.dart';
import 'package:dt_mobile/features/category/data/datasources/category_remote_datasource.dart';
import 'package:dt_mobile/features/debt/data/datasources/debt_remote_datasource.dart';
import 'package:dt_mobile/features/debt/data/models/create_debt_request.dart';
import 'package:dt_mobile/features/debt/data/models/debt_model.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/test_http_client.dart';

void main() {
  late DebtRemoteDatasource ds;
  late CategoryRemoteDatasource categoryDs;
  late String testCategoryId;

  // Backend ISO 8601 formatı + currency zorunlu
  CreateDebtRequest baseRequest() => CreateDebtRequest(
    name: 'Test Borcu',
    firstPaymentDate: '2026-04-30T00:00:00',
    paymentPeriodType: PaymentPeriodTypeJson.oneTime,
    amount: 250.0,
    currency: 'TRY',
    categoryId: testCategoryId,
  );

  setUpAll(() async {
    await TestHttpClient.setUp();
    ds = DebtRemoteDatasource(TestHttpClient.authed);
    categoryDs = CategoryRemoteDatasource(TestHttpClient.authed);

    final cat = await categoryDs.create('__Test Borç Kategorisi__');
    testCategoryId = cat.id;
  });

  tearDownAll(() async {
    await categoryDs.delete(testCategoryId).catchError((_) {});
  });

  group('create', () {
    late String createdDebtId;

    tearDown(() async {
      if (createdDebtId.isNotEmpty) {
        await ds.delete(createdDebtId).catchError((_) {});
      }
    });

    test('borç oluşturur ve alanlar doğru', () async {
      createdDebtId = '';
      final result = await ds.create(baseRequest());
      createdDebtId = result.id;

      expect(result.id, isNotEmpty);
      expect(result.name, equals('Test Borcu'));
      expect(result.amount, equals(250.0));
      expect(result.status, equals(DebtStatusJson.active));
      expect(result.paymentStatus, equals(PaymentStatusJson.unpaid));
    });
  });

  group('getGrouped', () {
    late String createdDebtId;

    setUp(() async {
      createdDebtId = '';
      final debt = await ds.create(baseRequest());
      createdDebtId = debt.id;
    });

    tearDown(() async {
      if (createdDebtId.isNotEmpty) {
        await ds.delete(createdDebtId).catchError((_) {});
      }
    });

    test('oluşturulan borç gruplu listede görünür', () async {
      final groups = await ds.getGrouped(month: '2026-04', isOwner: true);
      expect(groups, isNotEmpty);

      final allDebtIds = groups.expand((g) => g.debts.map((d) => d.id));
      expect(allDebtIds.contains(createdDebtId), isTrue);
    });
  });

  group('getById', () {
    late String createdDebtId;

    setUp(() async {
      createdDebtId = '';
      final debt = await ds.create(baseRequest());
      createdDebtId = debt.id;
    });

    tearDown(() async {
      if (createdDebtId.isNotEmpty) {
        await ds.delete(createdDebtId).catchError((_) {});
      }
    });

    test('ID ile doğru borcu getirir', () async {
      final result = await ds.getById(createdDebtId);
      expect(result.id, equals(createdDebtId));
      expect(result.name, equals('Test Borcu'));
    });

    test('var olmayan ID ile NotFoundException fırlar', () async {
      expect(
        () => ds.getById('000000000000000000000000'),
        throwsA(isA<NotFoundException>()),
      );
    });
  });

  group('pay', () {
    late String createdDebtId;

    setUp(() async {
      createdDebtId = '';
      final debt = await ds.create(baseRequest());
      createdDebtId = debt.id;
    });

    tearDown(() async {
      if (createdDebtId.isNotEmpty) {
        await ds.delete(createdDebtId).catchError((_) {});
      }
    });

    // ONE_TIME /debt/pay 500 (periodEnd NPE) — backend'de düzeltildi ama
    // 198.51.100.10:30080 gateway'ine henüz DEPLOY EDİLMEDİ (2026-09-01 doğrulandı,
    // hâlâ 500). Deploy sonrası skip kaldırılmalı.
    test(
      'borcu öder ve paymentStatus değişir',
      () async {
        final result = await ds.pay(createdDebtId);
        expect(result.id, equals(createdDebtId));
        expect(
          result.paymentStatus,
          anyOf(
            equals(PaymentStatusJson.paid),
            equals(PaymentStatusJson.partiallyPaid),
          ),
        );
      },
      skip: 'Backend /debt/pay 500 fix henüz deploy edilmedi',
    );
  });

  group('skip', () {
    late String createdDebtId;

    setUp(() async {
      createdDebtId = '';
      // skip için taksitli borç gerekli
      final debt = await ds.create(
        CreateDebtRequest(
          name: 'Atlama Test Borcu',
          firstPaymentDate: '2026-04-30T00:00:00',
          paymentPeriodType: PaymentPeriodTypeJson.monthly,
          installmentCount: 3,
          amount: 100.0,
          currency: 'TRY',
          categoryId: testCategoryId,
        ),
      );
      createdDebtId = debt.id;
    });

    tearDown(() async {
      if (createdDebtId.isNotEmpty) {
        await ds.delete(createdDebtId).catchError((_) {});
      }
    });

    test('taksidi atlar ve model döner', () async {
      final result = await ds.skip(createdDebtId);
      expect(result.id, equals(createdDebtId));
    });
  });

  group('delete', () {
    test('borcu siler ve sonrasında NotFoundException fırlar', () async {
      final debt = await ds.create(baseRequest());
      final id = debt.id;

      await ds.delete(id);

      expect(
        () => ds.getById(id),
        throwsA(isA<NotFoundException>()),
      );
    });

    test('var olmayan borç ID ile NotFoundException fırlar', () async {
      expect(
        () => ds.delete('000000000000000000000000'),
        throwsA(isA<NotFoundException>()),
      );
    });
  });

  group('update', () {
    late String createdDebtId;

    setUp(() async {
      createdDebtId = '';
      final debt = await ds.create(baseRequest());
      createdDebtId = debt.id;
    });

    tearDown(() async {
      if (createdDebtId.isNotEmpty) {
        await ds.delete(createdDebtId).catchError((_) {});
      }
    });

    test('borç adı ve tutarı güncellenir', () async {
      final updateReq = CreateDebtRequest(
        id: createdDebtId,
        name: 'Güncellenmiş Borç',
        firstPaymentDate: '2026-04-30T00:00:00',
        paymentPeriodType: PaymentPeriodTypeJson.oneTime,
        amount: 500.0,
        currency: 'TRY',
        categoryId: testCategoryId,
      );
      final result = await ds.update(updateReq);
      expect(result.name, equals('Güncellenmiş Borç'));
      expect(result.amount, equals(500.0));
    });
  });
}
