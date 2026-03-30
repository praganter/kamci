import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../domain/entities/subscription.dart';
import '../../domain/repositories/subscription_repository.dart';
import '../datasources/subscription_remote_datasource.dart';
import '../models/subscription_model.dart';
import '../models/verify_receipt_request.dart';

part 'subscription_repository_impl.g.dart';

@riverpod
SubscriptionRepository subscriptionRepository(Ref ref) =>
    SubscriptionRepositoryImpl(ref.watch(subscriptionRemoteDatasourceProvider));

class SubscriptionRepositoryImpl implements SubscriptionRepository {
  const SubscriptionRepositoryImpl(this._datasource);
  final SubscriptionRemoteDatasource _datasource;

  @override
  Future<Subscription> getStatus() async =>
      (await _datasource.getStatus()).toDomain();

  @override
  Future<Subscription> verifyReceipt(VerifyReceiptRequest request) async =>
      (await _datasource.verifyReceipt(request)).toDomain();
}
