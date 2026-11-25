package com.picnic.psi.dto;

/**
 * Response returned after simulating a customer purchase.
 */
public record PurchaseSimulationResponse(
        String customerId,
        String category,
        int newPurchaseCount,
        String message
) {
}
