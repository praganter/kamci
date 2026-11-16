import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../constants/api_constants.dart';
import '../providers/app_settings_provider.dart';
import 'interceptors/auth_interceptor.dart';
import 'interceptors/logging_interceptor.dart';

part 'dio_client.g.dart';

BaseOptions _baseOptions(String languageCode) => BaseOptions(
  baseUrl: ApiConstants.baseUrl,
  connectTimeout: const Duration(seconds: 30),
  receiveTimeout: const Duration(seconds: 30),
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Accept-Language': languageCode,
  },
);

@riverpod
Dio refreshDio(Ref ref) {
  final languageCode =
      ref.read(settingsServiceProvider).locale?.languageCode ?? 'tr';
  return Dio(_baseOptions(languageCode));
}

@riverpod
Dio dioClient(Ref ref) {
  final languageCode =
      ref.read(settingsServiceProvider).locale?.languageCode ?? 'tr';
  final dio = Dio(_baseOptions(languageCode));

  dio.interceptors.add(AuthInterceptor(ref));

  if (kDebugMode) {
    dio.interceptors.add(LoggingInterceptor());
  }

  return dio;
}
