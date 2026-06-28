package com.picnic.psi.service;

import java.util.Comparator;
import java.util.List;

import com.picnic.psi.dto.PageResponse;
import com.picnic.psi.dto.ProductResponse;
import com.picnic.psi.dto.SectionResponse;
import com.picnic.psi.model.Page;
import com.picnic.psi.model.Product;
import com.picnic.psi.model.Section;
import com.picnic.psi.repository.PageRepository;
import com.picnic.psi.repository.ProductRepository;
import com.picnic.psi.repository.SectionRepository;
import org.springframework.stereotype.Service;

/**
 * Core service that assembles a fully personalized page response.
 * Orchestrates the rule engine (visibility) and ranker (scoring) for each section.
 */
@Service
public class PageService {

    private final PageRepository pageRepository;
    private final SectionRepository sectionRepository;
    private final ProductRepository productRepository;
    private final RuleEngineService ruleEngineService;
    private final RankerService rankerService;
    private final com.picnic.psi.repository.PurchaseHistoryRepository purchaseHistoryRepository;

    public PageService(PageRepository pageRepository,
                       SectionRepository sectionRepository,
                       ProductRepository productRepository,
                       RuleEngineService ruleEngineService,
                       RankerService rankerService,
                       com.picnic.psi.repository.PurchaseHistoryRepository purchaseHistoryRepository) {
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
        this.productRepository = productRepository;
        this.ruleEngineService = ruleEngineService;
        this.rankerService = rankerService;
        this.purchaseHistoryRepository = purchaseHistoryRepository;
    }

    /**
     * Builds a complete personalized page for the given customer.
     *
     * @param pageId     the page to build (e.g., "home")
     * @param customerId the customer to personalize for (nullable for anonymous)
     * @return a fully assembled PageResponse with scored and visibility-checked sections
     */
    public PageResponse buildPage(String pageId, String customerId) {

        // Step 1 — Fetch the page entity from the database
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Page not found: " + pageId));

        // Step 2 — Fetch all sections for this page, ordered by displayOrder
        List<Section> sections = sectionRepository.findByPageIdOrderByDisplayOrderAsc(pageId);

        // Step 3 — For each section, evaluate visibility, calculate score, and attach products
        List<SectionResponse> sectionResponses = sections.stream().map(section -> {

            // Section is visible if rule engine says SHOW
            // OR if behaviour-based unlocking says it's earned
            boolean visible = ruleEngineService.isSectionVisible(section.getId(), section)
                    || ruleEngineService.isUnlockedByBehaviour(
                            section.getId(), customerId, purchaseHistoryRepository);

            // Calculate unlock progress for locked sections
            int unlockProgress = 0;
            if (section.getId().equals("recipes")) {
                int total = purchaseHistoryRepository.findByCustomerId(customerId)
                        .stream().mapToInt(h -> h.getPurchaseCount()).sum();
                unlockProgress = Math.min((total * 100) / 3, 100);
            } else if (section.getId().equals("weekend-promo")) {
                int total = purchaseHistoryRepository.findByCustomerId(customerId)
                        .stream().mapToInt(h -> h.getPurchaseCount()).sum();
                unlockProgress = Math.min((total * 100) / 5, 100);
            }

            // Calculate personalization score using the section's ID as the category
            // (our purchase history categories match section IDs: "dairy", "vegetables", "recipes")
            double score = rankerService.calculateScore(customerId, section.getId());

            // Fetch all products belonging to this section
            List<Product> products = productRepository.findBySectionId(section.getId());

            // Map Product entities to ProductResponse DTOs
            List<ProductResponse> productResponses = products.stream()
                    .map(ProductResponse::from)
                    .toList();

            // Build the SectionResponse with all computed fields
            return SectionResponse.builder()
                    .sectionId(section.getId())
                    .type(section.getType())
                    .title(section.getTitle())
                    .order(section.getDisplayOrder())
                    .visible(visible)
                    .score(score)
                    .businessSignal(rankerService.getBusinessSignal(score))
                    .unlockProgress(unlockProgress)
                    .products(productResponses)
                    .build();

        }).toList();

        // Step 4 — Sort sections by score descending
        // Higher score = more relevant to this customer = appears first
        List<SectionResponse> sortedSections = new java.util.ArrayList<>(sectionResponses);
        sortedSections.sort(Comparator.comparingDouble(SectionResponse::getScore).reversed());

        // Step 5 — Build and return the final page response
        return PageResponse.builder()
                .pageId(page.getId())
                .title(page.getTitle())
                .description(page.getDescription())
                .customerId(customerId)
                .sections(sortedSections)
                .build();
    }
}
