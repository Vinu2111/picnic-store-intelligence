package com.picnic.psi.config;

import java.time.LocalDateTime;
import java.util.List;

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
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with sample data on application startup.
 * Only runs if the database is empty (no pages exist yet).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final PageRepository pageRepository;
    private final SectionRepository sectionRepository;
    private final ProductRepository productRepository;
    private final RuleRepository ruleRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;

    public DataSeeder(PageRepository pageRepository,
                      SectionRepository sectionRepository,
                      ProductRepository productRepository,
                      RuleRepository ruleRepository,
                      PurchaseHistoryRepository purchaseHistoryRepository) {
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
        this.productRepository = productRepository;
        this.ruleRepository = ruleRepository;
        this.purchaseHistoryRepository = purchaseHistoryRepository;
    }

    @Override
    public void run(String... args) {

        // With ddl-auto=update, tables persist across restarts.
        // We only seed if no data exists — new customer purchase history
        // will survive server restarts, making the demo more realistic.
        if (pageRepository.count() > 0) {
            System.out.println("⏭️  Seed data already exists, skipping... (customer purchase history preserved)");
            return;
        }

        // =============================================
        // 1. Pages — top-level containers for the store
        // =============================================
        pageRepository.saveAll(List.of(
                Page.builder()
                        .id("home")
                        .title("Your Grocery Store")
                        .description("Personalized home page")
                        .build(),
                Page.builder()
                        .id("product-detail")
                        .title("Product Detail")
                        .description("Product detail page")
                        .build()
        ));

        // =============================================
        // 2. Sections — building blocks of the home page
        //    Each section has a type, display order, and
        //    visibility flag for the rule engine
        // =============================================
        sectionRepository.saveAll(List.of(
                Section.builder()
                        .id("banner")
                        .pageId("home")
                        .type("BANNER")
                        .title("Fresh Deals Today \uD83D\uDED2")
                        .displayOrder(1)
                        .alwaysVisible(true)
                        .build(),
                Section.builder()
                        .id("dairy")
                        .pageId("home")
                        .type("PRODUCT_GRID")
                        .title("Dairy & Eggs")
                        .displayOrder(2)
                        .alwaysVisible(false)
                        .build(),
                Section.builder()
                        .id("vegetables")
                        .pageId("home")
                        .type("PRODUCT_GRID")
                        .title("Fresh Vegetables")
                        .displayOrder(3)
                        .alwaysVisible(false)
                        .build(),
                Section.builder()
                        .id("recipes")
                        .pageId("home")
                        .type("RECIPE")
                        .title("Meal Ideas This Week")
                        .displayOrder(4)
                        .alwaysVisible(false)
                        .build(),
                Section.builder()
                        .id("weekend-promo")
                        .pageId("home")
                        .type("PROMO")
                        .title("Weekend Special \uD83C\uDF89")
                        .displayOrder(5)
                        .alwaysVisible(false)
                        .build()
        ));

        // =============================================
        // 3. Products — items displayed inside sections
        //    Linked to sections via sectionId
        // =============================================
        productRepository.saveAll(List.of(
                Product.builder().name("Whole Milk 1L").price(1.09).category("dairy").sectionId("dairy").build(),
                Product.builder().name("Free Range Eggs 12pk").price(3.49).category("dairy").sectionId("dairy").build(),
                Product.builder().name("Greek Yogurt 500g").price(2.29).category("dairy").sectionId("dairy").build(),
                Product.builder().name("Broccoli 500g").price(0.99).category("vegetables").sectionId("vegetables").build(),
                Product.builder().name("Baby Spinach 200g").price(1.79).category("vegetables").sectionId("vegetables").build(),
                Product.builder().name("Cherry Tomatoes 400g").price(1.49).category("vegetables").sectionId("vegetables").build(),
                Product.builder().name("Pasta Carbonara Kit").price(4.99).category("recipes").sectionId("recipes").build(),
                Product.builder().name("Stir Fry Meal Kit").price(5.49).category("recipes").sectionId("recipes").build()
        ));

        // =============================================
        // 4. Rules — control when sections are visible
        //    The rule engine evaluates these at request time
        // =============================================
        ruleRepository.saveAll(List.of(
                // Banner is always visible
                Rule.builder().sectionId("banner").conditionType("ALWAYS").conditionValue("ANY").action("SHOW").build(),
                // Weekend promo shows only on Saturday and Sunday
                Rule.builder().sectionId("weekend-promo").conditionType("DAY_OF_WEEK").conditionValue("SATURDAY").action("SHOW").build(),
                Rule.builder().sectionId("weekend-promo").conditionType("DAY_OF_WEEK").conditionValue("SUNDAY").action("SHOW").build(),
                
                // Make dairy, vegetables, and recipes always visible so the ranker effect is clearly visible in demos
                Rule.builder().sectionId("dairy").conditionType("ALWAYS").conditionValue("ANY").action("SHOW").build(),
                Rule.builder().sectionId("vegetables").conditionType("ALWAYS").conditionValue("ANY").action("SHOW").build(),
                Rule.builder().sectionId("recipes").conditionType("ALWAYS").conditionValue("ANY").action("SHOW").build()
        ));

        // =============================================
        // 5. Purchase History — simulates buying habits
        //    for 2 customers to drive personalization
        // =============================================
        LocalDateTime now = LocalDateTime.now();

        purchaseHistoryRepository.saveAll(List.of(
                // Customer-1: dairy lover — buys dairy frequently, rarely tries recipes
                PurchaseHistory.builder().customerId("customer-1").category("dairy").purchaseCount(12).lastPurchasedAt(now.minusDays(1)).build(),
                PurchaseHistory.builder().customerId("customer-1").category("vegetables").purchaseCount(3).lastPurchasedAt(now.minusDays(7)).build(),
                PurchaseHistory.builder().customerId("customer-1").category("recipes").purchaseCount(1).lastPurchasedAt(now.minusDays(30)).build(),

                // Customer-2: recipe lover — buys meal kits and veggies often, little dairy
                PurchaseHistory.builder().customerId("customer-2").category("recipes").purchaseCount(10).lastPurchasedAt(now.minusDays(2)).build(),
                PurchaseHistory.builder().customerId("customer-2").category("vegetables").purchaseCount(8).lastPurchasedAt(now.minusDays(3)).build(),
                PurchaseHistory.builder().customerId("customer-2").category("dairy").purchaseCount(2).lastPurchasedAt(now.minusDays(14)).build(),

                // Customer 3 — balanced shopper, buys everything equally
                PurchaseHistory.builder().customerId("customer-3").category("dairy").purchaseCount(5).lastPurchasedAt(now.minusDays(4)).build(),
                PurchaseHistory.builder().customerId("customer-3").category("vegetables").purchaseCount(5).lastPurchasedAt(now.minusDays(5)).build(),
                PurchaseHistory.builder().customerId("customer-3").category("recipes").purchaseCount(4).lastPurchasedAt(now.minusDays(6)).build()
        ));

        System.out.println("✅ Seed data loaded successfully");
    }
}
