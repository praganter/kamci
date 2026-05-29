import 'package:dio/dio.dart';
import 'package:dt_mobile/features/category/data/datasources/category_remote_datasource.dart';
import 'package:dt_mobile/features/category/domain/entities/member_embedded.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/test_credentials.dart';
import '../helpers/test_http_client.dart';

/// Share testleri premium hesap gerektirir.
/// Backend, yeni kaydedilen kullanıcılara premium vermez.
///
/// Çalıştırma:
///   flutter test test/integration/category/category_share_test.dart \
///     --dart-define=PREMIUM_EMAIL=premium@example.com \
///     --dart-define=PREMIUM_PASSWORD=PremiumPass123!
void main() {
  // Premium hesap yoksa tüm testleri skip et
  if (!PremiumCredentials.available) {
    group('share (premium gerekli — PREMIUM_EMAIL/PASSWORD verilmedi, skip)', () {
      test('addMember', () {}, skip: 'PREMIUM_EMAIL ve PREMIUM_PASSWORD --dart-define ile verilmeli');
      test('addMembersBulk', () {}, skip: 'PREMIUM_EMAIL ve PREMIUM_PASSWORD --dart-define ile verilmeli');
      test('removeMember', () {}, skip: 'PREMIUM_EMAIL ve PREMIUM_PASSWORD --dart-define ile verilmeli');
    });
    return;
  }

  late CategoryRemoteDatasource dsA; // premium sahip (kullanıcı A)
  late CategoryRemoteDatasource dsB; // üye (kullanıcı B — auto-register)

  setUpAll(() async {
    // Kullanıcı A: mevcut premium hesap
    final plainDio = Dio(
      BaseOptions(
        baseUrl: 'http://198.51.100.10:30080',
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 30),
        headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
      ),
    );
    final respA = await plainDio.post('/auth/login', data: {
      'email': PremiumCredentials.email,
      'password': PremiumCredentials.password,
    });
    final tokenA = respA.data['token'] as String;
    final authedA = Dio(
      BaseOptions(
        baseUrl: 'http://198.51.100.10:30080',
        headers: {
          'Authorization': 'Bearer $tokenA',
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );
    dsA = CategoryRemoteDatasource(authedA);

    // Kullanıcı B: auto-register
    await TestHttpClient.setUp();
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

      // Backend doesn't include userId in member embedded data, so we use
      // User B's actual ID parsed from their JWT sub claim.
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
