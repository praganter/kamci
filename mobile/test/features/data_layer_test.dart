import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kamci/core/network/api_exception.dart';
import 'package:kamci/features/auth/data/datasources/auth_remote_datasource.dart';
import 'package:kamci/features/auth/data/models/login_request.dart';
import 'package:kamci/features/auth/data/models/register_request.dart';
import 'package:kamci/features/category/data/datasources/category_remote_datasource.dart';
import 'package:kamci/features/category/domain/entities/member_embedded.dart';
import 'package:kamci/features/debt/data/datasources/debt_remote_datasource.dart';
import 'package:kamci/features/debt/data/models/create_debt_request.dart';
import 'package:kamci/features/debt/data/models/debt_model.dart';
import 'package:kamci/features/income/data/datasources/income_remote_datasource.dart';
import 'package:kamci/features/income/data/models/save_income_request.dart';

import '../helpers/fake_api.dart';

CreateDebtRequest _debtRequest() => const CreateDebtRequest(
  name: 'Rent',
  categoryId: 'c1',
  firstPaymentDate: '2026-12-01T10:00:00',
  paymentPeriodType: PaymentPeriodTypeJson.monthly,
  amount: 500,
  currency: 'TRY',
);

void main() {
  group('AuthRemoteDatasource', () {
    late FakeApi api;
    late AuthRemoteDatasource ds;
    setUp(() {
      api = FakeApi();
      ds = AuthRemoteDatasource(api.dio(), api.dio());
    });

    test('login posts credentials and parses tokens', () async {
      api.on('POST /auth/login', fixture('auth_refresh'));
      final r = await ds.login(
        const LoginRequest(email: 'a@b.c', password: 'secret123'),
      );
      expect(r.accessToken, isNotEmpty);
      expect(api.last.data, {'email': 'a@b.c', 'password': 'secret123'});
    });

    test('register posts credentials', () async {
      api.on('POST /auth/register', fixture('auth_refresh'));
      await ds.register(
        const RegisterRequest(email: 'a@b.c', password: 'secret123'),
      );
      expect(api.last.path, '/auth/register');
    });

    test('refreshToken sends the refresh token as Bearer header', () async {
      api.on('POST /auth/refresh-token', fixture('auth_refresh'));
      await ds.refreshToken('r-token');
      expect(api.last.headers['Authorization'], 'Bearer r-token');
    });

    test('logout and password endpoints send the documented bodies', () async {
      api
          .on('POST /auth/logout', null, 204)
          .on('POST /auth/forget-password', null, 204)
          .on('POST /auth/reset-password', null, 204)
          .on('POST /auth/change-password', null, 204);
      await ds.logout('r');
      expect(api.last.headers['Authorization'], 'Bearer r');
      await ds.forgotPassword('a@b.c');
      expect(api.last.data, {'email': 'a@b.c'});
      await ds.resetPassword('tok', 'newpass123');
      expect(api.last.data, {'token': 'tok', 'newPassword': 'newpass123'});
      await ds.changePassword('old', 'new');
      expect(api.last.data, {'currentPassword': 'old', 'newPassword': 'new'});
    });

    test('userDetail parses the fixture', () async {
      api.on('GET /auth/user-detail', fixture('auth_user_detail'));
      final u = await ds.userDetail();
      expect(u.email, fixture('auth_user_detail')['email']);
    });

    test('HTTP errors are mapped to typed ApiExceptions', () async {
      api.on('POST /auth/login', {'message': 'bad'}, 401);
      await expectLater(
        ds.login(const LoginRequest(email: 'a@b.c', password: 'x')),
        throwsA(isA<UnauthorizedException>()),
      );
      api.on('POST /auth/forget-password', {'message': 'slow down'}, 429);
      await expectLater(
        ds.forgotPassword('a@b.c'),
        throwsA(isA<RateLimitException>()),
      );
      api.on('DELETE /auth/account', {'message': 'boom'}, 500);
      await expectLater(ds.deleteAccount(), throwsA(isA<ServerException>()));
    });
  });

  group('CategoryRemoteDatasource', () {
    late FakeApi api;
    late CategoryRemoteDatasource ds;
    setUp(() {
      api = FakeApi();
      ds = CategoryRemoteDatasource(api.dio());
    });

    test('getAll / create / update parse category payloads', () async {
      api
          .on('GET /category/getAll', fixture('category_get_all'))
          .on('POST /category/save', fixture('category_save'))
          .on('POST /category/update', fixture('category_save'));
      expect((await ds.getAll()).single.name, 'Home');
      expect((await ds.create('Home')).name, 'Home');
      expect(api.last.data, {'name': 'Home'});
      await ds.update('c1', 'Home');
      expect(api.last.data, {'id': 'c1', 'name': 'Home'});
    });

    test('delete and member endpoints use the documented paths', () async {
      api
          .on('DELETE /category/delete/c1', null, 204)
          .on('POST /category/c1/member', null, 204)
          .on('POST /category/members/bulk', null, 204)
          .on('DELETE /category/c1/member/u1', null, 204);
      await ds.delete('c1');
      await ds.addMember('c1', 'x@y.z', AccessLevel.read);
      expect((api.last.data as Map)['email'], 'x@y.z');
      await ds.addMembersBulk(['c1', 'c2'], 'x@y.z', AccessLevel.write);
      expect((api.last.data as Map)['categoryIds'], ['c1', 'c2']);
      await ds.removeMember('c1', 'u1');
      expect(api.requests.map((r) => '${r.method} ${r.path}'), [
        'DELETE /category/delete/c1',
        'POST /category/c1/member',
        'POST /category/members/bulk',
        'DELETE /category/c1/member/u1',
      ]);
    });

    test('migrateAndDelete returns the migrated debt count', () async {
      api.on('DELETE /category/c1/migrate', {'migratedDebtCount': 3});
      expect(await ds.migrateAndDelete('c1', 'c2'), 3);
      expect(api.last.query, {'targetCategoryId': 'c2'});
    });

    test('409 name conflict becomes a BusinessException with its code', () async {
      api.on('POST /category/save', {
        'code': 'error.category.name_conflict',
        'message': 'dup',
      }, 409);
      await expectLater(
        ds.create('Home'),
        throwsA(
          isA<BusinessException>().having(
            (e) => e.code,
            'code',
            'error.category.name_conflict',
          ),
        ),
      );
    });
  });

  group('DebtRemoteDatasource', () {
    late FakeApi api;
    late DebtRemoteDatasource ds;
    setUp(() {
      api = FakeApi();
      ds = DebtRemoteDatasource(api.dio());
    });

    test('getGrouped sends month/isOwner and parses categories', () async {
      api.on('GET /debt/get', fixture('debt_get_grouped'));
      final groups = await ds.getGrouped(month: '2026-11', isOwner: true);
      expect(api.last.query, {'month': '2026-11', 'isOwner': true});
      expect(groups.first.debts, isNotEmpty);
    });

    test('create/update/skip unwrap the debt envelope', () async {
      api
          .on('POST /debt/save', fixture('debt_save'))
          .on('POST /debt/update', fixture('debt_save'))
          .on('POST /debt/skip/d1', fixture('debt_save'));
      expect((await ds.create(_debtRequest())).id, isNotEmpty);
      expect((api.last.data as Map)['paymentPeriodType'], 'MONTHLY');
      await ds.update(_debtRequest());
      await ds.skip('d1');
    });

    test('pay sends no body when empty, amount/time otherwise', () async {
      api.on('POST /debt/pay/d1', fixture('debt_pay_partial'));
      await ds.pay('d1');
      expect(api.last.data, isNull);
      final d = await ds.pay(
        'd1',
        amountPaid: 100,
        paidTime: DateTime(2026, 11, 3, 9, 30),
      );
      expect(api.last.data, {
        'amountPaid': 100.0,
        'paidTime': '2026-11-03T09:30:00',
      });
      expect(d.paymentStatus.name, 'partiallyPaid');
    });

    test('bulkPay posts an array, setPaidInstallments the count', () async {
      api
          .on('POST /debt/pay/d1/bulk', fixture('debt_pay_partial'))
          .on('POST /debt/d1/set-paid-installments', fixture('debt_pay_partial'));
      await ds.bulkPay('d1', [
        {'amountPaid': 10.0, 'paidTime': '2026-11-01T00:00:00'},
      ]);
      expect(api.last.data, isA<List<dynamic>>());
      await ds.setPaidInstallments('d1', 2);
      expect(api.last.data, {'count': 2});
    });

    test('getById, getSummary and delete', () async {
      api
          .on('GET /debt/d1', fixture('debt_by_id'))
          .on('GET /debt/summary', fixture('debt_summary'))
          .on('DELETE /debt/delete/d1', null, 204);
      expect((await ds.getById('d1')).name, 'Rent');
      expect((await ds.getSummary('2026-11'))['month'], '2026-11');
      await ds.delete('d1');
      expect(api.last.method, 'DELETE');
    });

    test('a response without "debt" is a FormatException', () async {
      api.on('POST /debt/skip/d1', {'other': 1});
      await expectLater(ds.skip('d1'), throwsFormatException);
    });

    test('premium limit code maps to PremiumLimitException', () async {
      api.on('POST /debt/save', {
        'code': 'error.debt.limit_exceeded',
        'message': 'limit',
      }, 403);
      await expectLater(
        ds.create(_debtRequest()),
        throwsA(isA<PremiumLimitException>()),
      );
    });
  });

  group('IncomeRemoteDatasource', () {
    test('save / history / current / delete', () async {
      final api = FakeApi()
          .on('POST /income/save', fixture('income_save'))
          .on('GET /income/history', fixture('income_history'))
          .on('GET /income/current', fixture('income_current'))
          .on('DELETE /income/delete/i1', null, 204);
      final ds = IncomeRemoteDatasource(api.dio());
      final saved = await ds.save(
        const SaveIncomeRequest(
          amount: 10000,
          currency: 'TRY',
          effectiveFrom: '2026-11-01',
        ),
      );
      expect(saved.amount, 10000.0);
      expect(await ds.getHistory(), isNotEmpty);
      expect((await ds.getCurrent()).currency, 'TRY');
      await ds.delete('i1');
      expect(api.last.method, 'DELETE');
    });

    test('connection failure becomes NetworkException', () async {
      final dio = Dio(BaseOptions(baseUrl: 'http://fake'))
        ..httpClientAdapter = _Failing();
      await expectLater(
        IncomeRemoteDatasource(dio).getCurrent(),
        throwsA(isA<NetworkException>()),
      );
    });
  });
}

class _Failing extends FakeApi {
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async => throw DioException(
    requestOptions: options,
    type: DioExceptionType.connectionError,
  );
}
