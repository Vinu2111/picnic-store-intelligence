# picnic-store-intelligence

An architectural prototype exploring server-driven UI and per-customer section ranking, inspired by Picnic Engineering's Page Platform blog series.

---

## The Problem This Explores

After reading Picnic's full Page Platform blog series, two problems stood out as worth exploring independently.

The rule engine described in the blogs controls section **visibility**  whether a section appears or not, based on time-of-day, day-of-week, or static rules. But it does not control section **order**. Every customer who passes the same visibility rules sees the same layout, in the same sequence, regardless of their purchase behaviour.

This means a customer who buys dairy every week sees the dairy section in the same position as a customer who has never bought dairy. The layout is correct, but it is not personalised. This project explores what happens when you add a per-customer ranking layer on top of the existing rule engine without replacing it.

---

## What This Builds

### Pillar 1 - Server-Driven Layout

`PageService` assembles the full page layout on the backend. `RuleEngineService` evaluates visibility rules per section. The frontend receives a complete, ordered list of sections and renders them without any layout logic of its own. Adding a new section or changing display order requires zero frontend changes the backend is the single source of truth.

### Pillar 2 - Smart Section Ranker

`RankerService` scores each section per customer using a weighted formula based on purchase frequency, recency, and time-of-day relevance. Sections are sorted by score descending before the response is sent. The customer never sees a score or knows that ranking occurred the store simply feels more relevant.

---

## The Scoring Algorithm

```
score = (frequencyScore × 0.5) + (recencyScore × 0.3) + (timeOfDayScore × 0.2)
```

| Component | Logic | Weight |
|---|---|---|
| Purchase frequency | `min(purchaseCount / 10.0, 1.0)` | 50% |
| Recency | `max(0.0, 1.0 - daysSinceLastPurchase / 30.0)` | 30% |
| Time of day | Category relevance at current hour | 20% |

Each numerical score maps to a business signal for non-technical stakeholders:

| Score | Business Signal |
|---|---|
| ≥ 0.80 | High purchase intent |
| ≥ 0.60 | Strong conversion candidate |
| ≥ 0.40 | Neutral visibility |
| > 0.00 | Low engagement signal |
| 0.00 | No purchase history |

---

## UI Component Registry

The frontend uses a component registry pattern to render sections. A `renderers` object maps each `section.type` (e.g. `BANNER`, `PRODUCT_GRID`, `RECIPE`, `PROMO`) to a dedicated render function. When the backend introduces a new section type, the frontend only needs a single new entry in the registry no structural changes, no conditionals, no refactoring.

Unknown section types fall back to a default renderer that displays gracefully instead of breaking. This is the practical implementation of "zero frontend deployment" the contract between backend and frontend is the section type string, nothing more.

---

## Real-time Feedback Loop

The system supports a complete feedback loop from cold start to personalised layout:

1. A new customer starts with zero purchase history all sections score 0.0 and display in default order.
2. The customer simulates a purchase via the `/purchase` endpoint, which increments the category count and updates the recency timestamp in the database.
3. On the next page load, the ranker recalculates scores using the updated history. Sections reorder automatically.
4. Purchase history persists across server restarts (`ddl-auto=update`). A customer created during a demo session will retain their history indefinitely.

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/pages/home?customerId=` | Personalised home page layout |
| GET | `/api/pages/product-detail` | Product detail page layout |
| GET | `/api/pages/{pageId}/scores?customerId=` | Raw section scores |
| POST | `/api/customers/{customerId}/purchase?category=` | Simulate a purchase |
| GET | `/api/customers/{customerId}/history` | Get purchase history |
| POST | `/api/customers/{customerId}/register` | Register new customer |

---

## Tech Stack

- Java 21 + Spring Boot 3.2
- PostgreSQL (Supabase)
- Spring Data JPA
- SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- Maven
- Vanilla HTML/JS frontend with UI Component Registry

---

## Project Structure

```
src/main/java/com/picnic/psi/
├── controller/    REST layer (PageController, PurchaseController)
├── service/       Business logic (PageService, RuleEngineService, RankerService)
├── model/         JPA entities (Page, Section, Product, Rule, PurchaseHistory)
├── repository/    Spring Data JPA repositories
├── dto/           API response objects (PageResponse, SectionResponse, ProductResponse)
├── config/        CORS configuration, DataSeeder
└── exception/     GlobalExceptionHandler

frontend/
└── index.html     Single-file frontend with UI Component Registry
```

---

## Running Locally

```bash
git clone https://github.com/Vinu2111/picnic-store-intelligence.git
cd picnic-store-intelligence
cp src/main/resources/application-local.properties src/main/resources/application.properties
# Fill in your Supabase credentials in application.properties
mvn spring-boot:run
# Open Swagger UI
open http://localhost:8080/swagger-ui.html
# Open frontend
open frontend/index.html
```

---

## Demo Flow

1. Open `frontend/index.html` in a browser while Spring Boot is running.
2. Type any name in the customer input (e.g. `rickie`) and click **Load Store**. All sections score 0.0 default layout, no personalisation.
3. Click **Simulate Purchase** on the Dairy section. Click it again.
4. Click **Load Store** again. Dairy has jumped up in the ranking — its score now reflects two recent purchases.
5. Click **Compare All Customers** to see three preset customer profiles side by side, each with a different section ordering based on their purchase history.
6. Restart the Spring Boot server. Type `rickie` again and click **Load Store**. The purchase history is still there scores and ranking are preserved.

---

## Design Decisions and Known Tradeoffs

**String-based foreign keys instead of JPA relationships.**
Entities use plain `String` fields (`pageId`, `sectionId`) instead of `@ManyToOne` JPA relationships. This was a deliberate simplification to keep the project focused on platform architecture rather than ORM modelling. In a production system, proper foreign key constraints and JPA relationships would enforce referential integrity and prevent orphaned records.

**Weighted scoring formula instead of ML-based ranking.**
The ranker uses a hand-tuned weighted formula rather than a trained model. This makes the algorithm fully transparent every score traces back to exactly three inputs with known weights. A production system would likely evolve toward collaborative filtering or a learned ranking model, but for an architectural prototype, explainability matters more than accuracy.

**Score explainer in the frontend.**
In a production store, personalisation is silent customers never see scores or ranking logic. The explainer panel in this prototype exists purely for demonstration purposes, to make the algorithm's behaviour visible during technical review. This is a conscious design choice, not an oversight.

---

## Inspired By

- [Faster Features, Happier Customers: Introducing the Platform That Transformed Our Grocery App](https://blog.picnic.nl/faster-features-happier-customers-introducing-the-platform-that-transformed-our-grocery-app-b38e57a85531)
- [Building a Store Platform to Scale Beyond a Million Customers](https://blog.picnic.nl/picnic-10-years-2022-building-a-store-platform-to-scale-beyond-a-million-customers-89bb8ee1bb55)
- [Server-Driven UI in Picnic's Page Platform](https://blog.picnic.nl/server-driven-ui-in-picnics-page-platform-c1603b55c7b2)
- [Java Meets JavaScript: A Modern Approach to Dynamic Page Rendering](https://blog.picnic.nl/java-meets-javascript-a-modern-approach-to-dynamic-page-rendering-31250dc66f33)
- [Beyond Static Pages: Bringing UI to Life](https://blog.picnic.nl/beyond-static-pages-bringing-ui-to-life-61f22c6b3e34)

---

*This project was built to deeply understand Picnic's platform philosophy not to replicate it, but to think alongside it.*
