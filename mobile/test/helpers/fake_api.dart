import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';

dynamic fixture(String name) =>
    jsonDecode(File('test/fixtures/$name.json').readAsStringSync());

class FakeResponse {
  const FakeResponse(this.body, [this.status = 200]);
  final Object? body;
  final int status;
}

class RecordedRequest {
  RecordedRequest(this.method, this.path, this.query, this.data, this.headers);
  final String method;
  final String path;
  final Map<String, dynamic> query;
  final Object? data;
  final Map<String, dynamic> headers;
}

class FakeApi implements HttpClientAdapter {
  FakeApi([Map<String, FakeResponse>? routes]) : routes = routes ?? {};

  final Map<String, FakeResponse> routes;
  final requests = <RecordedRequest>[];

  FakeApi on(String route, Object? body, [int status = 200]) {
    routes[route] = FakeResponse(body, status);
    return this;
  }

  Dio dio() =>
      Dio(BaseOptions(baseUrl: 'http://fake'))..httpClientAdapter = this;

  RecordedRequest get last => requests.last;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(
      RecordedRequest(
        options.method,
        options.path,
        options.queryParameters,
        options.data,
        options.headers,
      ),
    );
    final r = routes['${options.method} ${options.path}'];
    final json = {
      Headers.contentTypeHeader: ['application/json'],
    };
    if (r == null) {
      return ResponseBody.fromString(
        jsonEncode({'message': 'no route'}),
        404,
        headers: json,
      );
    }
    final body = r.body is Object? Function() ? (r.body as Object? Function())() : r.body;
    return ResponseBody.fromString(
      body == null ? '' : jsonEncode(body),
      r.status,
      headers: json,
    );
  }

  @override
  void close({bool force = false}) {}
}
