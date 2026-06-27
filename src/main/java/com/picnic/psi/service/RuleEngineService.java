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
}
