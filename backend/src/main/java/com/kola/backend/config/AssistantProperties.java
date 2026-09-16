package com.kola.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The in-app assistant (KOLA.md 4.7 "accompagnement de l'utilisateur"), bound from
 * {@code app.assistant.*}.
 *
 * <p>{@code apiKey} has no default on purpose: {@code application.properties} is gitignored, so a
 * missing key must degrade into a clear "assistant unavailable" rather than into a half-working
 * feature. Everything else has a working default so the module boots with one property set.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.assistant")
public class AssistantProperties {

    /** Kill switch. Off means the endpoints answer 503 without ever calling the provider. */
    private boolean enabled = true;

    /** Anthropic API key. Blank disables the assistant, loudly, at startup. */
    private String apiKey = "";

    private String baseUrl = "https://api.anthropic.com/v1/messages";

    private String model = "claude-sonnet-5";

    /** Anthropic's dated API contract, sent as the {@code anthropic-version} header. */
    private String apiVersion = "2023-06-01";

    /** Answers are short by design: this is a help chat inside a mobile app, not an essay writer. */
    private int maxTokens = 900;

    private Duration timeout = Duration.ofSeconds(30);

    /**
     * How many past messages of a conversation are replayed to the model. Bounds both the cost and
     * how far back a user can push the model with earlier turns.
     */
    private int historyLimit = 12;

    /** Per user, per rolling day. The assistant is the only endpoint that costs money per call. */
    private int dailyMessageLimit = 40;

    /** A question longer than this is refused before it reaches the provider. */
    private int maxMessageLength = 1000;

    /** How many recent transactions go into the user's snapshot. */
    private int recentTransactions = 10;
}
