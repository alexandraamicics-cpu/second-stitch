package com.secondstitch.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Used when no BREVO_API_KEY is set (your laptop, and the tests): instead of sending,
 * it prints the email to the console, so you can copy the link from IntelliJ's Run panel.
 * It also remembers recent emails, which the tests read.
 */
public class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    public record Sent(String to, String subject, String text) {
    }

    private final List<Sent> sent = Collections.synchronizedList(new ArrayList<>());

    @Override
    public boolean send(String to, String subject, String html, String text) {
        sent.add(new Sent(to, subject, text));
        log.info("""

                ---------- EMAIL (not sent: no BREVO_API_KEY set) ----------
                To: {}
                Subject: {}

                {}
                -------------------------------------------------------------""", to, subject, text);
        return true;
    }

    public List<Sent> sent() {
        return List.copyOf(sent);
    }

    public void clear() {
        sent.clear();
    }
}
