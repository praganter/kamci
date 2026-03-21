import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../core/storage/secure_storage_service.dart';

part 'current_user_provider.g.dart';

@riverpod
Future<String?> currentUserId(Ref ref) async {
  final storage = ref.watch(secureStorageServiceProvider);
  return storage.getUserId();
}
