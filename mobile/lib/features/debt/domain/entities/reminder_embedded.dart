class ReminderEmbedded {
  const ReminderEmbedded({
    required this.enabled,
    this.channels,
    this.reminderDate,
    this.reminderTime,
  });
  final bool enabled;
  final List<String>? channels; // EMAIL, IN_APP
  final DateTime? reminderDate;
  final String? reminderTime; // HH:mm:ss
}
