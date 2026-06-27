package com.picnic.psi.model;

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
 * Represents a visibility rule that controls when a section is shown or hidden.
 */
@Entity
@Table(name = "rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rule {

    /** Auto-generated rule ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The section this rule applies to (references Section.id) */
    private String sectionId;

    /** Type of condition: DAY_OF_WEEK, TIME_OF_DAY, or ALWAYS */
    private String conditionType;

    /** Value of the condition: SUNDAY, MONDAY, MORNING, EVENING, or ANY */
    private String conditionValue;

    /** Action to take when the condition is met: SHOW or HIDE */
    private String action;
}
