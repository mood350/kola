package com.kola.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the business-rule property beans (fees, assistant, QR) from KOLA.md §5. */
@Configuration
@EnableConfigurationProperties({FeeProperties.class, AssistantProperties.class, QrProperties.class})
public class BusinessConfig {
}
