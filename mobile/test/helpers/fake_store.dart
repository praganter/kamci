import 'dart:async';

import 'package:in_app_purchase/in_app_purchase.dart';
import 'package:kamci/features/subscription/data/store/purchase_store.dart';
import 'package:kamci/features/subscription/subscription_constants.dart';

ProductDetails fakeProduct(String id, String price) => ProductDetails(
  id: id,
  title: id,
  description: id,
  price: price,
  rawPrice: 1,
  currencyCode: 'USD',
);

PurchaseDetails fakePurchase(
  String productId,
  PurchaseStatus status, {
  bool pendingComplete = true,
  IAPError? error,
  String receipt = 'receipt-token-1',
}) =>
    PurchaseDetails(
        purchaseID: 'order-1',
        productID: productId,
        verificationData: PurchaseVerificationData(
          localVerificationData: 'local',
          serverVerificationData: receipt,
          source: 'test',
        ),
        transactionDate: '1700000000000',
        status: status,
      )
      ..pendingCompletePurchase = pendingComplete
      ..error = error;

class FakePurchaseStore implements PurchaseStore {
  FakePurchaseStore({this.available = true, List<ProductDetails>? products})
    : products =
          products ??
          [
            fakeProduct(SubscriptionConstants.monthlyProductId, r'$5.99'),
            fakeProduct(SubscriptionConstants.yearlyProductId, r'$59.99'),
          ];

  final controller = StreamController<List<PurchaseDetails>>.broadcast();
  bool available;
  List<ProductDetails> products;
  bool buyResult = true;
  Object? buyError;
  final bought = <String>[];
  final completed = <PurchaseDetails>[];
  int restores = 0;
  int queries = 0;

  void emit(PurchaseDetails p) => controller.add([p]);

  @override
  Stream<List<PurchaseDetails>> get purchaseStream => controller.stream;

  @override
  Future<bool> isAvailable() async => available;

  @override
  Future<ProductDetailsResponse> queryProducts(Set<String> productIds) async {
    queries++;
    return ProductDetailsResponse(
      productDetails: products.where((p) => productIds.contains(p.id)).toList(),
      notFoundIDs: const [],
    );
  }

  @override
  Future<bool> buy(ProductDetails product) async {
    if (buyError != null) throw buyError!;
    bought.add(product.id);
    return buyResult;
  }

  @override
  Future<void> restore() async => restores++;

  @override
  Future<void> complete(PurchaseDetails purchase) async =>
      completed.add(purchase);
}
