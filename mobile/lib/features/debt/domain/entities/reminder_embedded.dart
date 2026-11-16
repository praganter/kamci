import 'reminder_channel.dart';

class ReminderEmbedded {
  const ReminderEmbedded({
    required this.enabled,
    this.channels,
    this.reminderDate,
    this.reminderTime,
  });
  final bool enabled;
  final List<ReminderChannel>? channels;
  final DateTime? reminderDate;
  final String? reminderTime;
}
