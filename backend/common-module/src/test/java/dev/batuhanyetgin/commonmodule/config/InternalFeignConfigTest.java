package dev.batuhanyetgin.commonmodule.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InternalFeignConfigTest {

    private final RequestInterceptor interceptor = new InternalFeignConfig().internalTokenRequestInterceptor("tok");

    private RequestTemplate apply(final String path) {
        final RequestTemplate template = new RequestTemplate().method(feign.Request.HttpMethod.GET).uri(path);
        interceptor.apply(template);
        return template;
    }

    @Test
    @DisplayName("calls to an internal path carry the X-Internal-Token header")
    void internalPath_addsHeader() {
        assertThat(apply("/auth/internal/user/exists/u1").headers().get(InternalApiConfig.HEADER))
                .containsExactly("tok");
    }

    @Test
    @DisplayName("calls to other paths do not leak the token")
    void otherPath_noHeader() {
        assertThat(apply("/rates/USD/TRY").headers()).doesNotContainKey(InternalApiConfig.HEADER);
    }
}
