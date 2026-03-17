package dev.batuhanyetgin.commonmodule.event;

public final class RabbitMQConstants {

    // Exchanges
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_DLX = "notification.dlx";
    // Queues
    public static final String PASSWORD_RESET_QUEUE = "notification.password_reset.queue";
    public static final String PASSWORD_RESET_DLQ = "notification.password_reset.dlq";
    public static final String USER_REGISTERED_QUEUE = "notification.user_registered.queue";
    public static final String USER_REGISTERED_DLQ = "notification.user_registered.dlq";
    public static final String DEBT_ADDED_QUEUE = "notification.debt_added.queue";
    public static final String DEBT_ADDED_DLQ = "notification.debt_added.dlq";
    public static final String DEBT_REMINDER_QUEUE = "notification.debt_reminder.queue";
    public static final String DEBT_REMINDER_DLQ = "notification.debt_reminder.dlq";
    // Routing Keys
    public static final String PASSWORD_RESET_ROUTING_KEY = "notification.routing.password_reset";
    public static final String USER_REGISTERED_ROUTING_KEY = "notification.routing.user_registered";
    public static final String DEBT_ADDED_ROUTING_KEY = "notification.routing.debt_added";
    public static final String DEBT_REMINDER_ROUTING_KEY = "notification.routing.debt_reminder";
    // DLQ Routing Keys
    public static final String PASSWORD_RESET_DLQ_ROUTING_KEY = "notification.routing.password_reset.dlq";
    public static final String USER_REGISTERED_DLQ_ROUTING_KEY = "notification.routing.user_registered.dlq";
    public static final String DEBT_ADDED_DLQ_ROUTING_KEY = "notification.routing.debt_added.dlq";
    public static final String DEBT_REMINDER_DLQ_ROUTING_KEY = "notification.routing.debt_reminder.dlq";

    private RabbitMQConstants() {
    }
}