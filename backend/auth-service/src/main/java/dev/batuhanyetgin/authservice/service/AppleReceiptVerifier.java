package dev.batuhanyetgin.authservice.service;

import tools.jackson.databind.JsonNode;

/**
 * Verifies a purchase receipt sent by the iOS client.
 *
 * <p>The only implementation uses the legacy {@code verifyReceipt} endpoint, which Apple has
 * deprecated. It stays behind this interface until the app sends StoreKit 2 signed transactions,
 * which can be verified offline with {@link AppleJwsVerifier} (see SECURITY.md).
 */
public interface AppleReceiptVerifier {

    /** False while the shared secret or the bundle id is missing; {@link #verify} then fails. */
    boolean isConfigured();

    /**
     * Returns the newest receipt entry (with {@code product_id}, {@code original_transaction_id}
     * and {@code expires_date_ms}) of a receipt that was issued for this app.
     *
     * @throws dev.batuhanyetgin.commonmodule.exception.BaseException if the receipt is not valid or
     *         verification is not configured
     */
    JsonNode verify(String receiptData);
}
