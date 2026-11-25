package com.picnic.psi.dto;

/**
 * Response returned when resetting a customer's purchase history.
 */
public record HistoryResetResponse(
        String customerId,
        String message,
        int deletedRecords
) {
}
