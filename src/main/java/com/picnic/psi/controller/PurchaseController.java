package com.picnic.psi.controller;

import static java.util.Comparator.comparingInt;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import com.picnic.psi.dto.CustomerRegistrationResponse;
import com.picnic.psi.dto.CustomerSummaryResponse;
import com.picnic.psi.dto.HistoryResetResponse;
import com.picnic.psi.dto.PurchaseSimulationResponse;
import com.picnic.psi.model.PurchaseHistory;
import com.picnic.psi.repository.PurchaseHistoryRepository;
import com.picnic.psi.service.RuleEngineService;
import com.picnic.psi.service.StoreCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/customers")
@RestController
@Tag(name = "Purchase Simulation", description = "Simulate real-time customer purchases")
public class PurchaseController {

    private static final ZoneId STORE_ZONE = ZoneId.of("Europe/Amsterdam");

    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final StoreCatalogService catalogService;
    private final RuleEngineService ruleEngineService;
    private final Clock clock;

    public PurchaseController(PurchaseHistoryRepository purchaseHistoryRepository,
                              StoreCatalogService catalogService,
                              RuleEngineService ruleEngineService,
                              Clock clock) {
        this.purchaseHistoryRepository = purchaseHistoryRepository;
        this.catalogService = catalogService;
        this.ruleEngineService = ruleEngineService;
        this.clock = clock;
    }

    @Operation(summary = "Simulate a purchase for a customer in a category")
    @PostMapping("/{customerId}/purchase")
    public ResponseEntity<PurchaseSimulationResponse> simulatePurchase(
            @PathVariable String customerId,
            @RequestParam String category) {

        validateCustomerId(customerId);
        Set<String> validCategories = catalogService.getAllProductsGrouped().keySet();
        if (category == null || category.isBlank() || (!validCategories.isEmpty() && !validCategories.contains(category))) {
            throw new IllegalArgumentException("Unknown or blank category: " + category);
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), STORE_ZONE);
        PurchaseHistory record = purchaseHistoryRepository.findByCustomerIdAndCategory(customerId, category)
                .map(existing -> {
                    existing.setPurchaseCount(existing.getPurchaseCount() + 1);
                    existing.setLastPurchasedAt(now);
                    return existing;
                })
                .orElseGet(() -> PurchaseHistory.builder()
                        .customerId(customerId)
                        .category(category)
                        .purchaseCount(1)
                        .lastPurchasedAt(now)
                        .build());

        purchaseHistoryRepository.save(record);

        return ResponseEntity.ok(new PurchaseSimulationResponse(
                customerId,
                category,
                record.getPurchaseCount(),
                "Purchase recorded. Reload store to see updated ranking."));
    }

    @GetMapping("/{customerId}/history")
    @Operation(summary = "Get purchase history for a customer")
    public ResponseEntity<List<PurchaseHistory>> getHistory(@PathVariable String customerId) {
        validateCustomerId(customerId);
        return ResponseEntity.ok(purchaseHistoryRepository.findByCustomerId(customerId));
    }

    @Operation(summary = "Register a new customer with zero purchase history")
    @PostMapping("/{customerId}/register")
    public ResponseEntity<CustomerRegistrationResponse> registerCustomer(@PathVariable String customerId) {
        validateCustomerId(customerId);
        List<PurchaseHistory> existing = purchaseHistoryRepository.findByCustomerId(customerId);
        if (!existing.isEmpty()) {
            int totalPurchases = existing.stream().mapToInt(PurchaseHistory::getPurchaseCount).sum();
            return ResponseEntity.ok(new CustomerRegistrationResponse(
                    customerId,
                    "Customer already exists",
                    totalPurchases));
        }
        return ResponseEntity.ok(new CustomerRegistrationResponse(
                customerId,
                "New customer registered. No purchase history yet. All sections will score 0.",
                0));
    }

    @GetMapping("/{customerId}/summary")
    @Operation(summary = "Get customer summary — purchase count, top category, status")
    public ResponseEntity<CustomerSummaryResponse> getCustomerSummary(@PathVariable String customerId) {
        validateCustomerId(customerId);
        List<PurchaseHistory> history = purchaseHistoryRepository.findByCustomerId(customerId);

        int totalPurchases = history.stream()
                .mapToInt(PurchaseHistory::getPurchaseCount)
                .sum();

        String topCategory = history.stream()
                .max(comparingInt(PurchaseHistory::getPurchaseCount))
                .map(PurchaseHistory::getCategory)
                .orElse("none");

        String status;
        if (totalPurchases == 0) {
            status = "New Customer";
        } else if (totalPurchases < 3) {
            status = "Exploring";
        } else if (totalPurchases < 6) {
            status = "Active Shopper";
        } else {
            status = "Loyal Customer";
        }

        String preference = switch (topCategory) {
            case "dairy" -> "Dairy Lover";
            case "vegetables" -> "Health Conscious";
            case "recipes" -> "Home Chef";
            default -> "General Shopper";
        };

        return ResponseEntity.ok(new CustomerSummaryResponse(
                customerId,
                totalPurchases,
                topCategory,
                status,
                preference,
                ruleEngineService.calculateUnlockProgress("recipes", totalPurchases),
                ruleEngineService.calculateUnlockProgress("weekend-promo", totalPurchases)));
    }

    @DeleteMapping("/{customerId}/history")
    @Operation(summary = "Reset all purchase history for a customer")
    public ResponseEntity<HistoryResetResponse> resetHistory(@PathVariable String customerId) {
        validateCustomerId(customerId);
        List<PurchaseHistory> history = purchaseHistoryRepository.findByCustomerId(customerId);
        purchaseHistoryRepository.deleteAll(history);

        return ResponseEntity.ok(new HistoryResetResponse(
                customerId,
                "Purchase history reset successfully",
                history.size()));
    }

    private static void validateCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
    }
}
