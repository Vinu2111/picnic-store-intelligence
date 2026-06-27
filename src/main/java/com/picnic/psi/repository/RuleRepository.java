package com.picnic.psi.repository;

import java.util.List;

import com.picnic.psi.model.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing Rule entities.
 */
@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {

    // Fetches all visibility rules for a section, used by the rule engine to decide SHOW/HIDE
    List<Rule> findBySectionId(String sectionId);
}
