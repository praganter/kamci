package dev.batuhanyetgin.debttracker.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.lang.NonNull;
import dev.batuhanyetgin.debttracker.converter.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;
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
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new AccessLevelTypeReadConverter(),
                new AccessLevelTypeWriteConverter(),
                new DebtTypeReadConverter(),
                new DebtTypeWriteConverter(),
                new PaymentPeriodTypeReadConverter(),
                new PaymentPeriodTypeWriteConverter(),
                new DebtStatusReadConverter(),
                new DebtStatusWriteConverter()
        ));
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
        ConnectionString connectionString = new ConnectionString(uri);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder ->
                        builder.maxSize(50) // Aynı anda max 50 bağlantı
                                .minSize(5)   // Boşta en az 5 bağlantı tut
                                .maxWaitTime(120000, TimeUnit.MILLISECONDS))
                .applyToServerSettings(builder ->
                        builder.heartbeatFrequency(10000, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder ->
                        builder.connectTimeout(10000, TimeUnit.MILLISECONDS)
                                .readTimeout(15000, TimeUnit.MILLISECONDS))
                .retryWrites(true) // Yazma hatalarında otomatik tekrar dene
                .retryReads(true)  // Okuma hatalarında otomatik tekrar dene
                .build();
        return MongoClients.create(mongoClientSettings);
    }
}