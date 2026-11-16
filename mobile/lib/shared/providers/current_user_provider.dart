import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../features/auth/presentation/providers/auth_provider.dart';
import '../../features/auth/presentation/providers/auth_state.dart';

part 'current_user_provider.g.dart';

// Reads userId from the in-memory JWT claim rather than Keychain.
// This avoids a Keychain read after clearKeychain (E2E tests) where securityd
// may still be restarting and reads would hang or throw.
@riverpod
Future<String?> currentUserId(Ref ref) async {
  final authState = ref.watch(authNotifierProvider);
  if (authState is AuthStateAuthenticated) {
    return authState.tokens.userId;
  }
  return null;
}
