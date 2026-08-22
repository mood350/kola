package com.kola.backend.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public ConfigurableServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        // Ajoute un customizer pour intercepter le connecteur Tomcat
        factory.addConnectorCustomizers((Connector connector) -> {
            // Force la suppression complète du header "Server"
            connector.setProperty("server", "null");
        });
        return factory;
    }
}