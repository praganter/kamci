package dev.batuhanyetgin.authservice.service;

import java.security.PublicKey;

/** Source of the public keys that Google signs Pub/Sub push tokens with. */
public interface PubSubKeyProvider {

    /** The key with this {@code kid}, or null when Google does not publish one. */
    PublicKey keyFor(String keyId);
}
