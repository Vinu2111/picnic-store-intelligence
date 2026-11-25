package com.picnic.psi.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.picnic.psi.dto.ScoreBreakdown;
import com.picnic.psi.model.PurchaseHistory;
import com.picnic.psi.repository.PurchaseHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(RankerService.class);
    private static final ZoneId STORE_ZONE = ZoneId.of("Europe/Amsterdam");

    public record ScoreResult(double score, ScoreBreakdown breakdown) {
    }

    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final Clock clock;

    public RankerService(PurchaseHistoryRepository purchaseHistoryRepository, Clock clock) {
        this.purchaseHistoryRepository = purchaseHistoryRepository;
        this.clock = clock;
    }

    /**
     * Calculates a personalization score (0.0 to 1.0) for a customer–category pair.
     */
    public double calculateScore(String customerId, String sectionCategory) {
        return calculateScoreDetails(customerId, sectionCategory).score();
    }

    /**
     * Calculates score and breakdown by fetching purchase history for the customer.
     */
    public ScoreResult calculateScoreDetails(String customerId, String sectionCategory) {
        if (customerId == null || customerId.isBlank()
                || "banner".equals(sectionCategory)
                || "weekend-promo".equals(sectionCategory)) {
            return calculateScoreDetails(customerId, sectionCategory, Optional.empty());
        }

        List<PurchaseHistory> historyList = purchaseHistoryRepository.findByCustomerId(customerId);
        Optional<PurchaseHistory> matchingHistory = historyList.stream()
                .filter(history -> history.getCategory().equals(sectionCategory))
                .findFirst();

        return calculateScoreDetails(customerId, sectionCategory, matchingHistory);
    }

    /**
     * Calculates score and breakdown using pre-fetched category purchase history (zero DB queries).
     */
    public ScoreResult calculateScoreDetails(String customerId,
                                             String sectionCategory,
                                             Optional<PurchaseHistory> matchingHistory) {
        if (customerId == null || customerId.isBlank()) {
            return new ScoreResult(0.0, new ScoreBreakdown(0.0, 0.0, 0.0, 0, 0L, "Anonymous visitor — default order"));
        }

        if ("banner".equals(sectionCategory)) {
            return new ScoreResult(1.0, new ScoreBreakdown(1.0, 1.0, 1.0, 0, 0L, "Pinned hero banner"));
        }

        if ("weekend-promo".equals(sectionCategory)) {
            return new ScoreResult(0.5, new ScoreBreakdown(0.5, 0.5, 0.5, 0, 0L, "Promotional slot (neutral priority)"));
        }

        if (matchingHistory.isEmpty()) {
            return new ScoreResult(0.0, new ScoreBreakdown(0.0, 0.0, 0.0, 0, 0L, "No purchase history in this category yet"));
        }

        PurchaseHistory history = matchingHistory.get();

        double frequencyScore = roundTwoDecimals(Math.min(history.getPurchaseCount() / 10.0, 1.0));
        ZonedDateTime storeNow = ZonedDateTime.ofInstant(clock.instant(), STORE_ZONE);
        long daysSince = Math.max(0L, ChronoUnit.DAYS.between(history.getLastPurchasedAt(), LocalDateTime.ofInstant(clock.instant(), STORE_ZONE)));
        double recencyScore = roundTwoDecimals(Math.max(0.0, 1.0 - (daysSince / 30.0)));

        int currentHour = storeNow.getHour();
        double timeOfDayScore;
        if ("dairy".equals(sectionCategory) && currentHour >= 6 && currentHour <= 10) {
            timeOfDayScore = 1.0;
        } else if ("recipes".equals(sectionCategory) && currentHour >= 16 && currentHour <= 20) {
            timeOfDayScore = 1.0;
        } else {
            timeOfDayScore = 0.5;
        }

        double finalScore = roundTwoDecimals((frequencyScore * 0.5) + (recencyScore * 0.3) + (timeOfDayScore * 0.2));

        LOG.debug("Scored customer={} category={} score={} (freq={}, recency={}, timeOfDay={})",
                customerId, sectionCategory, finalScore, frequencyScore, recencyScore, timeOfDayScore);

        String reason = String.format("%d purchases, last bought %d day(s) ago", history.getPurchaseCount(), daysSince);
        return new ScoreResult(
                finalScore,
                new ScoreBreakdown(
                        frequencyScore,
                        recencyScore,
                        timeOfDayScore,
                        history.getPurchaseCount(),
                        daysSince,
                        reason));
    }

    /**
     * Translates a numerical score into a business impact signal.
     */
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

    private static double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
