package com.secondstitch.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/** The two emails the app sends, with links back to the website. */
@Service
public class AuthEmails {

    private final EmailSender sender;
    private final String frontendUrl;

    public AuthEmails(EmailSender sender, @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.sender = sender;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    public boolean sendVerification(String to, String name, String code) {
        String link = frontendUrl + "/?verify=" + code;
        return send(to, "Confirm your email for Second Stitch", name,
                "Click the button to confirm your email and open your closet. The link works for 24 hours.",
                "Confirm my email", link);
    }

    public boolean sendPasswordReset(String to, String name, String code) {
        String link = frontendUrl + "/?reset=" + code;
        return send(to, "Reset your Second Stitch password", name,
                "Someone (hopefully you) asked to reset your password. The link works for 1 hour. "
                        + "If it wasn't you, ignore this email and your password stays the same.",
                "Choose a new password", link);
    }

    private boolean send(String to, String subject, String name, String message, String button, String link) {
        String hello = name == null || name.isBlank() ? "Hi," : "Hi " + name + ",";
        String text = hello + "\n\n" + message + "\n\n" + button + ": " + link + "\n";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #272a30;">
                  <h1 style="font-size: 22px;">Second Stitch</h1>
                  <p>%s</p>
                  <p>%s</p>
                  <p style="margin: 28px 0;">
                    <a href="%s" style="background: #6e2e5e; color: #ffffff; padding: 12px 20px;
                       border-radius: 999px; text-decoration: none; font-weight: bold;">%s</a>
                  </p>
                  <p style="font-size: 13px; color: #5d6168;">Or paste this link into your browser:<br>%s</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(hello), HtmlUtils.htmlEscape(message), link,
                HtmlUtils.htmlEscape(button), link);
        return sender.send(to, subject, html, text);
    }
}
