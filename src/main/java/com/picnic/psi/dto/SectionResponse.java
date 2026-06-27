package com.picnic.psi.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single section within a page.
 * Contains visibility info, ranking score, and optionally a list of products.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionResponse {

    /** Unique section identifier, e.g. "dairy", "banner" */
    private String sectionId;

    /** Section type: BANNER, PRODUCT_GRID, PROMO, or RECIPE */
    private String type;

    /** Display title shown above the section, e.g. "Dairy & Eggs" */
    private String title;

    /** Display order on the page (lower = appears higher) */
    private int order;

    /** Whether this section is visible after rule engine evaluation */
    private boolean visible;

    /** Ranking score calculated by the personalization algorithm (0.0 if not ranked) */
    private double score;

    // Business impact signal derived from the relevance score
    private String businessSignal;

    /** Products in this section — populated only for PRODUCT_GRID and RECIPE types */
    private List<ProductResponse> products;
}
