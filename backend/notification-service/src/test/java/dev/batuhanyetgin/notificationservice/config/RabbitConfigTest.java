package dev.batuhanyetgin.notificationservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitConfigTest {

    private final RabbitConfig config = new RabbitConfig();

    @Test
    @DisplayName("retry interceptor builds and is wired into the listener factory advice chain")
    void factoryUsesRetryInterceptor() {
        final SimpleRabbitListenerContainerFactory factory = config.rabbitListenerContainerFactory(
                mock(ConnectionFactory.class), mock(MessageConverter.class));

        assertThat(config.retryInterceptor()).isNotNull();
        assertThat(factory).hasFieldOrPropertyWithValue("defaultRequeueRejected", false);
        assertThat(factory).extracting("adviceChain").isNotNull();
    }
}
