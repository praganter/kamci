class CurrencyModel {
  const CurrencyModel({required this.code, required this.symbol});

  final String code;
  final String symbol;

  factory CurrencyModel.fromJson(Map<String, dynamic> json) => CurrencyModel(
        code: json['code'] as String,
        symbol: json['symbol'] as String,
      );
}
