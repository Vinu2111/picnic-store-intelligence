# picnic-store-intelligence

An architectural prototype exploring server-driven UI and per-customer section ranking — built after reading Picnic Engineering's full Page Platform blog series.

> "Instead of asking how do we build this feature, we now ask how do we extend the platform so that this and the next ten features can be built more easily."
> — Picnic Engineering Blog

## Live Demo

| | URL |
|---|---|
| Frontend | https://picnic-store-intelligence.vercel.app |
| Backend API | https://picnic-store-intelligence-production.up.railway.app |
| Swagger UI | https://picnic-store-intelligence-production.up.railway.app/swagger-ui.html |

**Try it with a pre-loaded customer:**
```
https://picnic-store-intelligence.vercel.app?customer=your-name
```

## The Problem This Explores

After reading Picnic's full Page Platform blog series, two problems stood out worth exploring independently:

**Problem 1 — Server-driven layout control**
The backend decides what sections appear, in what order, and in what state. The frontend is a dumb renderer with a component registry — zero hardcoded UI logic. Adding a new section type requires one new entry in the registry, nothing else.

**Problem 2 — Per-customer section ranking**
The rule engine controls visibility but not ordering. This project adds a weighted scoring algorithm that silently ranks sections by relevance to each customer based on their purchase behaviour. The store personalizes itself — no human configuration required.

## The Two-Panel Experience

```
┌─────────────────────────┬──────────────────────────┐
│   🛒 THE STORE          │   👤 THE CUSTOMER        │
│                         │                          │
│  Vinayak                │  Vinayak                 │
│  Active Shopper         │  Dairy Lover • 5 buys    │
│  Dairy Lover            │                          │
│  ─────────────────────  │  SHOP NOW                │
│                         │  [Milk €1.09] [Buy]      │
│  #1 Dairy & Eggs ✅     │  [Eggs €3.49] [Buy]      │
│  Score: 0.89            │  [Yogurt €2.29] [Buy]    │
│  High purchase intent   │                          │
│                         │  RECENT ACTIVITY         │
│  #2 Fresh Vegetables ✅  │  🛒 Bought Whole Milk    │
│  Score: 0.61            │  🛒 Bought Greek Yogurt  │
│                         │  🛒 Bought Broccoli      │
│  #3 Meal Ideas 🔒       │                          │
│  67% to unlock          │                          │
└─────────────────────────┴──────────────────────────┘
```

## The Scoring Algorithm

```
score = (frequencyScore × 0.5) + (recencyScore × 0.3) + (timeOfDayScore × 0.2)
```

| Component | Logic | Weight |
|---|---|---|
| Purchase frequency | min(purchaseCount / 10.0, 1.0) | 50% |
| Recency | max(0.0, 1.0 - daysSince / 30.0) | 30% |
| Time of day | category relevance at current hour | 20% |

Each score maps to a business signal:

| Score | Signal |
|---|---|
| ≥ 0.80 | High purchase intent |
| ≥ 0.60 | Strong conversion candidate |
| ≥ 0.40 | Neutral visibility |
| > 0.00 | Low engagement signal |
| 0.00 | No purchase history |

## Smart Section Unlocking

The rule engine and ranker work together. Sections unlock automatically based on customer behaviour — no manual configuration:

| Trigger | Effect |
|---|---|
| 0 purchases | Default layout — banner + core sections visible |
| 3+ purchases | Recipes section unlocks automatically |
| 5+ purchases | Weekend promo unlocks automatically |

Progress bars show how close a customer is to unlocking each section.

## UI Component Registry

The frontend uses a component registry pattern — the backend decides WHAT to show, the registry decides HOW to render it:

```javascript
const renderers = {
  BANNER: (section) => `...`,
  PRODUCT_GRID: (section) => `...`,
  RECIPE: (section) => `...`,
  PROMO: (section) => `...`
};

// Unknown section types render gracefully without code changes
const defaultRenderer = (section) => `...`;
```

## Real-time Feedback Loop

New customer → zero history → default layout → buy products → scores update → store reranks automatically → purchase history persists across sessions (Supabase + localStorage + URL params).

Share a personalized store URL:
```
https://picnic-store-intelligence.vercel.app?customer=vinayak
```

## API Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/pages/home?customerId=` | Personalized home page layout |
| GET | `/api/pages/{pageId}/scores?customerId=` | Raw section scores |
| GET | `/api/products` | All products grouped by category |
| POST | `/api/customers/{id}/purchase?category=` | Simulate a purchase |
| GET | `/api/customers/{id}/summary` | Customer status and preference label |
| GET | `/api/customers/{id}/history` | Full purchase history |
| DELETE | `/api/customers/{id}/history` | Reset purchase history |

## Tech Stack

- Java 21 + Spring Boot 3.2
- PostgreSQL (Supabase)
- Spring Data JPA
- SpringDoc OpenAPI (Swagger UI)
- Maven
- Vanilla HTML/JS frontend with UI Component Registry
- Deployed: Railway (backend) + Vercel (frontend)

## Project Structure

```
src/main/java/com/picnic/psi/
├── controller/    PageController, PurchaseController, ProductController
├── service/       PageService, RuleEngineService, RankerService
├── model/         Page, Section, Product, Rule, PurchaseHistory
├── repository/    Spring Data JPA repositories
├── dto/           PageResponse, SectionResponse, ProductResponse
├── config/        CorsConfig, DataSeeder
└── exception/     GlobalExceptionHandler

frontend/
└── index.html    Two-panel UI with customer panel, activity feed, smart unlocking
```

## Running Locally

```bash
git clone https://github.com/Vinu2111/picnic-store-intelligence.git
cd picnic-store-intelligence
cp src/main/resources/application-local.properties src/main/resources/application.properties
# Fill in your Supabase credentials in application.properties
mvn spring-boot:run
open frontend/index.html
```

## Demo Flow

1. Open the live URL — type any name → Load Store
2. See default layout — "New Customer" status, neutral scores
3. Click Buy on products in the right panel
4. Watch the left panel update in real time — sections rerank, scores rise
5. After 3 purchases — Recipes section unlocks automatically
6. Click "Compare All" — see 3 different customers, 3 different stores
7. Click "Reset purchases" — store returns to default instantly
8. Share your personalized URL — anyone can see your exact store

## Design Decisions & Known Tradeoffs

**String-based foreign keys instead of JPA relationships**
Deliberate simplification to keep focus on platform architecture. Production would use proper SQL constraints and JPA relationships.

**Weighted scoring formula instead of ML-based ranking**
Hand-tuned weights make the algorithm fully transparent — every score traces back to exactly three inputs. Production would evolve toward collaborative filtering or a two-tower neural network, similar to Picnic's recipe recommendation evolution.

**Score explainer and activity feed in the UI**
In production, personalization is silent. These exist purely to make the algorithm's behaviour visible during technical review.

## Inspired By

- [Faster Features, Happier Customers](https://blog.picnic.nl/faster-features-happier-customers-introducing-the-platform-that-transformed-our-grocery-app-b38e57a85531)
- [Building a Store Platform to Scale Beyond a Million Customers](https://blog.picnic.nl/picnic-10-years-2022-building-a-store-platform-to-scale-beyond-a-million-customers-89bb8ee1bb55)
- [Server-Driven UI in Picnic's Page Platform](https://blog.picnic.nl/server-driven-ui-in-picnics-page-platform-c1603b55c7b2)
- [Java Meets JavaScript: Dynamic Page Rendering](https://blog.picnic.nl/java-meets-javascript-a-modern-approach-to-dynamic-page-rendering-31250dc66f33)
- [Beyond Static Pages: Bringing UI to Life](https://blog.picnic.nl/beyond-static-pages-bringing-ui-to-life-77959271a1c3)

---

*This project was built to deeply understand Picnic's platform philosophy — not to replicate it, but to think alongside it.*
