package com.picnic.psi.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.picnic.psi.model.PurchaseHistory;
import com.picnic.psi.repository.PurchaseHistoryRepository;
import org.springframework.stereotype.Service;

/**
 * Ranking service that calculates a personalization score for each section
 * based on the customer's purchase history and current context.
 *
 * Scoring formula:
 *   score = (frequencyScore × 0.5) + (recencyScore × 0.3) + (timeOfDayScore × 0.2)
 */
@Service
public class RankerService {

    private final PurchaseHistoryRepository purchaseHistoryRepository;

    public RankerService(PurchaseHistoryRepository purchaseHistoryRepository) {
        this.purchaseHistoryRepository = purchaseHistoryRepository;
    }

    /**
     * Calculates a personalization score (0.0 to 1.0) for a customer–category pair.
     *
     * @param customerId      the customer to personalize for (null = anonymous)
     * @param sectionCategory the category of the section being scored
     * @return the weighted score, rounded to 2 decimal places
     */
    public double calculateScore(String customerId, String sectionCategory) {

        // No personalization for anonymous users
        if (customerId == null || customerId.isBlank()) {
            return 0.0;
        }

        // Banner and promo sections have no purchase history category match.
        // Give banner a high default score since it is always visible and
        // should always appear prominent regardless of customer behaviour.
        if (sectionCategory.equals("banner")) {
            return 1.0;
        }

        // Weekend promo has no purchase history — give it a neutral mid score
        if (sectionCategory.equals("weekend-promo")) {
            return 0.5;
        }

        // Fetch all purchase history for this customer
        List<PurchaseHistory> historyList = purchaseHistoryRepository.findByCustomerId(customerId);

        // Find the record matching the section's category
        Optional<PurchaseHistory> matchingHistory = historyList.stream()
                .filter(history -> history.getCategory().equals(sectionCategory))
                .findFirst();

        // No purchase history for this category → no score
        if (matchingHistory.isEmpty()) {
            return 0.0;
        }

        PurchaseHistory history = matchingHistory.get();

        // =============================================
        // 1. Frequency Score (weight: 0.5)
        // Normalize purchase count to 0.0-1.0 range, cap at 10 purchases
        // =============================================
        double frequencyScore = Math.min(history.getPurchaseCount() / 10.0, 1.0);

        // =============================================
        // 2. Recency Score (weight: 0.3)
        // Recent purchases score higher — drops to 0 after 30 days
        // =============================================
        long daysSince = ChronoUnit.DAYS.between(history.getLastPurchasedAt(), LocalDateTime.now());
        double recencyScore = Math.max(0.0, 1.0 - (daysSince / 30.0));

        // =============================================
        // 3. Time-of-Day Score (weight: 0.2)
        // Boost categories that are contextually relevant at this time of day
        // =============================================
        int currentHour = LocalTime.now().getHour();
        double timeOfDayScore;

        if ("dairy".equals(sectionCategory) && currentHour >= 6 && currentHour <= 10) {
            // People tend to buy dairy in the morning (breakfast items)
            timeOfDayScore = 1.0;
        } else if ("recipes".equals(sectionCategory) && currentHour >= 16 && currentHour <= 20) {
            // People plan meals in the evening (dinner prep)
            timeOfDayScore = 1.0;
        } else {
            // Neutral — no time-of-day boost for this category right now
            timeOfDayScore = 0.5;
        }

        // =============================================
        // Final weighted score
        // =============================================
        double finalScore = (frequencyScore * 0.5) + (recencyScore * 0.3) + (timeOfDayScore * 0.2);

        // Round to 2 decimal places
        finalScore = Math.round(finalScore * 100.0) / 100.0;

        // Log the score — useful for live demos and debugging
        System.out.println("[Ranker] customer=" + customerId
                + " category=" + sectionCategory
                + " score=" + finalScore
                + " (freq=" + frequencyScore
                + " recency=" + recencyScore
                + " timeOfDay=" + timeOfDayScore + ")");

        return finalScore;
    }

    // Translate a numerical score into a business impact signal
    // This helps non-technical stakeholders understand what the score means
    public String getBusinessSignal(double score) {
        if (score >= 0.80) {
            return "High purchase intent";
        } else if (score >= 0.60) {
            return "Strong conversion candidate";
        } else if (score >= 0.40) {
            return "Neutral visibility";
        } else if (score > 0.0) {
            return "Low engagement signal";
        } else {
            return "No purchase history";
        }
    }
}
