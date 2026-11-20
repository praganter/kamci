package dev.batuhanyetgin.commonmodule.config;

import dev.batuhanyetgin.commonmodule.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.net.URI;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = InternalApiConfigTest.TestConfig.class)
@TestPropertySource(properties = "INTERNAL_API_TOKEN=test-token")
class InternalApiConfigTest {

    @Configuration
    @EnableWebMvc
    @Import(InternalApiConfig.class)
    static class TestConfig {
        @Bean
        static PropertySourcesPlaceholderConfigurer placeholders() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        MessageSource messageSource() {
            final StaticMessageSource messages = new StaticMessageSource();
            messages.setUseCodeAsDefaultMessage(true);
            return messages;
        }

        @Bean
        GlobalExceptionHandler handler(final MessageSource messageSource) {
            return new GlobalExceptionHandler(messageSource);
        }

        @Bean
        Probe probe() {
            return new Probe();
        }
    }

    @RestController
    @RequestMapping("svc/")
    static class Probe {
        @GetMapping("internal/ping")
        String internal() {
            return "internal-ok";
        }

        @GetMapping("public/ping")
        String open() {
            return "public-ok";
        }
    }

    @Autowired private WebApplicationContext context;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("internal path without token header → 403")
    void noToken_forbidden() throws Exception {
        mvc().perform(get("/svc/internal/ping")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("internal path with wrong token → 403")
    void wrongToken_forbidden() throws Exception {
        mvc().perform(get("/svc/internal/ping").header(InternalApiConfig.HEADER, "nope"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("the public dev default is rejected when a real token is configured")
    void devDefault_forbidden() throws Exception {
        mvc().perform(get("/svc/internal/ping").header(InternalApiConfig.HEADER, InternalApiConfig.DEV_DEFAULT_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("internal path with the configured token → 200")
    void validToken_ok() throws Exception {
        mvc().perform(get("/svc/internal/ping").header(InternalApiConfig.HEADER, "test-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("internal-ok"));
    }

    @Test
    @DisplayName("percent-encoded 'internal' segment does not bypass the check")
    void encodedSegment_stillGuarded() throws Exception {
        mvc().perform(get(URI.create("/svc/inter%6Eal/ping"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("non-internal path needs no token")
    void publicPath_open() throws Exception {
        mvc().perform(get("/svc/public/ping")).andExpect(status().isOk()).andExpect(content().string("public-ok"));
    }
}
