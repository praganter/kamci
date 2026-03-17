package dev.batuhanyetgin.commonmodule.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        final AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver() {
            private final List<Locale> supported = List.of(Locale.ENGLISH, new Locale("tr"));

            @Override
            public @NonNull Locale resolveLocale(final @NonNull HttpServletRequest request) {
                final Locale locale = super.resolveLocale(request);
                return supported.stream().anyMatch(l -> l.getLanguage().equals(locale.getLanguage()))
                        ? locale
                        : Locale.ENGLISH;
            }
        };

        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }
}