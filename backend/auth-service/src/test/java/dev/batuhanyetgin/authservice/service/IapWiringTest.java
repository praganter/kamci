package dev.batuhanyetgin.authservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.test.util.TestPropertyValues;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/** The purchase beans in a real Spring context: placeholders, defaults and the bundled Apple root. */
class IapWiringTest {

    private static final String APPLE_ROOT_CA_G3_SHA256 =
            "63343abfb89a6a03ebb57e9b3f5fa7be7c4f5c756f3017b3a8c488c3653e9179";

    private AnnotationConfigApplicationContext context(final Consumer<AnnotationConfigApplicationContext> properties) {
        final AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        properties.accept(ctx);
        ctx.registerBean(PropertySourcesPlaceholderConfigurer.class);
        ctx.registerBean("jsonMapper", JsonMapper.class, (Supplier<JsonMapper>) JsonMapper::new);
        ctx.register(IapSettings.class, AppleJwsVerifier.class, LegacyAppleReceiptVerifier.class,
                GooglePlayVerifier.class, GoogleCertsKeyProvider.class, PubSubTokenVerifier.class);
        ctx.refresh();
        return ctx;
    }

    @Test
    @DisplayName("starts with no store settings at all; every verification path reports 'not configured'")
    void startsWithoutAnyConfiguration() {
        try (var ctx = context(c -> { })) {
            assertThat(ctx.getBean(IapSettings.class).isAppleConfigured()).isFalse();
            assertThat(ctx.getBean(IapSettings.class).isGoogleConfigured()).isFalse();
            assertThat(ctx.getBean(IapSettings.class).isPubSubConfigured()).isFalse();
            assertThat(ctx.getBean(IapSettings.class).productsConfigured()).isFalse();
            assertThat(ctx.getBean(LegacyAppleReceiptVerifier.class).isConfigured()).isFalse();
            assertThat(ctx.getBean(GooglePlayVerifier.class).isConfigured()).isFalse();
        }
    }

    @Test
    @DisplayName("environment-style settings are picked up, including the shared secret via APPLE_SHARED_SECRET")
    void picksUpConfiguration() {
        try (var ctx = context(c -> TestPropertyValues.of(
                "APPLE_BUNDLE_ID=com.example.kamci",
                "APPLE_ALLOW_SANDBOX=true",
                "APPLE_SHARED_SECRET=example-secret",
                "IAP_MONTHLY_PRODUCT_IDS=m1, m2",
                "IAP_YEARLY_PRODUCT_IDS=y1",
                "GOOGLE_PLAY_PACKAGE_NAME=com.example.app",
                "GOOGLE_PUBSUB_AUDIENCE=https://example.test/hook",
                "GOOGLE_PUBSUB_SERVICE_ACCOUNT_EMAIL=push@example.iam.test").applyTo(c))) {
            final IapSettings settings = ctx.getBean(IapSettings.class);
            assertThat(settings.getAppleBundleId()).isEqualTo("com.example.kamci");
            assertThat(settings.isAppleAllowSandbox()).isTrue();
            assertThat(settings.getGooglePackageName()).isEqualTo("com.example.app");
            assertThat(settings.isPubSubConfigured()).isTrue();
            assertThat(settings.planFor("m2")).hasValue(dev.batuhanyetgin.authservice.enums.SubscriptionPlan.MONTHLY);
            assertThat(settings.planFor("y1")).hasValue(dev.batuhanyetgin.authservice.enums.SubscriptionPlan.YEARLY);
            assertThat(settings.planFor("unknown")).isEmpty();
            assertThat(ctx.getBean(LegacyAppleReceiptVerifier.class).isConfigured()).isTrue();
        }
    }

    @Test
    @DisplayName("the original apple.store.shared-secret property still works")
    void legacyPropertyName() {
        try (var ctx = context(c -> TestPropertyValues.of(
                "APPLE_BUNDLE_ID=com.example.kamci", "apple.store.shared-secret=example-secret").applyTo(c))) {
            assertThat(ctx.getBean(LegacyAppleReceiptVerifier.class).isConfigured()).isTrue();
        }
    }

    @Test
    @DisplayName("the bundled apple-root-ca-g3.pem is Apple's published Root CA G3")
    void bundledRootIsAppleRootG3() throws Exception {
        try (InputStream in = new ClassPathResource("apple-root-ca-g3.pem").getInputStream()) {
            final X509Certificate root = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
            final String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(root.getEncoded()));

            assertThat(fingerprint).isEqualTo(APPLE_ROOT_CA_G3_SHA256);
            assertThat(root.getSubjectX500Principal().getName()).contains("Apple Root CA - G3");
            root.checkValidity();
        }
    }
}
