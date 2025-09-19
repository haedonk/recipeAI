package com.haekitchenapp.recipeapp.support

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Configuration
class ClockFixedConfig {

    @Bean
    Clock fixedClock() {
        Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC)
    }
}
