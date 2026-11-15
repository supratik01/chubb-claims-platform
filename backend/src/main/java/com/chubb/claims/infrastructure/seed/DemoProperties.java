package com.chubb.claims.infrastructure.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param userPassword the password given to every seeded demo user */
@ConfigurationProperties("claims.demo")
public record DemoProperties(String userPassword) {
}
