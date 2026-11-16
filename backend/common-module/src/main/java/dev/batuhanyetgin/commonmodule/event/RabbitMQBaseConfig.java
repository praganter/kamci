package dev.batuhanyetgin.commonmodule.event;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

@Configuration
public class RabbitMQBaseConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setRetryTemplate(new RetryTemplate(RetryPolicy.builder()
                .maxRetries(2)
                .delay(Duration.ofSeconds(1))
                .multiplier(2.0)
                .maxDelay(Duration.ofSeconds(10))
                .build()));
        return rabbitTemplate;
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(RabbitMQConstants.NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange notificationDlx() {
        return new DirectExchange(RabbitMQConstants.NOTIFICATION_DLX, true, false);
    }

    @Bean
    public Queue passwordResetQueue() {
        return QueueBuilder.durable(RabbitMQConstants.PASSWORD_RESET_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.PASSWORD_RESET_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue passwordResetDlq() {
        return QueueBuilder.durable(RabbitMQConstants.PASSWORD_RESET_DLQ).build();
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(RabbitMQConstants.USER_REGISTERED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.USER_REGISTERED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue userRegisteredDlq() {
        return QueueBuilder.durable(RabbitMQConstants.USER_REGISTERED_DLQ).build();
    }

    @Bean
    public Queue debtAddedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.DEBT_ADDED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEBT_ADDED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue debtAddedDlq() {
        return QueueBuilder.durable(RabbitMQConstants.DEBT_ADDED_DLQ).build();
    }

    @Bean
    public Queue debtReminderQueue() {
        return QueueBuilder.durable(RabbitMQConstants.DEBT_REMINDER_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEBT_REMINDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue debtReminderDlq() {
        return QueueBuilder.durable(RabbitMQConstants.DEBT_REMINDER_DLQ).build();
    }

    @Bean
    public Binding passwordResetBinding() {
        return BindingBuilder.bind(passwordResetQueue())
                .to(notificationExchange())
                .with(RabbitMQConstants.PASSWORD_RESET_ROUTING_KEY);
    }

    @Bean
    public Binding userRegisteredBinding() {
        return BindingBuilder.bind(userRegisteredQueue())
                .to(notificationExchange())
                .with(RabbitMQConstants.USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Binding debtAddedBinding() {
        return BindingBuilder.bind(debtAddedQueue())
                .to(notificationExchange())
                .with(RabbitMQConstants.DEBT_ADDED_ROUTING_KEY);
    }

    @Bean
    public Binding debtReminderBinding() {
        return BindingBuilder.bind(debtReminderQueue())
                .to(notificationExchange())
                .with(RabbitMQConstants.DEBT_REMINDER_ROUTING_KEY);
    }

    @Bean
    public Queue categoryMemberAddedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.CATEGORY_MEMBER_ADDED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.CATEGORY_MEMBER_ADDED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue categoryMemberAddedDlq() {
        return QueueBuilder.durable(RabbitMQConstants.CATEGORY_MEMBER_ADDED_DLQ).build();
    }

    @Bean
    public Binding categoryMemberAddedBinding() {
        return BindingBuilder.bind(categoryMemberAddedQueue())
                .to(notificationExchange())
                .with(RabbitMQConstants.CATEGORY_MEMBER_ADDED_ROUTING_KEY);
    }

    @Bean
    public Queue householdInviteQueue() {
        return QueueBuilder.durable(RabbitMQConstants.HOUSEHOLD_INVITE_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.HOUSEHOLD_INVITE_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue householdInviteDlq() {
        return QueueBuilder.durable(RabbitMQConstants.HOUSEHOLD_INVITE_DLQ).build();
    }

    @Bean
    public Binding householdInviteBinding() {
        return BindingBuilder.bind(householdInviteQueue())
                .to(notificationExchange())
                .with(RabbitMQConstants.HOUSEHOLD_INVITE_ROUTING_KEY);
    }

    @Bean
    public Queue weeklyDigestQueue() {
        return QueueBuilder.durable(RabbitMQConstants.WEEKLY_DIGEST_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.WEEKLY_DIGEST_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue weeklyDigestDlq() {
        return QueueBuilder.durable(RabbitMQConstants.WEEKLY_DIGEST_DLQ).build();
    }

    @Bean
    public Binding weeklyDigestBinding() {
        return BindingBuilder.bind(weeklyDigestQueue())
                .to(notificationExchange())
                .with(RabbitMQConstants.WEEKLY_DIGEST_ROUTING_KEY);
    }

    @Bean
    public Binding passwordResetDlqBinding() {
        return BindingBuilder.bind(passwordResetDlq())
                .to(notificationDlx())
                .with(RabbitMQConstants.PASSWORD_RESET_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding userRegisteredDlqBinding() {
        return BindingBuilder.bind(userRegisteredDlq())
                .to(notificationDlx())
                .with(RabbitMQConstants.USER_REGISTERED_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding debtAddedDlqBinding() {
        return BindingBuilder.bind(debtAddedDlq())
                .to(notificationDlx())
                .with(RabbitMQConstants.DEBT_ADDED_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding debtReminderDlqBinding() {
        return BindingBuilder.bind(debtReminderDlq())
                .to(notificationDlx())
                .with(RabbitMQConstants.DEBT_REMINDER_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding categoryMemberAddedDlqBinding() {
        return BindingBuilder.bind(categoryMemberAddedDlq())
                .to(notificationDlx())
                .with(RabbitMQConstants.CATEGORY_MEMBER_ADDED_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding householdInviteDlqBinding() {
        return BindingBuilder.bind(householdInviteDlq())
                .to(notificationDlx())
                .with(RabbitMQConstants.HOUSEHOLD_INVITE_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding weeklyDigestDlqBinding() {
        return BindingBuilder.bind(weeklyDigestDlq())
                .to(notificationDlx())
                .with(RabbitMQConstants.WEEKLY_DIGEST_DLQ_ROUTING_KEY);
    }
}