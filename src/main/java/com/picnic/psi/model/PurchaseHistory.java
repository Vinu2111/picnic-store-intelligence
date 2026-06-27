package com.picnic.psi.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tracks how many times a customer has purchased items in a given category.
 * Used to personalize sections based on buying habits.
 */
@Entity
@Table(name = "purchase_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseHistory {

    /** Auto-generated record ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique customer identifier */
    private String customerId;

    /** Product category the customer purchased from, e.g. "fruits", "dairy" */
    private String category;

    /** Total number of purchases in this category */
    private int purchaseCount;

    /** When the customer last purchased from this category */
    private LocalDateTime lastPurchasedAt;
}
