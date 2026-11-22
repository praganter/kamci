package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.authservice.enums.SubscriptionPlan;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Store settings for in-app purchases. Everything is optional so the service starts without them;
 * the verification paths that need a missing value answer with an explicit "not configured" error.
 */
@Getter
@Component
public class IapSettings {

    /** Bundle identifier of the iOS app; receipts and notifications for other apps are rejected. */
    private final String appleBundleId;
    /** Sandbox receipts and notifications are free to produce, so they are accepted only when this is set. */
    private final boolean appleAllowSandbox;
    private final Map<String, SubscriptionPlan> productPlans = new HashMap<>();

    public IapSettings(@Value("${APPLE_BUNDLE_ID:}") final String appleBundleId,
                       @Value("${APPLE_ALLOW_SANDBOX:false}") final boolean appleAllowSandbox,
                       @Value("${IAP_MONTHLY_PRODUCT_IDS:}") final String monthlyProductIds,
                       @Value("${IAP_YEARLY_PRODUCT_IDS:}") final String yearlyProductIds) {
        this.appleBundleId = appleBundleId.trim();
        this.appleAllowSandbox = appleAllowSandbox;
        addProducts(monthlyProductIds, SubscriptionPlan.MONTHLY);
        addProducts(yearlyProductIds, SubscriptionPlan.YEARLY);
    }

    private void addProducts(final String csv, final SubscriptionPlan plan) {
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .forEach(id -> productPlans.put(id, plan));
    }

    public boolean isAppleConfigured() {
        return !appleBundleId.isEmpty();
    }

    public boolean productsConfigured() {
        return !productPlans.isEmpty();
    }

    public Optional<SubscriptionPlan> planFor(final String productId) {
        return Optional.ofNullable(productId).map(productPlans::get);
    }
}
