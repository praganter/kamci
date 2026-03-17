package dev.batuhanyetgin.commonmodule.event;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQBaseConfig {

    // -------------------------
    // Converter & Template
    // -------------------------

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    // -------------------------
    // Exchanges
    // -------------------------

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(RabbitMQConstants.NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange notificationDlx() {
        return new DirectExchange(RabbitMQConstants.NOTIFICATION_DLX, true, false);
    }

    // -------------------------
    // Queues — Password Reset
    // -------------------------

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

    // -------------------------
    // Queues — User Registered
    // -------------------------

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

    // -------------------------
    // Queues — Debt Added
    // -------------------------

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

    // -------------------------
    // Queues — Debt Reminder
    // -------------------------

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

    // -------------------------
    // Bindings — Main Exchange
    // -------------------------

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

    // -------------------------
    // Bindings — DLX
    // -------------------------

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
}