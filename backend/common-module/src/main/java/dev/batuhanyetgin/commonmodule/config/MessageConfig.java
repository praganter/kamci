package dev.batuhanyetgin.commonmodule.config;

import dev.batuhanyetgin.commonmodule.constant.CommonConstants;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();

        source.setBasename("classpath:messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setDefaultLocale(Locale.forLanguageTag(CommonConstants.DEFAULT_LOCALE));
        source.setFallbackToSystemLocale(false);

        return source;
    }
}