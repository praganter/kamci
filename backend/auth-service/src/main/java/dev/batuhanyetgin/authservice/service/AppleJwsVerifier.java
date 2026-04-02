package dev.batuhanyetgin.authservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Verifies Apple App Store Server Notifications V2 (SSNS V2) JWS payloads.
 *
 * Apple signs each notification as a JWS (RFC 7515). The header contains an
 * {@code x5c} array with the certificate chain (leaf → intermediate → root).
 * We verify:
 *   1. The certificate chain up to Apple Root CA G3.
 *   2. The ES256 signature using the leaf certificate's public key.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleJwsVerifier {

    private final ObjectMapper objectMapper;

    private X509Certificate appleRootCa;

    @PostConstruct
    void loadAppleRootCa() throws Exception {
        final ClassPathResource resource = new ClassPathResource("apple-root-ca-g3.pem");
        try (final InputStream is = resource.getInputStream()) {
            appleRootCa = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(is);
            log.info("Apple Root CA G3 loaded: subject={}", appleRootCa.getSubjectX500Principal().getName());
        }
    }

    /**
     * Verifies the signed JWS payload from Apple and returns the decoded payload as a JsonNode.
     *
     * @throws SecurityException if signature or certificate chain verification fails
     */
    public JsonNode verifyAndDecode(final String signedPayload) throws Exception {
        final String[] parts = signedPayload.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWS format: expected 3 parts");
        }

        final JsonNode header = decodeBase64Json(parts[0]);

        final JsonNode x5c = header.path("x5c");
        if (!x5c.isArray() || x5c.size() < 2) {
            throw new SecurityException("Missing or incomplete x5c certificate chain in Apple JWS");
        }

        final X509Certificate leafCert = parseCertificate(x5c.get(0).asText());
        final X509Certificate intermCert = parseCertificate(x5c.get(1).asText());

        verifyChain(leafCert, intermCert);

        final String signingInput = parts[0] + "." + parts[1];
        verifySignature(signingInput, parts[2], leafCert.getPublicKey());

        return decodeBase64Json(parts[1]);
    }

    private void verifyChain(final X509Certificate leaf, final X509Certificate intermediate) throws Exception {
        leaf.verify(intermediate.getPublicKey());
        intermediate.verify(appleRootCa.getPublicKey());
    }

    private void verifySignature(final String signingInput, final String signatureB64,
                                  final PublicKey publicKey) throws Exception {
        final Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initVerify(publicKey);
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        final byte[] sigBytes = Base64.getUrlDecoder().decode(signatureB64);
        if (!sig.verify(sigBytes)) {
            throw new SecurityException("Apple JWS signature verification failed");
        }
    }

    private X509Certificate parseCertificate(final String base64Der) throws Exception {
        final byte[] der = Base64.getDecoder().decode(base64Der);
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(der));
    }

    private JsonNode decodeBase64Json(final String encoded) throws Exception {
        final byte[] decoded = Base64.getUrlDecoder().decode(encoded);
        return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
    }
}
