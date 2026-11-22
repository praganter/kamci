import '../entities/subscription.dart';
import '../../data/models/verify_receipt_request.dart';

abstract interface class SubscriptionRepository {
  Future<Subscription> getStatus();
  Future<Subscription> verifyReceipt(VerifyReceiptRequest request);
}
