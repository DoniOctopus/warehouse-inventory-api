package com.ifg.warehouse.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Injectable clock so time-dependent code can be tested deterministically. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
