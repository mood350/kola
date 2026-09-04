package com.dogaa.backend.modules.notification;

import com.dogaa.backend.common.enums.NotificationChannel;
import com.dogaa.backend.modules.notification.entity.Notification;
import com.dogaa.backend.modules.notification.provider.EmailNotificationProvider;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;

class EmailNotificationProviderTest {

    /**
     * SpringTemplateEngine (not the plain thymeleaf-core TemplateEngine) so the
     * standard dialect resolves variables through SpringEL rather than OGNL,
     * which is not on the classpath here — same engine Spring wires at runtime.
     */
    private static TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Test
    void rendersTheTitleAndBodyIntoTheHtmlTemplate() {
        EmailNotificationProvider provider = new EmailNotificationProvider(templateEngine());
        Notification notification = new Notification();
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setTitle("Bienvenue sur Dogaa");
        notification.setBody("Votre compte a ete cree avec succes.");

        String html = provider.render(notification);

        assertThat(html).contains("Bienvenue sur Dogaa");
        assertThat(html).contains("Votre compte a ete cree avec succes.");
    }

    @Test
    void sendReportsSuccess() {
        EmailNotificationProvider provider = new EmailNotificationProvider(templateEngine());
        Notification notification = new Notification();
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setTitle("Titre");
        notification.setBody("Corps");

        assertThat(provider.send(notification)).isTrue();
    }
}
