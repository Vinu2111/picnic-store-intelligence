package com.picnic.psi.service;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import com.picnic.psi.model.PurchaseHistory;
import com.picnic.psi.model.Rule;
import com.picnic.psi.model.Section;
import com.picnic.psi.repository.PurchaseHistoryRepository;
import com.picnic.psi.repository.RuleRepository;
import org.springframework.stereotype.Service;

/**
 * Rule engine that determines whether a section should be visible
 * based on its rules (day of week, time of day, or always-on) and
 * customer behaviour thresholds.
 */
@Service
public class RuleEngineService {

    private static final ZoneId STORE_ZONE = ZoneId.of("Europe/Amsterdam");
    private static final Map<String, Integer> UNLOCK_THRESHOLDS = Map.of(
            "recipes", 3,
            "weekend-promo", 5
    );

    private final RuleRepository ruleRepository;
    private final Clock clock;

    public RuleEngineService(RuleRepository ruleRepository, Clock clock) {
        this.ruleRepository = ruleRepository;
        this.clock = clock;
    }

    /**
     * Evaluates whether a section should be visible right now by loading its rules.
     */
    public boolean isSectionVisible(String sectionId, Section section) {
        if (section.isAlwaysVisible()) {
            return true;
        }
        return isSectionVisible(section, ruleRepository.findBySectionId(sectionId));
    }

    /**
     * Evaluates whether a section should be visible using pre-fetched rules (zero DB queries).
     */
    public boolean isSectionVisible(Section section, List<Rule> rules) {
        if (section.isAlwaysVisible()) {
            return true;
        }

        if (rules == null || rules.isEmpty()) {
            return false;
        }

        ZonedDateTime storeNow = ZonedDateTime.ofInstant(clock.instant(), STORE_ZONE);
        String today = storeNow.getDayOfWeek().name();
        int currentHour = storeNow.getHour();

        for (Rule rule : rules) {
            if (!"SHOW".equals(rule.getAction())) {
                continue;
            }
            String conditionType = rule.getConditionType();
            String conditionValue = rule.getConditionValue();

            if ("ALWAYS".equals(conditionType)) {
                return true;
            }
            if ("DAY_OF_WEEK".equals(conditionType) && today.equals(conditionValue)) {
                return true;
            }
            if ("TIME_OF_DAY".equals(conditionType)) {
                if (("MORNING".equals(conditionValue) && currentHour < 12)
                        || ("EVENING".equals(conditionValue) && currentHour >= 17)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Evaluates behaviour-based unlocking using an already-computed purchase count.
     */
    public boolean isUnlockedByBehaviour(String sectionId, int totalPurchases) {
        Integer threshold = UNLOCK_THRESHOLDS.get(sectionId);
        return threshold != null && totalPurchases >= threshold;
    }

    /**
     * Evaluates behaviour-based unlocking by querying customer history when not pre-fetched.
     */
    public boolean isUnlockedByBehaviour(String sectionId, String customerId,
                                         PurchaseHistoryRepository purchaseHistoryRepository) {
        if (customerId == null || customerId.isBlank()) {
            return false;
        }
        int totalPurchases = purchaseHistoryRepository.findByCustomerId(customerId).stream()
                .mapToInt(PurchaseHistory::getPurchaseCount)
                .sum();
        return isUnlockedByBehaviour(sectionId, totalPurchases);
    }

    /**
     * Returns unlock progress percentage (0-100) for sections gated by purchase count.
     */
    public int calculateUnlockProgress(String sectionId, int totalPurchases) {
        Integer threshold = UNLOCK_THRESHOLDS.get(sectionId);
        if (threshold == null || threshold <= 0) {
            return 0;
        }
        return Math.min((totalPurchases * 100) / threshold, 100);
    }
}
