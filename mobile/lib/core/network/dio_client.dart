import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../constants/api_constants.dart';
import 'interceptors/auth_interceptor.dart';
import 'interceptors/logging_interceptor.dart';

part 'dio_client.g.dart';

BaseOptions _baseOptions() => BaseOptions(
  baseUrl: ApiConstants.baseUrl,
  connectTimeout: const Duration(seconds: 30),
  receiveTimeout: const Duration(seconds: 30),
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Accept-Language': 'tr',
  },
);

@riverpod
Dio refreshDio(Ref ref) {
  return Dio(_baseOptions());
}

@riverpod
Dio dioClient(Ref ref) {
  final dio = Dio(_baseOptions());

  dio.interceptors.add(AuthInterceptor(ref));

  if (kDebugMode) {
    dio.interceptors.add(LoggingInterceptor());
  }

  return dio;
}
