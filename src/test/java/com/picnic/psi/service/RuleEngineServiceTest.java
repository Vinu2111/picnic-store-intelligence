package com.picnic.psi.service;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.picnic.psi.model.Rule;
import com.picnic.psi.model.Section;
import org.junit.jupiter.api.Test;

final class RuleEngineServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), UTC);

    @Test
    void alwaysVisibleSectionBypassesRules() {
        RuleEngineService service = new RuleEngineService(mock(), FIXED_CLOCK);
        Section banner = Section.builder().id("banner").alwaysVisible(true).build();

        assertThat(service.isSectionVisible(banner, List.of())).isTrue();
    }

    @Test
    void sectionWithoutRulesFailsClosed() {
        RuleEngineService service = new RuleEngineService(mock(), FIXED_CLOCK);
        Section section = Section.builder().id("recipes").alwaysVisible(false).build();

        assertThat(service.isSectionVisible(section, List.of())).isFalse();
    }

    @Test
    void dayOfWeekRuleMatchesWeekendUsingInjectedClock() {
        // 2026-11-28 is a Saturday
        Clock saturdayClock = Clock.fixed(Instant.parse("2026-11-28T10:00:00Z"), UTC);
        RuleEngineService saturdayService = new RuleEngineService(mock(), saturdayClock);

        // 2026-11-25 is a Wednesday
        Clock wednesdayClock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), UTC);
        RuleEngineService wednesdayService = new RuleEngineService(mock(), wednesdayClock);

        Section promo = Section.builder().id("weekend-promo").alwaysVisible(false).build();
        List<Rule> rules = List.of(
                Rule.builder().sectionId("weekend-promo").conditionType("DAY_OF_WEEK").conditionValue("SATURDAY").action("SHOW").build());

        assertThat(saturdayService.isSectionVisible(promo, rules)).isTrue();
        assertThat(wednesdayService.isSectionVisible(promo, rules)).isFalse();
    }

    @Test
    void behaviourUnlockAndProgressUseConfiguredThresholds() {
        RuleEngineService service = new RuleEngineService(mock(), FIXED_CLOCK);

        assertThat(service.isUnlockedByBehaviour("recipes", 2)).isFalse();
        assertThat(service.isUnlockedByBehaviour("recipes", 3)).isTrue();
        assertThat(service.calculateUnlockProgress("recipes", 2)).isEqualTo(66);
        assertThat(service.calculateUnlockProgress("recipes", 4)).isEqualTo(100);

        assertThat(service.isUnlockedByBehaviour("weekend-promo", 4)).isFalse();
        assertThat(service.isUnlockedByBehaviour("weekend-promo", 5)).isTrue();
        assertThat(service.calculateUnlockProgress("weekend-promo", 3)).isEqualTo(60);
    }
}
