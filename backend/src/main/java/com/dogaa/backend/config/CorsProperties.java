package com.dogaa.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.cors")
public class CorsProperties {

    /** Front-end origins allowed to call the API. 3000 is the local React/Next dev server. */
    private List<String> allowedOrigins = List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000");
}
