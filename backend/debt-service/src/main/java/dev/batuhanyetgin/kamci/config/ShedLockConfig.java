package dev.batuhanyetgin.kamci.config;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT55M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(final MongoTemplate mongoTemplate) {
        return new MongoLockProvider(mongoTemplate.getDb());
    }

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(final LockProvider lockProvider) {
        return new DefaultLockingTaskExecutor(lockProvider);
    }
}
