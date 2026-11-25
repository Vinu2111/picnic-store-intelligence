package com.picnic.psi.service;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.picnic.psi.dto.PageResponse;
import com.picnic.psi.dto.SectionResponse;
import com.picnic.psi.model.Page;
import com.picnic.psi.model.Product;
import com.picnic.psi.model.PurchaseHistory;
import com.picnic.psi.model.Rule;
import com.picnic.psi.model.Section;
import com.picnic.psi.repository.PageRepository;
import com.picnic.psi.repository.ProductRepository;
import com.picnic.psi.repository.PurchaseHistoryRepository;
import com.picnic.psi.repository.RuleRepository;
import com.picnic.psi.repository.SectionRepository;
import org.junit.jupiter.api.Test;

final class PageServiceTest {

    private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");

    @Test
    void buildPageFetchesCustomerHistoryOnceAndSortsByScoreDescending() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-25T11:00:00Z"), UTC);
        PageRepository pageRepo = mock();
        SectionRepository sectionRepo = mock();
        ProductRepository productRepo = mock();
        RuleRepository ruleRepo = mock();
        PurchaseHistoryRepository historyRepo = mock();

        StoreCatalogService catalogService = new StoreCatalogService(pageRepo, sectionRepo, productRepo, ruleRepo);
        RuleEngineService ruleEngine = new RuleEngineService(ruleRepo, fixedClock);
        RankerService ranker = new RankerService(historyRepo, fixedClock);
        PageService pageService = new PageService(catalogService, ruleEngine, ranker, historyRepo);

        Page home = Page.builder().id("home").title("Store").description("Home").build();
        List<Section> sections = List.of(
                Section.builder().id("banner").pageId("home").type("BANNER").title("Banner").displayOrder(1).alwaysVisible(true).build(),
                Section.builder().id("dairy").pageId("home").type("PRODUCT_GRID").title("Dairy").displayOrder(2).alwaysVisible(false).build(),
                Section.builder().id("vegetables").pageId("home").type("PRODUCT_GRID").title("Veggies").displayOrder(3).alwaysVisible(false).build(),
                Section.builder().id("recipes").pageId("home").type("RECIPE").title("Recipes").displayOrder(4).alwaysVisible(false).build()
        );
        List<String> sectionIds = List.of("banner", "dairy", "vegetables", "recipes");

        when(pageRepo.findById("home")).thenReturn(Optional.of(home));
        when(sectionRepo.findByPageIdOrderByDisplayOrderAsc("home")).thenReturn(sections);
        when(ruleRepo.findBySectionIdIn(sectionIds)).thenReturn(List.of(
                Rule.builder().sectionId("dairy").conditionType("ALWAYS").conditionValue("ANY").action("SHOW").build(),
                Rule.builder().sectionId("vegetables").conditionType("ALWAYS").conditionValue("ANY").action("SHOW").build()
        ));
        when(productRepo.findBySectionIdIn(sectionIds)).thenReturn(List.of(
                Product.builder().id(1L).name("Milk").price(1.09).category("dairy").sectionId("dairy").build()
        ));

        LocalDateTime now = LocalDateTime.ofInstant(fixedClock.instant(), AMSTERDAM);
        when(historyRepo.findByCustomerId("vinayak")).thenReturn(List.of(
                PurchaseHistory.builder().customerId("vinayak").category("vegetables").purchaseCount(9).lastPurchasedAt(now).build(),
                PurchaseHistory.builder().customerId("vinayak").category("dairy").purchaseCount(1).lastPurchasedAt(now.minusDays(10)).build()
        ));

        PageResponse response = pageService.buildPage("home", "vinayak");

        // Customer history, rules, and products are each queried at most ONCE for the whole page (no N+1)
        verify(historyRepo, times(1)).findByCustomerId("vinayak");
        verify(ruleRepo, times(1)).findBySectionIdIn(sectionIds);
        verify(productRepo, times(1)).findBySectionIdIn(sectionIds);

        assertThat(response.getSections())
                .extracting(SectionResponse::getSectionId)
                .containsExactly("banner", "vegetables", "dairy", "recipes");
        // 10 total purchases unlocks recipes (>=3)
        assertThat(response.getSections().get(3).isVisible()).isTrue();
        assertThat(response.getSections().get(1).getScoreBreakdown()).isNotNull();
    }
}
