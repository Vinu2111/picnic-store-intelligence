package com.picnic.psi.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides an injectable UTC {@link Clock} bean so time-dependent ranking,
 * rule evaluation, and error timestamps remain deterministic in tests.
 */
@Configuration
public class TimeConfig {

    @Bean
    @SuppressWarnings("TimeZoneUsage")
    public Clock clock() {
        return Clock.systemUTC();
    }
}
