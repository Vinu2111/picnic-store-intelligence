package com.picnic.psi.controller;

import com.picnic.psi.model.PurchaseHistory;
import com.picnic.psi.repository.PurchaseHistoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Purchase Simulation", description = "Simulate real-time customer purchases")
public class PurchaseController {

    private final PurchaseHistoryRepository purchaseHistoryRepository;

    public PurchaseController(PurchaseHistoryRepository purchaseHistoryRepository) {
        this.purchaseHistoryRepository = purchaseHistoryRepository;
    }

    // Simulate a purchase — increments count and updates recency
    @PostMapping("/{customerId}/purchase")
    @Operation(summary = "Simulate a purchase for a customer in a category")
    public ResponseEntity<Map<String, Object>> simulatePurchase(
            @PathVariable String customerId,
            @RequestParam String category) {

        List<PurchaseHistory> history = purchaseHistoryRepository.findByCustomerId(customerId);

        // Find existing record for this category
        PurchaseHistory record = history.stream()
                .filter(h -> h.getCategory().equals(category))
                .findFirst()
                .orElse(null);

        if (record != null) {
            // Update existing record
            record.setPurchaseCount(record.getPurchaseCount() + 1);
            record.setLastPurchasedAt(LocalDateTime.now());
            purchaseHistoryRepository.save(record);
        } else {
            // Create new record — this customer never bought from this category before
            record = PurchaseHistory.builder()
                    .customerId(customerId)
                    .category(category)
                    .purchaseCount(1)
                    .lastPurchasedAt(LocalDateTime.now())
                    .build();
            purchaseHistoryRepository.save(record);
        }

        return ResponseEntity.ok(Map.of(
                "customerId", customerId,
                "category", category,
                "newPurchaseCount", record.getPurchaseCount(),
                "message", "Purchase recorded. Reload store to see updated ranking."
        ));
    }

    // Get current purchase history for a customer
    @GetMapping("/{customerId}/history")
    @Operation(summary = "Get purchase history for a customer")
    public ResponseEntity<List<PurchaseHistory>> getHistory(@PathVariable String customerId) {
        return ResponseEntity.ok(purchaseHistoryRepository.findByCustomerId(customerId));
    }

    // Register a brand new customer with zero history
    @PostMapping("/{customerId}/register")
    @Operation(summary = "Register a new customer with zero purchase history")
    public ResponseEntity<Map<String, Object>> registerCustomer(@PathVariable String customerId) {
        List<PurchaseHistory> existing = purchaseHistoryRepository.findByCustomerId(customerId);
        if (!existing.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "customerId", customerId,
                    "message", "Customer already exists",
                    "purchaseCount", existing.size()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "customerId", customerId,
                "message", "New customer registered. No purchase history yet. All sections will score 0.",
                "purchaseCount", 0
        ));
    }
}
