package dev.batuhanyetgin.authservice.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Builds real EC certificate chains with {@code keytool} (no extra crypto dependency) and signs
 * JWS values the way Apple does (ES256, raw R||S), so AppleJwsVerifier's signature and
 * chain-of-trust checks run for real instead of against mocks.
 */
class AppleJwsVerifierTest {

    private static final char[] STORE_PASSWORD = "changeit".toCharArray();
    private static final JsonMapper MAPPER = new JsonMapper();
    private static final String PAYLOAD = "{\"notificationType\":\"TEST\"}";

    private static Path tempDir;
    private static CertChain trustedChain;
    private static CertChain rogueChain;
    private static CertChain noMarkerChain;

    private final AppleJwsVerifier verifier = new AppleJwsVerifier(MAPPER);

    private record CertChain(byte[] leafCertDer, byte[] interCertDer, X509Certificate rootCert,
                             PrivateKey leafPrivateKey) {
    }

    @BeforeAll
    static void buildCertChains() throws Exception {
        tempDir = Files.createTempDirectory("apple-jws-test");
        trustedChain = buildChain("trusted", true);
        rogueChain = buildChain("rogue", true);
        noMarkerChain = buildChain("nomarker", false);
    }

    @AfterAll
    static void cleanup() throws IOException {
        if (tempDir != null) {
            try (var paths = Files.walk(tempDir)) {
                paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
            }
        }
    }

    /** root (self-signed CA) -> intermediate (CA) -> leaf, all EC/P-256, via keytool. */
    private static CertChain buildChain(final String name, final boolean appleMarkers) throws Exception {
        final String pw = new String(STORE_PASSWORD);
        final Path rootKs = tempDir.resolve(name + "-root.p12");
        final Path interKs = tempDir.resolve(name + "-inter.p12");
        final Path leafKs = tempDir.resolve(name + "-leaf.p12");
        final Path rootCer = tempDir.resolve(name + "-root.cer");
        final Path interCsr = tempDir.resolve(name + "-inter.csr");
        final Path interCer = tempDir.resolve(name + "-inter.cer");
        final Path leafCsr = tempDir.resolve(name + "-leaf.csr");
        final Path leafCer = tempDir.resolve(name + "-leaf.cer");

        keytool("-genkeypair", "-alias", "root", "-keyalg", "EC", "-groupname", "secp256r1",
                "-sigalg", "SHA256withECDSA", "-dname", "CN=" + name + " Root CA",
                "-validity", "3650", "-keystore", rootKs.toString(), "-storetype", "PKCS12",
                "-storepass", pw, "-ext", "bc:c=ca:true");
        keytool("-exportcert", "-alias", "root", "-keystore", rootKs.toString(),
                "-storepass", pw, "-file", rootCer.toString());

        keytool("-genkeypair", "-alias", "inter", "-keyalg", "EC", "-groupname", "secp256r1",
                "-sigalg", "SHA256withECDSA", "-dname", "CN=" + name + " Intermediate",
                "-validity", "3650", "-keystore", interKs.toString(), "-storetype", "PKCS12",
                "-storepass", pw);
        keytool("-certreq", "-alias", "inter", "-keystore", interKs.toString(),
                "-storepass", pw, "-file", interCsr.toString());
        final var interArgs = new ArrayList<>(Arrays.asList("-gencert", "-alias", "root",
                "-keystore", rootKs.toString(), "-storepass", pw, "-infile", interCsr.toString(),
                "-outfile", interCer.toString(), "-validity", "3650", "-ext", "bc:c=ca:true"));
        if (appleMarkers) {
            interArgs.addAll(Arrays.asList("-ext", AppleJwsVerifier.INTERMEDIATE_MARKER_OID + "=05:00"));
        }
        keytool(interArgs.toArray(new String[0]));
        keytool("-importcert", "-alias", "root", "-keystore", interKs.toString(),
                "-storepass", pw, "-file", rootCer.toString(), "-noprompt");
        keytool("-importcert", "-alias", "inter", "-keystore", interKs.toString(),
                "-storepass", pw, "-file", interCer.toString(), "-trustcacerts", "-noprompt");

        keytool("-genkeypair", "-alias", "leaf", "-keyalg", "EC", "-groupname", "secp256r1",
                "-sigalg", "SHA256withECDSA", "-dname", "CN=" + name + " Leaf",
                "-validity", "3650", "-keystore", leafKs.toString(), "-storetype", "PKCS12",
                "-storepass", pw);
        keytool("-certreq", "-alias", "leaf", "-keystore", leafKs.toString(),
                "-storepass", pw, "-file", leafCsr.toString());
        final var leafArgs = new ArrayList<>(Arrays.asList("-gencert", "-alias", "inter",
                "-keystore", interKs.toString(), "-storepass", pw, "-infile", leafCsr.toString(),
                "-outfile", leafCer.toString(), "-validity", "3650"));
        if (appleMarkers) {
            leafArgs.addAll(Arrays.asList("-ext", AppleJwsVerifier.LEAF_MARKER_OID + "=05:00"));
        }
        keytool(leafArgs.toArray(new String[0]));
        keytool("-importcert", "-alias", "root", "-keystore", leafKs.toString(),
                "-storepass", pw, "-file", rootCer.toString(), "-noprompt");
        keytool("-importcert", "-alias", "inter", "-keystore", leafKs.toString(),
                "-storepass", pw, "-file", interCer.toString(), "-trustcacerts", "-noprompt");
        keytool("-importcert", "-alias", "leaf", "-keystore", leafKs.toString(),
                "-storepass", pw, "-file", leafCer.toString(), "-trustcacerts", "-noprompt");

        final KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var in = new FileInputStream(leafKs.toFile())) {
            ks.load(in, STORE_PASSWORD);
        }
        final PrivateKey leafKey = (PrivateKey) ks.getKey("leaf", STORE_PASSWORD);

        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final X509Certificate rootCert;
        try (var in = new FileInputStream(rootCer.toFile())) {
            rootCert = (X509Certificate) cf.generateCertificate(in);
        }

        return new CertChain(Files.readAllBytes(leafCer), Files.readAllBytes(interCer), rootCert, leafKey);
    }

    private static void keytool(final String... args) throws Exception {
        final String keytoolPath = System.getProperty("java.home") + "/bin/keytool";
        final var command = new ArrayList<String>();
        command.add(keytoolPath);
        command.addAll(Arrays.asList(args));
        final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("keytool failed (exit " + exit + "): " + output);
        }
    }

    private static String b64Url(final byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String b64Url(final String s) {
        return b64Url(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String jws(final CertChain chain, final String alg, final String payloadJson,
                              final String signatureAlgorithm) throws Exception {
        final ObjectNode header = MAPPER.createObjectNode();
        header.put("alg", alg);
        final var x5c = header.putArray("x5c");
        x5c.add(Base64.getEncoder().encodeToString(chain.leafCertDer()));
        x5c.add(Base64.getEncoder().encodeToString(chain.interCertDer()));

        final String signingInput = b64Url(MAPPER.writeValueAsString(header)) + "." + b64Url(payloadJson);

        final Signature sig = Signature.getInstance(signatureAlgorithm);
        sig.initSign(chain.leafPrivateKey());
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + b64Url(sig.sign());
    }

    /** Signed the way Apple does: ES256 with a raw R||S signature. */
    private static String appleStyleJws(final CertChain chain, final String payloadJson) throws Exception {
        return jws(chain, "ES256", payloadJson, "SHA256withECDSAinP1363Format");
    }

    private void trust(final CertChain chain) {
        ReflectionTestUtils.setField(verifier, "appleRootCa", chain.rootCert());
    }

    @Nested
    @DisplayName("verifyAndDecode")
    class VerifyAndDecode {

        @Test
        @DisplayName("ES256 raw R||S signature + chain rooted in the pinned CA → decoded payload")
        void validChain_returnsDecodedPayload() throws Exception {
            trust(trustedChain);

            final JsonNode result = verifier.verifyAndDecode(appleStyleJws(trustedChain, PAYLOAD));

            assertThat(result.path("notificationType").asString()).isEqualTo("TEST");
        }

        @Test
        @DisplayName("many fresh signatures all verify (signature bytes are always 64, whatever the nonce)")
        void manySignatures_allVerify() throws Exception {
            trust(trustedChain);

            for (int i = 0; i < 40; i++) {
                final JsonNode result = verifier.verifyAndDecode(appleStyleJws(trustedChain, "{\"n\":" + i + "}"));
                assertThat(result.path("n").asInt()).isEqualTo(i);
            }
        }

        @Test
        @DisplayName("DER-encoded signature (not what Apple sends) → SecurityException")
        void derSignature_rejected() throws Exception {
            trust(trustedChain);
            final String derJws = jws(trustedChain, "ES256", PAYLOAD, "SHA256withECDSA");

            assertThatThrownBy(() -> verifier.verifyAndDecode(derJws))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("not 3 dot-separated parts → SecurityException")
        void malformedFormat_throwsSecurityException() {
            assertThatThrownBy(() -> verifier.verifyAndDecode("only.two"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("3 parts");
        }

        @Test
        @DisplayName("null payload → SecurityException")
        void nullPayload_throwsSecurityException() {
            assertThatThrownBy(() -> verifier.verifyAndDecode(null))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("garbage that is not base64 JSON → SecurityException, never another exception type")
        void garbage_throwsSecurityException() {
            assertThatThrownBy(() -> verifier.verifyAndDecode("###.###.###"))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("missing x5c in header → SecurityException")
        void missingX5c_throwsSecurityException() {
            final String jws = b64Url("{\"alg\":\"ES256\"}") + "." + b64Url("{}") + "." + b64Url("sig");

            assertThatThrownBy(() -> verifier.verifyAndDecode(jws))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("x5c");
        }

        @Test
        @DisplayName("alg other than ES256 → SecurityException")
        void wrongAlg_throwsSecurityException() throws Exception {
            trust(trustedChain);
            final String jws = jws(trustedChain, "none", PAYLOAD, "SHA256withECDSAinP1363Format");

            assertThatThrownBy(() -> verifier.verifyAndDecode(jws))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("ES256");
        }

        @Test
        @DisplayName("tampered signature (a middle byte changed) → SecurityException, deterministically")
        void tamperedSignature_throwsSecurityException() throws Exception {
            trust(trustedChain);
            final String[] parts = appleStyleJws(trustedChain, PAYLOAD).split("\\.");
            final byte[] sig = Base64.getUrlDecoder().decode(parts[2]);
            sig[sig.length / 2] ^= 0x01;
            final String tamperedJws = parts[0] + "." + parts[1] + "." + b64Url(sig);

            assertThatThrownBy(() -> verifier.verifyAndDecode(tamperedJws))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("signature verification failed");
        }

        @Test
        @DisplayName("tampered payload → SecurityException")
        void tamperedPayload_throwsSecurityException() throws Exception {
            trust(trustedChain);
            final String[] parts = appleStyleJws(trustedChain, PAYLOAD).split("\\.");
            final String tamperedJws = parts[0] + "." + b64Url("{\"notificationType\":\"SUBSCRIBED\"}") + "." + parts[2];

            assertThatThrownBy(() -> verifier.verifyAndDecode(tamperedJws))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("chain not rooted in the pinned Apple Root CA → SecurityException")
        void untrustedChain_verificationFails() throws Exception {
            trust(trustedChain);

            assertThatThrownBy(() -> verifier.verifyAndDecode(appleStyleJws(rogueChain, PAYLOAD)))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("valid chain without the Apple marker extensions → SecurityException")
        void chainWithoutAppleMarkers_rejected() throws Exception {
            trust(noMarkerChain);

            assertThatThrownBy(() -> verifier.verifyAndDecode(appleStyleJws(noMarkerChain, PAYLOAD)))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("marker");
        }
    }
}
