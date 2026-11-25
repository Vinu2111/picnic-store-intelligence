package com.picnic.psi.service;

import static java.util.Comparator.comparingDouble;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.picnic.psi.dto.PageResponse;
import com.picnic.psi.dto.ProductResponse;
import com.picnic.psi.dto.SectionResponse;
import com.picnic.psi.model.Page;
import com.picnic.psi.model.PurchaseHistory;
import com.picnic.psi.model.Rule;
import com.picnic.psi.model.Section;
import com.picnic.psi.repository.PurchaseHistoryRepository;
import org.springframework.stereotype.Service;

/**
 * Core service that assembles a personalized page response.
 * Uses batched/cached catalog lookups and a single per-customer history query.
 */
@Service
public class PageService {

    private final StoreCatalogService catalogService;
    private final RuleEngineService ruleEngineService;
    private final RankerService rankerService;
    private final PurchaseHistoryRepository purchaseHistoryRepository;

    public PageService(StoreCatalogService catalogService,
                       RuleEngineService ruleEngineService,
                       RankerService rankerService,
                       PurchaseHistoryRepository purchaseHistoryRepository) {
        this.catalogService = catalogService;
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
        Page page = catalogService.getPage(pageId);
        List<Section> sections = catalogService.getSectionsForPage(pageId);

        List<String> sectionIds = sections.stream()
                .map(Section::getId)
                .toList();

        Map<String, List<Rule>> rulesBySection = catalogService.getRulesForPage(pageId, sectionIds);
        Map<String, List<ProductResponse>> productsBySection = catalogService.getProductsForPage(pageId, sectionIds);

        List<PurchaseHistory> customerHistory = (customerId == null || customerId.isBlank())
                ? List.of()
                : purchaseHistoryRepository.findByCustomerId(customerId);

        Map<String, PurchaseHistory> historyByCategory = customerHistory.stream()
                .collect(toMap(
                        PurchaseHistory::getCategory,
                        identity(),
                        (first, second) -> first));

        int totalPurchases = customerHistory.stream()
                .mapToInt(PurchaseHistory::getPurchaseCount)
                .sum();

        List<SectionResponse> sortedSections = sections.stream()
                .map(section -> buildSectionResponse(
                        section,
                        customerId,
                        totalPurchases,
                        rulesBySection.getOrDefault(section.getId(), List.of()),
                        productsBySection.getOrDefault(section.getId(), List.of()),
                        Optional.ofNullable(historyByCategory.get(section.getId()))))
                .sorted(comparingDouble(SectionResponse::getScore).reversed())
                .toList();

        return PageResponse.builder()
                .pageId(page.getId())
                .title(page.getTitle())
                .description(page.getDescription())
                .customerId(customerId)
                .sections(sortedSections)
                .build();
    }

    private SectionResponse buildSectionResponse(Section section,
                                                 String customerId,
                                                 int totalPurchases,
                                                 List<Rule> rules,
                                                 List<ProductResponse> products,
                                                 Optional<PurchaseHistory> categoryHistory) {
        boolean visible = ruleEngineService.isSectionVisible(section, rules)
                || ruleEngineService.isUnlockedByBehaviour(section.getId(), totalPurchases);

        int unlockProgress = ruleEngineService.calculateUnlockProgress(section.getId(), totalPurchases);
        RankerService.ScoreResult scoreResult = rankerService.calculateScoreDetails(
                customerId, section.getId(), categoryHistory);
        double score = scoreResult.score();

        return SectionResponse.builder()
                .sectionId(section.getId())
                .type(section.getType())
                .title(section.getTitle())
                .subtitle(section.getSubtitle())
                .order(section.getDisplayOrder())
                .visible(visible)
                .score(score)
                .scoreBreakdown(scoreResult.breakdown())
                .businessSignal(rankerService.getBusinessSignal(score))
                .unlockProgress(unlockProgress)
                .products(products)
                .build();
    }
}
