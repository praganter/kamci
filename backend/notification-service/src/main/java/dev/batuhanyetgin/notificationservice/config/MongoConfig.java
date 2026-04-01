package dev.batuhanyetgin.notificationservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.lang.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableMongoAuditing
@RequiredArgsConstructor
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Value("${spring.data.mongodb.uri}")
    private String uri;

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of());
    }

    @NonNull
    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    protected boolean autoIndexCreation() {
        return true;
    }

    @NonNull
    @Override
    public MongoClient mongoClient() {
        try {
            final ConnectionString connectionString = new ConnectionString(uri);
            final MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .applyToConnectionPoolSettings(builder ->
                            builder.maxSize(50)
                                    .minSize(5)
                                    .maxWaitTime(120000, TimeUnit.MILLISECONDS)
                                    .maxConnectionLifeTime(30, TimeUnit.MINUTES)
                                    .maxConnectionIdleTime(10, TimeUnit.MINUTES))
                    .applyToServerSettings(builder ->
                            builder.heartbeatFrequency(10000, TimeUnit.MILLISECONDS))
                    .applyToSocketSettings(builder ->
                            builder.connectTimeout(10000, TimeUnit.MILLISECONDS)
                                    .readTimeout(15000, TimeUnit.MILLISECONDS))
                    .build();
            return MongoClients.create(mongoClientSettings);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create MongoDB client with URI: " + uri, e);
        }
    }
}
