class PaymentEmbedded {
  const PaymentEmbedded({required this.paid, this.paidTime, this.amountPaid});
  final bool paid;
  final DateTime? paidTime;

  final double? amountPaid;
}
