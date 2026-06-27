package com.picnic.psi.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the full page response sent to the frontend.
 * Contains page metadata and a list of sections sorted by ranking score (highest first).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse {

    /** Page identifier, e.g. "home", "product-detail" */
    private String pageId;

    /** Display title of the page */
    private String title;

    /** Short description of what this page shows */
    private String description;

    /** The customer this page was personalized for (null if anonymous / no customerId provided) */
    private String customerId;

    /** Sections on this page, already sorted by score (highest first) after personalization */
    private List<SectionResponse> sections;
}
