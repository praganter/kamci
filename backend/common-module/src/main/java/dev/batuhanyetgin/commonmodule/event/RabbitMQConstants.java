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
    public static final String CATEGORY_MEMBER_ADDED_QUEUE = "notification.category_member_added.queue";
    public static final String CATEGORY_MEMBER_ADDED_DLQ = "notification.category_member_added.dlq";
    public static final String HOUSEHOLD_INVITE_QUEUE = "notification.household_invite.queue";
    public static final String HOUSEHOLD_INVITE_DLQ = "notification.household_invite.dlq";
    public static final String WEEKLY_DIGEST_QUEUE = "notification.weekly_digest.queue";
    public static final String WEEKLY_DIGEST_DLQ = "notification.weekly_digest.dlq";
    // Routing Keys
    public static final String PASSWORD_RESET_ROUTING_KEY = "notification.routing.password_reset";
    public static final String USER_REGISTERED_ROUTING_KEY = "notification.routing.user_registered";
    public static final String DEBT_ADDED_ROUTING_KEY = "notification.routing.debt_added";
    public static final String DEBT_REMINDER_ROUTING_KEY = "notification.routing.debt_reminder";
    public static final String CATEGORY_MEMBER_ADDED_ROUTING_KEY = "notification.routing.category_member_added";
    public static final String HOUSEHOLD_INVITE_ROUTING_KEY = "notification.routing.household_invite";
    public static final String WEEKLY_DIGEST_ROUTING_KEY = "notification.routing.weekly_digest";
    // DLQ Routing Keys
    public static final String PASSWORD_RESET_DLQ_ROUTING_KEY = "notification.routing.password_reset.dlq";
    public static final String USER_REGISTERED_DLQ_ROUTING_KEY = "notification.routing.user_registered.dlq";
    public static final String DEBT_ADDED_DLQ_ROUTING_KEY = "notification.routing.debt_added.dlq";
    public static final String DEBT_REMINDER_DLQ_ROUTING_KEY = "notification.routing.debt_reminder.dlq";
    public static final String CATEGORY_MEMBER_ADDED_DLQ_ROUTING_KEY = "notification.routing.category_member_added.dlq";
    public static final String HOUSEHOLD_INVITE_DLQ_ROUTING_KEY = "notification.routing.household_invite.dlq";
    public static final String WEEKLY_DIGEST_DLQ_ROUTING_KEY = "notification.routing.weekly_digest.dlq";
    private RabbitMQConstants() {
    }
}