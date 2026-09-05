package com.dogaa.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the business-rule property beans (fees, assistant) from DOGAA.md §5. */
@Configuration
@EnableConfigurationProperties({FeeProperties.class, AssistantProperties.class})
public class BusinessConfig {
}
