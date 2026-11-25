package com.picnic.psi.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import com.picnic.psi.dto.ProductResponse;
import com.picnic.psi.exception.ResourceNotFoundException;
import com.picnic.psi.model.Page;
import com.picnic.psi.model.Product;
import com.picnic.psi.model.Rule;
import com.picnic.psi.model.Section;
import com.picnic.psi.repository.PageRepository;
import com.picnic.psi.repository.ProductRepository;
import com.picnic.psi.repository.RuleRepository;
import com.picnic.psi.repository.SectionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Serves static/semi-static store catalog metadata (pages, sections, rules, products)
 * in batched queries with caching so per-customer page assembly never repeats N+1 reads.
 */
@Service
public class StoreCatalogService {

    private final PageRepository pageRepository;
    private final SectionRepository sectionRepository;
    private final ProductRepository productRepository;
    private final RuleRepository ruleRepository;

    public StoreCatalogService(PageRepository pageRepository,
                               SectionRepository sectionRepository,
                               ProductRepository productRepository,
                               RuleRepository ruleRepository) {
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
        this.productRepository = productRepository;
        this.ruleRepository = ruleRepository;
    }

    @Cacheable(value = "pages", key = "#pageId")
    public Page getPage(String pageId) {
        return pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + pageId));
    }

    @Cacheable(value = "pageSections", key = "#pageId")
    public List<Section> getSectionsForPage(String pageId) {
        return sectionRepository.findByPageIdOrderByDisplayOrderAsc(pageId);
    }

    @Cacheable(value = "pageRules", key = "#pageId")
    public Map<String, List<Rule>> getRulesForPage(String pageId, List<String> sectionIds) {
        if (sectionIds.isEmpty()) {
            return Map.of();
        }
        return ruleRepository.findBySectionIdIn(sectionIds).stream()
                .collect(groupingBy(Rule::getSectionId));
    }

    @Cacheable(value = "pageProducts", key = "#pageId")
    public Map<String, List<ProductResponse>> getProductsForPage(String pageId, List<String> sectionIds) {
        if (sectionIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findBySectionIdIn(sectionIds).stream()
                .collect(groupingBy(Product::getSectionId, mapping(ProductResponse::from, toList())));
    }

    @Cacheable("productsGrouped")
    public Map<String, List<ProductResponse>> getAllProductsGrouped() {
        return productRepository.findAll().stream()
                .collect(groupingBy(Product::getCategory, mapping(ProductResponse::from, toList())));
    }
}
