package com.chubb.claims.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
class ApplicationConfig {

    /** One clock for the whole application, so time can be controlled in tests. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
