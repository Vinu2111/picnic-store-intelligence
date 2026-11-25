package com.picnic.psi.dto;

/**
 * Transparent breakdown of how a section's personalization score was computed.
 */
public record ScoreBreakdown(
        double frequencyScore,
        double recencyScore,
        double timeOfDayScore,
        int purchaseCount,
        long daysSinceLastPurchase,
        String reason
) {
}
