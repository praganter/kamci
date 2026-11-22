package dev.batuhanyetgin.authservice.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
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
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Builds real EC certificate chains with {@code keytool} (no extra crypto dependency) to
 * exercise AppleJwsVerifier's actual signature + chain-of-trust verification, not mocks of it.
 */
class AppleJwsVerifierTest {

    private static final char[] STORE_PASSWORD = "changeit".toCharArray();
    private static final JsonMapper MAPPER = new JsonMapper();

    private static Path tempDir;
    private static CertChain trustedChain;
    private static CertChain rogueChain;

    private final AppleJwsVerifier verifier = new AppleJwsVerifier(MAPPER);

    private record CertChain(byte[] leafCertDer, byte[] interCertDer, X509Certificate rootCert,
                             PrivateKey leafPrivateKey) {
    }

    @BeforeAll
    static void buildCertChains() throws Exception {
        tempDir = Files.createTempDirectory("apple-jws-test");
        trustedChain = buildChain("trusted");
        rogueChain = buildChain("rogue");
    }

    @AfterAll
    static void cleanup() throws IOException {
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> p.toFile().delete());
        }
    }

    /** root (self-signed CA) -> intermediate (CA) -> leaf, all EC/P-256, via keytool. */
    private static CertChain buildChain(final String name) throws Exception {
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
                "-storepass", new String(STORE_PASSWORD), "-ext", "bc:c=ca:true");
        keytool("-exportcert", "-alias", "root", "-keystore", rootKs.toString(),
                "-storepass", new String(STORE_PASSWORD), "-file", rootCer.toString());

        keytool("-genkeypair", "-alias", "inter", "-keyalg", "EC", "-groupname", "secp256r1",
                "-sigalg", "SHA256withECDSA", "-dname", "CN=" + name + " Intermediate",
                "-validity", "3650", "-keystore", interKs.toString(), "-storetype", "PKCS12",
                "-storepass", new String(STORE_PASSWORD));
        keytool("-certreq", "-alias", "inter", "-keystore", interKs.toString(),
                "-storepass", new String(STORE_PASSWORD), "-file", interCsr.toString());
        keytool("-gencert", "-alias", "root", "-keystore", rootKs.toString(),
                "-storepass", new String(STORE_PASSWORD), "-infile", interCsr.toString(),
                "-outfile", interCer.toString(), "-validity", "3650", "-ext", "bc:c=ca:true");
        keytool("-importcert", "-alias", "root", "-keystore", interKs.toString(),
                "-storepass", new String(STORE_PASSWORD), "-file", rootCer.toString(), "-noprompt");
        keytool("-importcert", "-alias", "inter", "-keystore", interKs.toString(),
                "-storepass", new String(STORE_PASSWORD), "-file", interCer.toString(),
                "-trustcacerts", "-noprompt");

        keytool("-genkeypair", "-alias", "leaf", "-keyalg", "EC", "-groupname", "secp256r1",
                "-sigalg", "SHA256withECDSA", "-dname", "CN=" + name + " Leaf",
                "-validity", "3650", "-keystore", leafKs.toString(), "-storetype", "PKCS12",
                "-storepass", new String(STORE_PASSWORD));
        keytool("-certreq", "-alias", "leaf", "-keystore", leafKs.toString(),
                "-storepass", new String(STORE_PASSWORD), "-file", leafCsr.toString());
        keytool("-gencert", "-alias", "inter", "-keystore", interKs.toString(),
                "-storepass", new String(STORE_PASSWORD), "-infile", leafCsr.toString(),
                "-outfile", leafCer.toString(), "-validity", "3650");
        keytool("-importcert", "-alias", "root", "-keystore", leafKs.toString(),
                "-storepass", new String(STORE_PASSWORD), "-file", rootCer.toString(), "-noprompt");
        keytool("-importcert", "-alias", "inter", "-keystore", leafKs.toString(),
                "-storepass", new String(STORE_PASSWORD), "-file", interCer.toString(),
                "-trustcacerts", "-noprompt");
        keytool("-importcert", "-alias", "leaf", "-keystore", leafKs.toString(),
                "-storepass", new String(STORE_PASSWORD), "-file", leafCer.toString(),
                "-trustcacerts", "-noprompt");

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
        final var command = new java.util.ArrayList<String>();
        command.add(keytoolPath);
        command.addAll(java.util.Arrays.asList(args));
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

    private String buildSignedJws(final CertChain chain, final String payloadJson) throws Exception {
        final ObjectNode header = MAPPER.createObjectNode();
        header.put("alg", "ES256");
        final var x5c = header.putArray("x5c");
        x5c.add(Base64.getEncoder().encodeToString(chain.leafCertDer()));
        x5c.add(Base64.getEncoder().encodeToString(chain.interCertDer()));

        final String headerB64 = b64Url(MAPPER.writeValueAsString(header));
        final String payloadB64 = b64Url(payloadJson);
        final String signingInput = headerB64 + "." + payloadB64;

        final Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(chain.leafPrivateKey());
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        final String sigB64 = b64Url(sig.sign());

        return signingInput + "." + sigB64;
    }

    @Nested
    @DisplayName("verifyAndDecode")
    class VerifyAndDecode {

        @Test
        @DisplayName("valid signature + chain rooted in trusted CA → returns decoded payload")
        void validChain_returnsDecodedPayload() throws Exception {
            ReflectionTestUtils.setField(verifier, "appleRootCa", trustedChain.rootCert());
            final String jws = buildSignedJws(trustedChain, "{\"notificationType\":\"TEST\"}");

            final JsonNode result = verifier.verifyAndDecode(jws);

            assertThat(result.path("notificationType").asString()).isEqualTo("TEST");
        }

        @Test
        @DisplayName("not 3 dot-separated parts → IllegalArgumentException")
        void malformedFormat_throwsIllegalArgument() {
            assertThatThrownBy(() -> verifier.verifyAndDecode("only.two"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("missing x5c in header → SecurityException")
        void missingX5c_throwsSecurityException() {
            final String headerB64 = b64Url("{\"alg\":\"ES256\"}");
            final String payloadB64 = b64Url("{}");
            final String jws = headerB64 + "." + payloadB64 + "." + b64Url("sig");

            assertThatThrownBy(() -> verifier.verifyAndDecode(jws))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("x5c");
        }

        @Test
        @DisplayName("tampered signature → SecurityException")
        void tamperedSignature_throwsSecurityException() throws Exception {
            ReflectionTestUtils.setField(verifier, "appleRootCa", trustedChain.rootCert());
            final String jws = buildSignedJws(trustedChain, "{\"notificationType\":\"TEST\"}");
            final String[] parts = jws.split("\\.");
            final int lastIdx = parts[2].length() - 1;
            final char last = parts[2].charAt(lastIdx);
            final char flipped = last == 'A' ? 'B' : 'A';
            final String tamperedSig = parts[2].substring(0, lastIdx) + flipped;
            final String tamperedJws = parts[0] + "." + parts[1] + "." + tamperedSig;

            // corrupting the DER-encoded signature either fails ASN.1 parsing (SignatureException)
            // or parses but fails verification (SecurityException) — both mean tamper detection works
            assertThatThrownBy(() -> verifier.verifyAndDecode(tamperedJws))
                    .isInstanceOfAny(SecurityException.class, java.security.GeneralSecurityException.class);
        }

        @Test
        @DisplayName("chain not rooted in the trusted Apple Root CA → verification fails")
        void untrustedChain_verificationFails() throws Exception {
            // trusted root is the verifier's CA, but the JWS presents a chain signed by a different (rogue) root
            ReflectionTestUtils.setField(verifier, "appleRootCa", trustedChain.rootCert());
            final String jws = buildSignedJws(rogueChain, "{\"notificationType\":\"TEST\"}");

            assertThatThrownBy(() -> verifier.verifyAndDecode(jws))
                    .isInstanceOf(java.security.GeneralSecurityException.class);
        }
    }
}
