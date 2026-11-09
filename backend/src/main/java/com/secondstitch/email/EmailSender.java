package com.secondstitch.email;

/** Sends an email. Brevo on the live site, the console on your laptop (see EmailConfig). */
public interface EmailSender {

    /** Returns false if the email couldn't be sent (the error is logged). */
    boolean send(String to, String subject, String html, String text);
}
