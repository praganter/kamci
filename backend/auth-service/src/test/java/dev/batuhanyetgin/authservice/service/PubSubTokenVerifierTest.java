package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Signs real RS256 tokens with a generated key and verifies them through the real JWT parser. */
class PubSubTokenVerifierTest {

    private static final String AUDIENCE = "https://kamci.example.test/subscription/google/webhook";
    private static final String EMAIL = "push@example.iam.test";
    private static final String ISSUER = "https://accounts.google.com";
    private static final String KID = "key-1";

    private static KeyPair signingKey;
    private static KeyPair otherKey;
    private static PubSubKeyProvider keys;
    private static PubSubTokenVerifier verifier;

    @BeforeAll
    static void generateKeys() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        signingKey = generator.generateKeyPair();
        otherKey = generator.generateKeyPair();
        final Map<String, PublicKey> published = Map.of(KID, signingKey.getPublic());
        keys = published::get;
        verifier = new PubSubTokenVerifier(new IapSettings("", false, "", "", "", AUDIENCE, EMAIL), keys);
    }

    private String token(final Function<io.jsonwebtoken.JwtBuilder, io.jsonwebtoken.JwtBuilder> customize,
                         final KeyPair signWith) {
        final io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .header().keyId(KID).and()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .claim("email", EMAIL)
                .claim("email_verified", true)
                .expiration(new Date(System.currentTimeMillis() + 600_000));
        return customize.apply(builder).signWith(signWith.getPrivate(), Jwts.SIG.RS256).compact();
    }

    private String validToken() {
        return token(b -> b, signingKey);
    }

    private static void assertForbidden(final org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatExceptionOfType(BaseException.class)
                .isThrownBy(call)
                .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN)
                .withMessage("error.subscription.webhook_unauthorized");
    }

    @Test
    @DisplayName("valid Google-signed token for our audience and service account → accepted")
    void validToken_accepted() {
        assertThatCode(() -> verifier.verify("Bearer " + validToken())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the 'accounts.google.com' issuer form and a string email_verified are accepted too")
    void alternateIssuerForm_accepted() {
        final String token = token(b -> b.issuer("accounts.google.com").claim("email_verified", "true"), signingKey);

        assertThatCode(() -> verifier.verify("Bearer " + token)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("expired token → 403")
    void expired_forbidden() {
        final String token = token(b -> b.expiration(new Date(System.currentTimeMillis() - 3_600_000)), signingKey);

        assertForbidden(() -> verifier.verify("Bearer " + token));
    }

    @Test
    @DisplayName("token for another audience → 403")
    void wrongAudience_forbidden() {
        final String token = token(b -> b.audience().add("https://elsewhere.example.test").and().claim("aud", "https://elsewhere.example.test"), signingKey);

        assertForbidden(() -> verifier.verify("Bearer " + token));
    }

    @Test
    @DisplayName("token for another service account → 403")
    void wrongEmail_forbidden() {
        final String token = token(b -> b.claim("email", "someone-else@example.iam.test"), signingKey);

        assertForbidden(() -> verifier.verify("Bearer " + token));
    }

    @Test
    @DisplayName("email_verified false → 403")
    void emailNotVerified_forbidden() {
        final String token = token(b -> b.claim("email_verified", false), signingKey);

        assertForbidden(() -> verifier.verify("Bearer " + token));
    }

    @Test
    @DisplayName("issuer other than Google → 403")
    void wrongIssuer_forbidden() {
        final String token = token(b -> b.issuer("https://evil.example.test"), signingKey);

        assertForbidden(() -> verifier.verify("Bearer " + token));
    }

    @Test
    @DisplayName("signed with a key Google does not publish under that kid → 403")
    void forgedSignature_forbidden() {
        assertForbidden(() -> verifier.verify("Bearer " + token(b -> b, otherKey)));
    }

    @Test
    @DisplayName("unknown key id → 403")
    void unknownKid_forbidden() {
        final String token = Jwts.builder().header().keyId("other-kid").and().issuer(ISSUER)
                .audience().add(AUDIENCE).and().claim("email", EMAIL).claim("email_verified", true)
                .expiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(signingKey.getPrivate(), Jwts.SIG.RS256).compact();

        assertForbidden(() -> verifier.verify("Bearer " + token));
    }

    @Test
    @DisplayName("HS256 token signed with the public key as the secret (algorithm confusion) → 403")
    void hmacWithPublicKey_forbidden() {
        final SecretKey hmac = Keys.hmacShaKeyFor(signingKey.getPublic().getEncoded());
        final String token = Jwts.builder().header().keyId(KID).and().issuer(ISSUER)
                .audience().add(AUDIENCE).and().claim("email", EMAIL).claim("email_verified", true)
                .expiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(hmac, Jwts.SIG.HS256).compact();

        assertForbidden(() -> verifier.verify("Bearer " + token));
    }

    @Test
    @DisplayName("unsigned (alg none) token → 403")
    void unsigned_forbidden() {
        final String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"kid\":\"key-1\"}".getBytes(StandardCharsets.UTF_8));
        final String body = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"" + ISSUER + "\",\"aud\":\"" + AUDIENCE + "\",\"email\":\"" + EMAIL + "\",\"email_verified\":true}").getBytes(StandardCharsets.UTF_8));

        assertForbidden(() -> verifier.verify("Bearer " + header + "." + body + "."));
    }

    @Test
    @DisplayName("missing, non-Bearer or garbage Authorization header → 403")
    void badHeader_forbidden() {
        assertForbidden(() -> verifier.verify(null));
        assertForbidden(() -> verifier.verify(validToken()));
        assertForbidden(() -> verifier.verify("Basic abc"));
        assertForbidden(() -> verifier.verify("Bearer not-a-jwt"));
        assertForbidden(() -> verifier.verify("Bearer "));
    }

    @Test
    @DisplayName("audience or service account not configured → 403 even for a valid token (fail closed)")
    void notConfigured_forbidden() {
        final String token = validToken();

        assertForbidden(() -> new PubSubTokenVerifier(new IapSettings("", false, "", "", "", "", EMAIL), keys)
                .verify("Bearer " + token));
        assertForbidden(() -> new PubSubTokenVerifier(new IapSettings("", false, "", "", "", AUDIENCE, ""), keys)
                .verify("Bearer " + token));
    }
}
