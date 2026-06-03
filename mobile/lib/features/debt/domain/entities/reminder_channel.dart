enum ReminderChannel {
  email,
  inApp;

  String toJson() => switch (this) {
    ReminderChannel.email => 'EMAIL',
    ReminderChannel.inApp => 'IN_APP',
  };

  static ReminderChannel fromJson(String value) => switch (value) {
    'EMAIL' => ReminderChannel.email,
    'IN_APP' => ReminderChannel.inApp,
    _ => throw ArgumentError('Unknown ReminderChannel: $value'),
  };
}
