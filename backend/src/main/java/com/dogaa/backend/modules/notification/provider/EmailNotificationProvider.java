package com.dogaa.backend.modules.notification.provider;

import com.dogaa.backend.common.enums.NotificationChannel;
import com.dogaa.backend.modules.notification.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Placeholder: renders the {@code emails/notification} Thymeleaf template
 * and logs the resulting HTML instead of calling a real email gateway (e.g.
 * SES, SendGrid). Wire a real client here (and keep the template) when one
 * is chosen.
 */
@Component
public class EmailNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationProvider.class);

    private final TemplateEngine templateEngine;

    public EmailNotificationProvider(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean send(Notification notification) {
        String html = render(notification);
        log.info("[EMAIL] to user {} - {}: rendered {} characters of HTML (dev sender, configure a real gateway before going live)",
                notification.getUserId(), notification.getTitle(), html.length());
        log.debug("[EMAIL] rendered body:\n{}", html);
        return true;
    }

    public String render(Notification notification) {
        Context context = new Context();
        context.setVariable("title", notification.getTitle());
        context.setVariable("body", notification.getBody());
        return templateEngine.process("emails/notification", context);
    }
}
