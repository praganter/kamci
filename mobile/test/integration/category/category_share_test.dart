import 'package:kamci/features/category/data/datasources/category_remote_datasource.dart';
import 'package:kamci/features/category/domain/entities/member_embedded.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/test_credentials.dart';
import '../helpers/test_http_client.dart';

void main() {
  if (!TestAccounts.hasAB) {
    group('share (TEST_A_* / TEST_B_* verilmedi, skip)', () {
      test('addMember', () {}, skip: TestAccounts.missingMessage);
      test('addMembersBulk', () {}, skip: TestAccounts.missingMessage);
      test('removeMember', () {}, skip: TestAccounts.missingMessage);
    });
    return;
  }

  late CategoryRemoteDatasource dsA;
  late CategoryRemoteDatasource dsB;

  setUpAll(() async {
    await TestHttpClient.setUp();
    dsA = CategoryRemoteDatasource(TestHttpClient.authed);
    dsB = CategoryRemoteDatasource(TestHttpClient.authedB);
  });

  group('addMember (tekli)', () {
    late String categoryId;

    setUp(() async {
      final cat = await dsA.create('Paylaşım Test ${DateTime.now().millisecondsSinceEpoch}');
      categoryId = cat.id;
    });

    tearDown(() async {
      await dsA.delete(categoryId).catchError((_) {});
    });

    test('üye eklenir ve kategoride görünür', () async {
      await dsA.addMember(categoryId, TestHttpClient.emailB, AccessLevel.read);

      final list = await dsA.getAll();
      final cat = list.firstWhere((c) => c.id == categoryId);
      expect(cat.members.any((m) => m.email == TestHttpClient.emailB), isTrue);
    });

    test('eklenen üye READ erişim seviyesiyle kaydedilir', () async {
      await dsA.addMember(categoryId, TestHttpClient.emailB, AccessLevel.read);

      final list = await dsA.getAll();
      final cat = list.firstWhere((c) => c.id == categoryId);
      final member = cat.members.firstWhere((m) => m.email == TestHttpClient.emailB);
      expect(member.accessLevel.toDomain(), equals(AccessLevel.read));
    });

    test('aynı üyeyi tekrar ekleme hata fırlatır', () async {
      await dsA.addMember(categoryId, TestHttpClient.emailB, AccessLevel.read);
      expect(
        () => dsA.addMember(categoryId, TestHttpClient.emailB, AccessLevel.read),
        throwsA(anything),
      );
    });

    test('B kullanıcısı paylaşılan kategoriyi kendi listesinde görür', () async {
      await dsA.addMember(categoryId, TestHttpClient.emailB, AccessLevel.read);

      final listB = await dsB.getAll();
      expect(listB.any((c) => c.id == categoryId), isTrue);
    });
  });

  group('addMembersBulk', () {
    final createdIds = <String>[];

    setUp(() async {
      final ts = DateTime.now().millisecondsSinceEpoch;
      for (var i = 1; i <= 2; i++) {
        final cat = await dsA.create('Toplu Paylaşım $i $ts');
        createdIds.add(cat.id);
      }
    });

    tearDown(() async {
      for (final id in createdIds) {
        await dsA.delete(id).catchError((_) {});
      }
      createdIds.clear();
    });

    test('birden fazla kategoriye aynı anda üye eklenir', () async {
      await dsA.addMembersBulk(createdIds, TestHttpClient.emailB, AccessLevel.write);

      final list = await dsA.getAll();
      for (final id in createdIds) {
        final cat = list.firstWhere((c) => c.id == id);
        expect(
          cat.members.any((m) => m.email == TestHttpClient.emailB),
          isTrue,
          reason: 'Kategori $id üye listesinde B kullanıcısı bulunamadı',
        );
      }
    });
  });

  group('removeMember', () {
    late String categoryId;
    late String memberUserId;

    setUp(() async {
      final cat = await dsA.create('Üye Çıkarma ${DateTime.now().millisecondsSinceEpoch}');
      categoryId = cat.id;

      await dsA.addMember(categoryId, TestHttpClient.emailB, AccessLevel.read);

      memberUserId = TestHttpClient.userIdB;
    });

    tearDown(() async {
      await dsA.delete(categoryId).catchError((_) {});
    });

    test('üye çıkarılır ve listede artık görünmez', () async {
      await dsA.removeMember(categoryId, memberUserId);

      final list = await dsA.getAll();
      final cat = list.firstWhere((c) => c.id == categoryId);
      expect(cat.members.any((m) => m.userId == memberUserId), isFalse);
    });

    test('çıkarılan üye B\'nin kendi listesinde artık bu kategori yok', () async {
      await dsA.removeMember(categoryId, memberUserId);

      final listB = await dsB.getAll();
      expect(listB.any((c) => c.id == categoryId), isFalse);
    });
  });
}
