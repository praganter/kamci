import 'package:intl/intl.dart';

abstract final class DateFormatter {
  static final DateFormat _apiFormat = DateFormat('yyyy-MM-dd');
  // Backend LocalDateTime: yyyy-MM-ddTHH:mm:ss — NO milliseconds, NO zone/offset.
  static final DateFormat _apiDateTimeFormat =
      DateFormat("yyyy-MM-dd'T'HH:mm:ss");
  static String formatDate(DateTime date) =>
      DateFormat('dd MMM yyyy', Intl.defaultLocale).format(date);

  static String formatDateTime(DateTime dateTime) =>
      DateFormat('dd MMM yyyy, HH:mm', Intl.defaultLocale).format(dateTime);

  static String toApiFormat(DateTime date) => _apiFormat.format(date);

  // Naive (zone'suz) ISO: backend datetime'ları timezone'suz saklar ve naive
  // karşılaştırır. toUtc() kullanmak gece yarısını önceki güne (21:00Z)
  // kaydırıp aynı gün ödemelerde "payment_before_start" (400) üretiyordu.
  // toIso8601String() milisaniye ekliyordu; backend LocalDateTime bunu
  // reddediyor — saniye hassasiyetinde, zone'suz string üretiyoruz.
  static String toApiDateTimeFormat(DateTime date) =>
      _apiDateTimeFormat.format(date);

  static DateTime? parseApiDate(String? dateStr) {
    if (dateStr == null) return null;
    try {
      return _apiFormat.parse(dateStr);
    } catch (_) {
      return null;
    }
  }
}
