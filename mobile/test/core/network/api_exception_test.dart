import 'package:dio/dio.dart';
import 'package:dt_mobile/core/network/api_exception.dart';
import 'package:flutter_test/flutter_test.dart';

DioException _err(int status, Map<String, dynamic> body) => DioException(
      requestOptions: RequestOptions(path: '/x'),
      response: Response(
        requestOptions: RequestOptions(path: '/x'),
        statusCode: status,
        data: body,
      ),
      type: DioExceptionType.badResponse,
    );

void main() {
  group('ApiException.fromDioException — backend code routing', () {
    test('premium/limit codes → PremiumLimitException (any status)', () {
      for (final code in const [
        'error.debt.limit_exceeded',
        'error.category.premium_required',
      ]) {
        expect(
          ApiException.fromDioException(_err(400, {'code': code, 'message': 'x'})),
          isA<PremiumLimitException>(),
          reason: code,
        );
      }
    });

    test('permission_denied codes → PermissionDeniedException, not premium', () {
      final e = ApiException.fromDioException(
        _err(403, {'code': 'error.category.permission_denied', 'message': 'x'}),
      );
      expect(e, isA<PermissionDeniedException>());
      expect(e, isNot(isA<PremiumLimitException>()));
    });

    test('error.user.not_found_in_context → UnauthorizedException', () {
      expect(
        ApiException.fromDioException(
          _err(400, {'code': 'error.user.not_found_in_context'}),
        ),
        isA<UnauthorizedException>(),
      );
    });

    group('business codes → BusinessException carrying the code', () {
      const cases = {
        'error.debt.already_completed': 409,
        'error.debt.overpayment': 400,
        'error.category.name_conflict': 409,
        'error.category.cannot_add_self': 409,
        'error.category.member_not_found': 404,
      };
      cases.forEach((code, status) {
        test('$code ($status)', () {
          final e = ApiException.fromDioException(
            _err(status, {'code': code, 'message': 'backend msg'}),
          );
          expect(e, isA<BusinessException>());
          expect((e as BusinessException).code, code);
        });
      });

      test('member_not_found is a BusinessException, not a bare NotFound', () {
        final e = ApiException.fromDioException(
          _err(404, {'code': 'error.category.member_not_found', 'message': 'x'}),
        );
        expect(e, isA<BusinessException>());
        expect(e, isNot(isA<NotFoundException>()));
      });
    });

    test('unknown code falls back to status-based behavior', () {
      expect(
        ApiException.fromDioException(_err(409, {'code': 'error.whatever'})),
        isA<BusinessException>(),
      );
      expect(
        ApiException.fromDioException(_err(404, {'code': 'error.whatever'})),
        isA<NotFoundException>(),
      );
      expect(
        ApiException.fromDioException(_err(403, {'message': 'x'})),
        isA<PremiumLimitException>(),
      );
    });
  });
}
