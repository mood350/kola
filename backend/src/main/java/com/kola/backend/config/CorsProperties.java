package com.kola.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.cors")
public class CorsProperties {

    /**
     * Front-end origins allowed to call the API. Exact origins, not patterns: the CORS spec
     * refuses a wildcard once credentials are allowed.
     *   3000     -> web front-end dev server
     *   10.0.2.2 -> the host as seen from the Android emulator
     */
    private List<String> allowedOrigins = List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://10.0.2.2:8081");
}
