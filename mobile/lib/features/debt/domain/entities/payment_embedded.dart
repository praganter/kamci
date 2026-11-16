class PaymentEmbedded {
  const PaymentEmbedded({required this.paid, this.paidTime, this.amountPaid});
  final bool paid;
  final DateTime? paidTime;

  /// Bu dönem için ödenen tutar (kısmi ödemede borç tutarından az olabilir).
  final double? amountPaid;
}
