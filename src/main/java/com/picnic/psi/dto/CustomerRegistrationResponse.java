package com.picnic.psi.dto;

/**
 * Response returned when registering or checking a customer profile.
 */
public record CustomerRegistrationResponse(
        String customerId,
        String message,
        int purchaseCount
) {
}
