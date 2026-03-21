import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

class LoggingInterceptor extends Interceptor {
  static const _separator = '------------------------------------------------------------';

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    _log([
      _separator,
      '▶ REQUEST  ${options.method}  ${options.uri}',
      _separator,
      '  Headers:',
      ..._formatMap(options.headers),
      if (options.data != null) ...[
        '  Body:',
        _formatBody(options.data),
      ],
    ]);
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    _log([
      _separator,
      '◀ RESPONSE  ${response.statusCode}  ${response.requestOptions.uri}',
      _separator,
      '  Headers:',
      ..._formatMap(response.headers.map),
      '  Body:',
      _formatBody(response.data),
    ]);
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    _log([
      _separator,
      '✖ ERROR  ${err.response?.statusCode ?? err.type.name}  ${err.requestOptions.uri}',
      _separator,
      if (err.response?.data != null) ...[
        '  Body:',
        _formatBody(err.response!.data),
      ],
      '  Message: ${err.message}',
    ]);
    handler.next(err);
  }

  void _log(List<String> lines) {
    if (!kDebugMode) return;
    debugPrint(lines.join('\n'));
  }

  List<String> _formatMap(Map<String, dynamic> map) {
    return map.entries
        .map((e) => '    ${e.key}: ${e.value}')
        .toList();
  }

  String _formatBody(dynamic data) {
    if (data == null) return '    (empty)';
    try {
      const encoder = JsonEncoder.withIndent('  ');
      final jsonStr = data is String ? data : encoder.convert(data);
      return jsonStr
          .split('\n')
          .map((line) => '    $line')
          .join('\n');
    } catch (_) {
      return '    $data';
    }
  }
}
