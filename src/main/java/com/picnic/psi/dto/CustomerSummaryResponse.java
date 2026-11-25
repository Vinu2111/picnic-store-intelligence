package com.picnic.psi.dto;

/**
 * High-level customer profile summary derived from purchase history.
 */
public record CustomerSummaryResponse(
        String customerId,
        int totalPurchases,
        String topCategory,
        String status,
        String preference,
        int recipesUnlockProgress,
        int promoUnlockProgress
) {
}
