package com.secondstitch.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Sends email through Brevo's web API (https://api.brevo.com/v3/smtp/email).
 *
 * Why not Gmail directly? Render's free plan blocks the ports email servers use
 * (25, 465, 587). Brevo is reached over normal HTTPS, which is allowed, and sends
 * from your verified Gmail address on your behalf.
 */
public class BrevoEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailSender.class);

    private final RestClient http;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailSender(String apiKey, String fromEmail, String fromName) {
        this.http = RestClient.builder().baseUrl("https://api.brevo.com/v3").build();
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public boolean send(String to, String subject, String html, String text) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", fromName, "email", fromEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", html,
                "textContent", text);
        try {
            http.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Brevo couldn't send the email to {}: {}", to, e.getMessage());
            return false;
        }
    }
}
