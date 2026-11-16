import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../features/auth/presentation/providers/auth_provider.dart';
import '../../features/auth/presentation/providers/auth_state.dart';

part 'current_user_provider.g.dart';

@riverpod
Future<String?> currentUserId(Ref ref) async {
  final authState = ref.watch(authNotifierProvider);
  if (authState is AuthStateAuthenticated) {
    return authState.tokens.userId;
  }
  return null;
}
