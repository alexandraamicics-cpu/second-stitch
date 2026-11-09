package com.secondstitch.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Picks how emails are sent: Brevo when BREVO_API_KEY is set, otherwise printed to the console. */
@Configuration
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    EmailSender emailSender(@Value("${app.brevo-api-key:}") String apiKey,
                            @Value("${app.mail-from:}") String fromEmail,
                            @Value("${app.mail-from-name:Second Stitch}") String fromName) {
        if (apiKey.isBlank() || fromEmail.isBlank()) {
            log.warn("No BREVO_API_KEY / MAIL_FROM set: emails will be printed to the console, not sent.");
            return new ConsoleEmailSender();
        }
        return new BrevoEmailSender(apiKey, fromEmail, fromName);
    }
}
