package com.kola.backend.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                  EmailService.java                          ║
 * ║         Envoi des emails via Gmail SMTP + Thymeleaf         ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * @Async → les emails sont envoyés dans un thread séparé.
 * L'utilisateur n'attend pas que l'email soit envoyé pour
 * recevoir la réponse de l'API. ✅
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    /**
     * Méthode générique d'envoi d'email HTML.
     *
     * @param to           Destinataire
     * @param subject      Sujet de l'email
     * @param template     Template Thymeleaf à utiliser
     * @param properties   Variables injectées dans le template
     * @param from         Expéditeur
     */
    @Async
    public void sendEmail(
            String to,
            String subject,
            EmailTemplateName template,
            Map<String, Object> properties,
            String from
    ) throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED,
                StandardCharsets.UTF_8.name()
        );

        // Injection des variables dans le template Thymeleaf
        Context context = new Context();
        context.setVariables(properties);

        // Rendu du template HTML
        String htmlContent = templateEngine.process(template.getName(), context);

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML

        mailSender.send(mimeMessage);
        log.info("Email envoyé à {} — sujet : {}", to, subject);
    }
}