package dev.batuhanyetgin.commonmodule.util;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestContextUtilTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void bindRequest(final MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Nested
    @DisplayName("getUserId")
    class GetUserId {

        @Test
        @DisplayName("no request context bound → throws BaseException")
        void noContext_throwsBaseException() {
            assertThatThrownBy(RequestContextUtil::getUserId).isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("user-id header missing → throws BaseException")
        void missingHeader_throwsBaseException() {
            bindRequest(new MockHttpServletRequest());

            assertThatThrownBy(RequestContextUtil::getUserId).isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("user-id header present → returns header value")
        void headerPresent_returnsValue() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("user-id", "user1");
            bindRequest(request);

            assertThat(RequestContextUtil.getUserId()).isEqualTo("user1");
        }
    }

    @Nested
    @DisplayName("isPremium")
    class IsPremium {

        @Test
        @DisplayName("no request context bound → false")
        void noContext_returnsFalse() {
            assertThat(RequestContextUtil.isPremium()).isFalse();
        }

        @Test
        @DisplayName("header = true (any case) → true")
        void headerTrue_returnsTrue() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("user-premium", "TRUE");
            bindRequest(request);

            assertThat(RequestContextUtil.isPremium()).isTrue();
        }

        @Test
        @DisplayName("header missing → false")
        void headerMissing_returnsFalse() {
            bindRequest(new MockHttpServletRequest());

            assertThat(RequestContextUtil.isPremium()).isFalse();
        }

        @Test
        @DisplayName("header = false → false")
        void headerFalse_returnsFalse() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("user-premium", "false");
            bindRequest(request);

            assertThat(RequestContextUtil.isPremium()).isFalse();
        }
    }

    @Nested
    @DisplayName("getLocale")
    class GetLocale {

        @Test
        @DisplayName("no request context bound → default locale (tr)")
        void noContext_returnsDefaultLocale() {
            assertThat(RequestContextUtil.getLocale()).isEqualTo("tr");
        }

        @Test
        @DisplayName("Accept-Language starts with en → en")
        void acceptLanguageEn_returnsEn() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "en-US,en;q=0.9");
            bindRequest(request);

            assertThat(RequestContextUtil.getLocale()).isEqualTo("en");
        }

        @Test
        @DisplayName("Accept-Language is some other language → falls back to default locale")
        void acceptLanguageOther_returnsDefault() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "de-DE");
            bindRequest(request);

            assertThat(RequestContextUtil.getLocale()).isEqualTo("tr");
        }

        @Test
        @DisplayName("Accept-Language header missing → default locale")
        void headerMissing_returnsDefault() {
            bindRequest(new MockHttpServletRequest());

            assertThat(RequestContextUtil.getLocale()).isEqualTo("tr");
        }
    }
}
