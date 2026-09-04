package com.dogaa.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the business-rule property beans (fees, and later credit/scoring) from DOGAA.md §5. */
@Configuration
@EnableConfigurationProperties(FeeProperties.class)
public class BusinessConfig {
}
