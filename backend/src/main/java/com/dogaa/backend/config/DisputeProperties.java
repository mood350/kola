package com.dogaa.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.disputes")
public class DisputeProperties {

    /**
     * Distinct administrators required to execute a chargeback. Two is the four-eyes rule; setting
     * it to one removes the control entirely, so change it deliberately.
     */
    private int validationsRequired = 2;
}
