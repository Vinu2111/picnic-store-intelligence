package com.picnic.psi.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.picnic.psi.model.Rule;
import com.picnic.psi.model.Section;
import com.picnic.psi.repository.RuleRepository;
import org.springframework.stereotype.Service;

/**
 * Rule engine that determines whether a section should be visible
 * based on its rules (day of week, time of day, or always-on).
 */
@Service
public class RuleEngineService {

    private final RuleRepository ruleRepository;

    public RuleEngineService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /**
     * Evaluates whether a section should be visible right now.
     *
     * @param sectionId the ID of the section to check
     * @param section   the Section entity (used for the alwaysVisible flag)
     * @return true if the section should be shown, false if hidden
     */
    public boolean isSectionVisible(String sectionId, Section section) {

        // Sections marked as alwaysVisible bypass the rule engine entirely
        if (section.isAlwaysVisible()) {
            return true;
        }

        // Fetch all rules defined for this section
        List<Rule> rules = ruleRepository.findBySectionId(sectionId);

        // No rules defined → hidden by default (fail-closed)
        if (rules.isEmpty()) {
            return false;
        }

        // Evaluate each rule — first matching SHOW rule wins
        for (Rule rule : rules) {

            // ALWAYS rule: section is unconditionally visible
            if ("ALWAYS".equals(rule.getConditionType()) && "SHOW".equals(rule.getAction())) {
                return true;
            }

            // DAY_OF_WEEK rule: compare current day name to the condition value
            // e.g., conditionValue = "SATURDAY" matches on Saturdays
            if ("DAY_OF_WEEK".equals(rule.getConditionType())) {
                String today = LocalDate.now().getDayOfWeek().name(); // e.g., "MONDAY", "SATURDAY"
                if (today.equals(rule.getConditionValue()) && "SHOW".equals(rule.getAction())) {
                    return true;
                }
            }

            // TIME_OF_DAY rule: check if current hour falls in MORNING or EVENING
            // MORNING = before 12:00, EVENING = 17:00 or later
            if ("TIME_OF_DAY".equals(rule.getConditionType())) {
                int currentHour = LocalTime.now().getHour();
                boolean matches = false;

                if ("MORNING".equals(rule.getConditionValue()) && currentHour < 12) {
                    matches = true;
                }
                if ("EVENING".equals(rule.getConditionValue()) && currentHour >= 17) {
                    matches = true;
                }

                if (matches && "SHOW".equals(rule.getAction())) {
                    return true;
                }
            }
        }

        // No rule matched → section stays hidden
        return false;
    }

    // Smart unlocking — sections unlock based on customer purchase behaviour
    // This connects the ranker and rule engine together
    public boolean isUnlockedByBehaviour(String sectionId, String customerId,
            com.picnic.psi.repository.PurchaseHistoryRepository purchaseHistoryRepository) {

        if (customerId == null || customerId.isBlank()) {
            return false;
        }

        var history = purchaseHistoryRepository.findByCustomerId(customerId);
        int totalPurchases = history.stream()
                .mapToInt(com.picnic.psi.model.PurchaseHistory::getPurchaseCount)
                .sum();

        // Recipes unlocks after 3 total purchases — customer is engaged
        if (sectionId.equals("recipes") && totalPurchases >= 3) {
            return true;
        }

        // Weekend promo unlocks after 5 total purchases — loyal customer
        if (sectionId.equals("weekend-promo") && totalPurchases >= 5) {
            return true;
        }

        return false;
    }
}
