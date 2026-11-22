package dev.batuhanyetgin.authservice.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Verifies Apple App Store Server Notifications V2 (signed JWS, RFC 7515).
 *
 * <p>The header carries an {@code x5c} chain (leaf, intermediate, root). We check:
 * <ol>
 *   <li>{@code alg} is ES256;</li>
 *   <li>the leaf is signed by the intermediate and the intermediate by the pinned Apple Root CA G3
 *       (the root inside the token is ignored), all within their validity period;</li>
 *   <li>the Apple marker extensions are present on the leaf and the intermediate. Without them any
 *       certificate Apple issued to a developer would pass the chain check;</li>
 *   <li>the ES256 signature, which JWS encodes as raw R||S (not DER).</li>
 * </ol>
 * Every failure is a {@link SecurityException}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleJwsVerifier {

    // Apple marker extensions for notification signing certificates
    static final String LEAF_MARKER_OID = "1.2.840.113635.100.6.11.1";
    static final String INTERMEDIATE_MARKER_OID = "1.2.840.113635.100.6.2.1";

    private final JsonMapper jsonMapper;

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
     * Verifies the signed JWS from Apple and returns the decoded payload.
     *
     * @throws SecurityException if the format, certificate chain or signature is not valid
     */
    public JsonNode verifyAndDecode(final String signedPayload) {
        try {
            return verify(signedPayload);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Invalid Apple JWS: " + e.getClass().getSimpleName(), e);
        }
    }

    private JsonNode verify(final String signedPayload) throws Exception {
        final String[] parts = signedPayload == null ? new String[0] : signedPayload.split("\\.", -1);
        if (parts.length != 3) {
            throw new SecurityException("Invalid JWS format: expected 3 parts");
        }

        final JsonNode header = decodeBase64Json(parts[0]);
        if (!"ES256".equals(header.path("alg").asString(null))) {
            throw new SecurityException("Unsupported JWS algorithm: expected ES256");
        }

        final JsonNode x5c = header.path("x5c");
        if (!x5c.isArray() || x5c.size() < 2) {
            throw new SecurityException("Missing or incomplete x5c certificate chain in Apple JWS");
        }

        final X509Certificate leafCert = parseCertificate(x5c.get(0).asString());
        final X509Certificate intermCert = parseCertificate(x5c.get(1).asString());

        verifyChain(leafCert, intermCert);

        final String signingInput = parts[0] + "." + parts[1];
        verifySignature(signingInput, parts[2], leafCert.getPublicKey());

        return decodeBase64Json(parts[1]);
    }

    private void verifyChain(final X509Certificate leaf, final X509Certificate intermediate) throws Exception {
        appleRootCa.checkValidity();
        intermediate.checkValidity();
        leaf.checkValidity();
        if (intermediate.getBasicConstraints() < 0) {
            throw new SecurityException("Intermediate certificate is not a CA");
        }
        leaf.verify(intermediate.getPublicKey());
        intermediate.verify(appleRootCa.getPublicKey());
        if (leaf.getExtensionValue(LEAF_MARKER_OID) == null
                || intermediate.getExtensionValue(INTERMEDIATE_MARKER_OID) == null) {
            throw new SecurityException("Certificate chain lacks the Apple notification marker extensions");
        }
    }

    private void verifySignature(final String signingInput, final String signatureB64,
                                 final PublicKey publicKey) throws Exception {
        final byte[] sigBytes = Base64.getUrlDecoder().decode(signatureB64);
        if (sigBytes.length != 64) {
            throw new SecurityException("ES256 signature must be 64 bytes (R||S)");
        }
        final Signature sig = Signature.getInstance("SHA256withECDSAinP1363Format");
        sig.initVerify(publicKey);
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        if (!sig.verify(sigBytes)) {
            throw new SecurityException("Apple JWS signature verification failed");
        }
    }

    private X509Certificate parseCertificate(final String base64Der) throws Exception {
        final byte[] der = Base64.getDecoder().decode(base64Der);
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(der));
    }

    private JsonNode decodeBase64Json(final String encoded) {
        final byte[] decoded = Base64.getUrlDecoder().decode(encoded);
        return jsonMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
    }
}
