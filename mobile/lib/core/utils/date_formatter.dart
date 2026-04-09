import 'package:intl/intl.dart';

abstract final class DateFormatter {
  static final DateFormat _apiFormat = DateFormat('yyyy-MM-dd');
  static final DateFormat _apiDateTimeFormat = DateFormat(
    "yyyy-MM-dd'T'HH:mm:ss",
  );

  static String formatDate(DateTime date) =>
      DateFormat('dd MMM yyyy', Intl.defaultLocale).format(date);

  static String formatDateTime(DateTime dateTime) =>
      DateFormat('dd MMM yyyy, HH:mm', Intl.defaultLocale).format(dateTime);

  static String toApiFormat(DateTime date) => _apiFormat.format(date);

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
