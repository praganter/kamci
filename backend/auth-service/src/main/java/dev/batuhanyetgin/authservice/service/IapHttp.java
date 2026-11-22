package dev.batuhanyetgin.authservice.service;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

final class IapHttp {

    private IapHttp() {
    }

    /** RestTemplate with timeouts, so a slow store API cannot hold request threads indefinitely. */
    static RestTemplate restTemplate() {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return new RestTemplate(factory);
    }
}
