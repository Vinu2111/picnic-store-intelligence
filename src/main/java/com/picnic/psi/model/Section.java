package com.picnic.psi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a section within a page (e.g., a banner, product grid, promo block).
 */
@AllArgsConstructor
@Builder
@Data
@Entity
@NoArgsConstructor
@Table(name = "sections")
public class Section {

    /** Manually set section identifier, e.g. "home-banner", "weekend-promo" */
    @Id
    private String id;

    /** The page this section belongs to (references Page.id) */
    private String pageId;

    /** Section type: BANNER, PRODUCT_GRID, PROMO, or RECIPE */
    private String type;

    /** Display title shown above the section */
    private String title;

    /** Optional subtitle / marketing copy controlled by the backend */
    private String subtitle;

    /** Order in which this section appears on the page (lower = higher) */
    private int displayOrder;

    /** If true, this section is always shown regardless of rules */
    private boolean alwaysVisible;
}
