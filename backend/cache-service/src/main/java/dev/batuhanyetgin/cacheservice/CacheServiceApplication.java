package dev.batuhanyetgin.cacheservice;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// @SpringBootApplication scans dev.batuhanyetgin.cacheservice.* by default.
// @ComponentScan adds only the common-module packages we actually need,
// deliberately omitting commonmodule.event (RabbitMQBaseConfig) and
// commonmodule.repository (MongoDB-specific beans).
@SpringBootApplication(
        exclude = {
                RabbitAutoConfiguration.class,
                MongoAutoConfiguration.class,
                MongoDataAutoConfiguration.class,
                MongoRepositoriesAutoConfiguration.class
        }
)
@ComponentScan(basePackages = {
        "dev.batuhanyetgin.cacheservice",
        "dev.batuhanyetgin.commonmodule.config",
        "dev.batuhanyetgin.commonmodule.exception",
        "dev.batuhanyetgin.commonmodule.filter",
        "dev.batuhanyetgin.commonmodule.util",
        "dev.batuhanyetgin.commonmodule.constant"
})
@EnableDiscoveryClient
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT55M")
public class CacheServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(CacheServiceApplication.class, args);
    }
}
