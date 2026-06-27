# picnic-store-intelligence

A Spring Boot 3 backend exploring two unsolved problems in Picnic's Page Platform —
built after reading Picnic Engineering's full blog series.

> "Instead of asking how do we build this feature, we now ask how do we extend the
> platform so that this and the next ten features can be built more easily."
> — Picnic Engineering Blog

## What This Explores

After reading Picnic's engineering blog series on their Page Platform, two problems
stood out worth exploring:

**Problem 1 — Server-driven layout control**
The backend decides what sections appear and in what state. The frontend is a dumb
renderer with zero hardcoded UI logic. Inspired by Picnic's Page Platform blog:
[Faster Features, Happier Customers](https://blog.picnic.nl/faster-features-happier-customers-introducing-the-platform-that-transformed-our-grocery-app-b38e57a85531)

**Problem 2 — Per-customer section ranking**
The rule engine controls visibility but not ordering. This project adds a weighted
scoring algorithm that ranks sections by relevance to each customer automatically.
Inspired by: [Building a Store Platform to Scale Beyond a Million Customers](https://blog.picnic.nl/picnic-10-years-2022-building-a-store-platform-to-scale-beyond-a-million-customers-89bb8ee1bb55)

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

## Real-time Behaviour

Purchase history updates in real time via the simulation API. A new customer starts
with zero history — all sections score 0. As they simulate purchases, scores update
and sections rerank automatically. The store personalizes itself around behaviour,
not manual configuration.

## Tech Stack

- Java 21 + Spring Boot 3.2
- PostgreSQL (Supabase)
- Spring Data JPA
- SpringDoc OpenAPI (Swagger UI)
- Maven
- Vanilla HTML/JS frontend

## Design Decisions & Known Tradeoffs

**String-based foreign keys instead of JPA relationships**
Entities use plain `String` fields (e.g. `pageId`, `sectionId`) instead of
`@ManyToOne` JPA relationships. This was a deliberate simplification to keep
the project focused on the platform architecture rather than ORM complexity.
In a production system, proper SQL foreign key constraints and JPA relationships
would enforce data integrity and prevent orphaned records.

**Weighted scoring vs ML-based ranking**
The ranker uses a hand-tuned weighted formula rather than a trained ML model.
This makes the algorithm fully transparent and explainable — every score can be
traced back to exactly three inputs. A production system would likely evolve
this toward collaborative filtering or a two-tower neural network, similar to
Picnic's recipe recommendation evolution described in their engineering blog.

**Info bar and score explainer in the UI**
In a real production store, personalization is silent — customers never see
scores or ranking explanations. The explainer UI exists purely for demonstration
purposes to make the algorithm's behaviour visible during technical discussions.

## API Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/pages/home?customerId=` | Personalized home page layout |
| GET | `/api/pages/product-detail` | Product detail page layout |
| GET | `/api/pages/{pageId}/scores?customerId=` | Raw section scores |
| POST | `/api/customers/{customerId}/purchase?category=` | Simulate a purchase |
| GET | `/api/customers/{customerId}/history` | Get purchase history |
| POST | `/api/customers/{customerId}/register` | Register new customer |

## Running Locally

```bash
git clone https://github.com/Vinu2111/picnic-store-intelligence.git
cp src/main/resources/application-local.properties src/main/resources/application.properties
# Fill in your Supabase credentials in application.properties
mvn spring-boot:run
open http://localhost:8080/swagger-ui.html
```

## Live Demo

- Backend: [Render URL — coming soon]
- Frontend: [Vercel URL — coming soon]

## Blog Series That Inspired This

- [Faster Features, Happier Customers](https://blog.picnic.nl/faster-features-happier-customers-introducing-the-platform-that-transformed-our-grocery-app-b38e57a85531)
- [Building a Store Platform to Scale Beyond a Million Customers](https://blog.picnic.nl/picnic-10-years-2022-building-a-store-platform-to-scale-beyond-a-million-customers-89bb8ee1bb55)
- [Server-Driven UI in Picnic's Page Platform](https://blog.picnic.nl/server-driven-ui-in-picnics-page-platform-c1603b55c7b2)
- [Java Meets JavaScript: Dynamic Page Rendering](https://blog.picnic.nl/java-meets-javascript-a-modern-approach-to-dynamic-page-rendering-31250dc66f33)
- [Beyond Static Pages: Bringing UI to Life](https://blog.picnic.nl/beyond-static-pages-bringing-ui-to-life-61f22c6b3e34)
