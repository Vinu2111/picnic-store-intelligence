package com.picnic.psi.repository;

import java.util.List;

import com.picnic.psi.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing Section entities.
 */
@Repository
public interface SectionRepository extends JpaRepository<Section, String> {

    // Fetches all sections belonging to a page, sorted by displayOrder so they render in the correct sequence
    List<Section> findByPageIdOrderByDisplayOrderAsc(String pageId);
}
