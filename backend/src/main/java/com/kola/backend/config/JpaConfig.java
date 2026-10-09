package com.kola.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables the {@code @CreatedDate} / {@code @LastModifiedDate} handling of BaseEntity. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
