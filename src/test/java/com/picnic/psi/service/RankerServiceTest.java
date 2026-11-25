package com.picnic.psi.service;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.picnic.psi.model.PurchaseHistory;
import com.picnic.psi.repository.PurchaseHistoryRepository;
import org.junit.jupiter.api.Test;

final class RankerServiceTest {

    private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), UTC);

    @Test
    void anonymousVisitorGetsZeroScoreAndExplanation() {
        PurchaseHistoryRepository repository = mock();
        RankerService rankerService = new RankerService(repository, FIXED_CLOCK);

        RankerService.ScoreResult result = rankerService.calculateScoreDetails(null, "dairy");

        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.breakdown().reason()).contains("Anonymous");
    }

    @Test
    void morningDairyPurchaseGetsTimeOfDayBoost() {
        PurchaseHistoryRepository repository = mock();
        // 08:30 Amsterdam time (UTC+1 in November) -> morning dairy boost (1.0)
        Clock morningClock = Clock.fixed(Instant.parse("2026-11-25T07:30:00Z"), UTC);
        RankerService rankerService = new RankerService(repository, morningClock);

        LocalDateTime now = LocalDateTime.ofInstant(morningClock.instant(), AMSTERDAM);
        PurchaseHistory history = PurchaseHistory.builder()
                .customerId("vinayak")
                .category("dairy")
                .purchaseCount(10)
                .lastPurchasedAt(now)
                .build();

        when(repository.findByCustomerId("vinayak")).thenReturn(List.of(history));

        RankerService.ScoreResult result = rankerService.calculateScoreDetails("vinayak", "dairy");

        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.breakdown().frequencyScore()).isEqualTo(1.0);
        assertThat(result.breakdown().recencyScore()).isEqualTo(1.0);
        assertThat(result.breakdown().timeOfDayScore()).isEqualTo(1.0);
        assertThat(result.breakdown().purchaseCount()).isEqualTo(10);
        assertThat(result.breakdown().daysSinceLastPurchase()).isEqualTo(0L);
    }

    @Test
    void eveningRecipesBoostAndRecencyDecayWorkCorrectly() {
        PurchaseHistoryRepository repository = mock();
        // 18:00 Amsterdam time -> evening recipes boost (1.0)
        Clock eveningClock = Clock.fixed(Instant.parse("2026-11-25T17:00:00Z"), UTC);
        RankerService rankerService = new RankerService(repository, eveningClock);

        LocalDateTime fifteenDaysAgo = LocalDateTime.ofInstant(eveningClock.instant(), AMSTERDAM).minusDays(15);
        PurchaseHistory history = PurchaseHistory.builder()
                .customerId("chef")
                .category("recipes")
                .purchaseCount(20) // capped at 1.0
                .lastPurchasedAt(fifteenDaysAgo)
                .build();

        RankerService.ScoreResult result = rankerService.calculateScoreDetails(
                "chef", "recipes", Optional.of(history));

        // freq=1.0*0.5 + recency=0.5*0.3 + tod=1.0*0.2 = 0.50 + 0.15 + 0.20 = 0.85
        assertThat(result.breakdown().frequencyScore()).isEqualTo(1.0);
        assertThat(result.breakdown().recencyScore()).isEqualTo(0.5);
        assertThat(result.breakdown().timeOfDayScore()).isEqualTo(1.0);
        assertThat(result.score()).isEqualTo(0.85);
    }

    @Test
    void stalePurchaseOverThirtyDaysClampsRecencyToZero() {
        PurchaseHistoryRepository repository = mock();
        Clock noonClock = Clock.fixed(Instant.parse("2026-11-25T11:00:00Z"), UTC);
        RankerService rankerService = new RankerService(repository, noonClock);

        PurchaseHistory oldHistory = PurchaseHistory.builder()
                .customerId("vinayak")
                .category("vegetables")
                .purchaseCount(4)
                .lastPurchasedAt(LocalDateTime.ofInstant(noonClock.instant(), AMSTERDAM).minusDays(45))
                .build();

        RankerService.ScoreResult result = rankerService.calculateScoreDetails(
                "vinayak", "vegetables", Optional.of(oldHistory));

        assertThat(result.breakdown().frequencyScore()).isEqualTo(0.4);
        assertThat(result.breakdown().recencyScore()).isEqualTo(0.0);
        assertThat(result.breakdown().timeOfDayScore()).isEqualTo(0.5);
        assertThat(result.score()).isEqualTo(0.30);
    }

    @Test
    void businessSignalsMapScoreBandsCorrectly() {
        RankerService rankerService = new RankerService(mock(), FIXED_CLOCK);

        assertThat(rankerService.getBusinessSignal(0.85)).isEqualTo("High purchase intent");
        assertThat(rankerService.getBusinessSignal(0.65)).isEqualTo("Strong conversion candidate");
        assertThat(rankerService.getBusinessSignal(0.45)).isEqualTo("Neutral visibility");
        assertThat(rankerService.getBusinessSignal(0.15)).isEqualTo("Low engagement signal");
        assertThat(rankerService.getBusinessSignal(0.0)).isEqualTo("No purchase history");
    }
}
