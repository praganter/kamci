import 'package:dt_mobile/core/network/api_exception.dart';
import 'package:dt_mobile/features/category/data/datasources/category_remote_datasource.dart';
import 'package:flutter_test/flutter_test.dart';

import '../helpers/test_http_client.dart';

void main() {
  late CategoryRemoteDatasource ds;

  setUpAll(() async {
    await TestHttpClient.setUp();
    ds = CategoryRemoteDatasource(TestHttpClient.authed);
  });

  group('create', () {
    late String createdId;

    tearDown(() async {
      if (createdId.isNotEmpty) {
        await ds.delete(createdId).catchError((_) {});
      }
    });

    test('kategori oluşturur ve doğru isimle döner', () async {
      createdId = '';
      final result = await ds.create('Test Kategori');
      createdId = result.id;

      // create endpoint ownerId döndürmez; id ve name yeterli
      expect(result.name, equals('Test Kategori'));
      expect(result.id, isNotEmpty);
    });

    test('boş isimle BusinessException fırlar', () async {
      createdId = '';
      expect(
        () => ds.create(''),
        throwsA(isA<BusinessException>()),
      );
    });
  });

  group('getAll', () {
    late String createdId;

    setUp(() async {
      final cat = await ds.create('Liste Test Kategori');
      createdId = cat.id;
    });

    tearDown(() async {
      await ds.delete(createdId).catchError((_) {});
    });

    test('kategori listesini döner ve oluşturulan kategori içinde', () async {
      final list = await ds.getAll();
      expect(list, isNotEmpty);
      expect(list.any((c) => c.id == createdId), isTrue);
    });
  });

  group('update', () {
    late String createdId;

    setUp(() async {
      final cat = await ds.create('Güncelleme Öncesi');
      createdId = cat.id;
    });

    tearDown(() async {
      await ds.delete(createdId).catchError((_) {});
    });

    test('kategori adı güncellenir', () async {
      final result = await ds.update(createdId, 'Güncelleme Sonrası');
      expect(result.name, equals('Güncelleme Sonrası'));
      expect(result.id, equals(createdId));
    });
  });

  group('delete', () {
    test('kategori silinir ve sonrasında listede bulunmaz', () async {
      final cat = await ds.create('Silinecek Kategori');
      final id = cat.id;

      await ds.delete(id);

      final list = await ds.getAll();
      expect(list.any((c) => c.id == id), isFalse);
    });

    test('var olmayan ID ile NotFoundException fırlar', () async {
      expect(
        () => ds.delete('000000000000000000000000'),
        throwsA(isA<NotFoundException>()),
      );
    });
  });
}
