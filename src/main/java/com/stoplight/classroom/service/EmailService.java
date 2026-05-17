package com.stoplight.classroom.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional notifications for the teacher signup flow.
 *
 * <p>Designed to degrade gracefully: when {@code mail.from} is blank or no
 * {@link JavaMailSender} bean is configured (the default in tests and in dev), the
 * service logs the message it would have sent and returns without raising. This keeps
 * signup working in environments where SMTP/SES isn't wired up yet.</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;
    private final String adminNotificationAddress;
    private final String appBaseUrl;

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${mail.from:}") String from,
            @Value("${mail.admin-notification-address:}") String adminNotificationAddress,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl) {
        this.mailSenderProvider = mailSenderProvider;
        this.from = from == null ? "" : from.trim();
        this.adminNotificationAddress = adminNotificationAddress == null ? "" : adminNotificationAddress.trim();
        this.appBaseUrl = appBaseUrl == null ? "" : appBaseUrl.trim();
    }

    /** Confirmation that a public signup was received and is awaiting review. */
    @Async
    public void sendSignupReceived(String to, String username) {
        send(to,
                "Stoplight Classroom — signup received",
                "Hi " + username + ",\n\n" +
                        "Your teacher account request has been received and is pending review " +
                        "by an administrator. You'll get another email once it has been approved " +
                        "or rejected.\n\n" +
                        "— Stoplight Classroom");
    }

    /** Notify admins that a new signup is waiting in the queue. No-op if no address configured. */
    @Async
    public void sendAdminSignupAlert(String teacherUsername, String teacherEmail) {
        if (adminNotificationAddress.isEmpty()) {
            return;
        }
        String reviewUrl = appBaseUrl + "/admin/teacher-signups";
        send(adminNotificationAddress,
                "Stoplight Classroom — new teacher signup pending",
                "A new teacher signup is awaiting review.\n\n" +
                        "Username: " + teacherUsername + "\n" +
                        "Email:    " + teacherEmail + "\n\n" +
                        "Review at: " + reviewUrl + "\n");
    }

    @Async
    public void sendApproved(String to, String username) {
        String loginUrl = appBaseUrl + "/login";
        send(to,
                "Stoplight Classroom — your account is approved",
                "Hi " + username + ",\n\n" +
                        "Your teacher account has been approved. You can log in at:\n" +
                        loginUrl + "\n\n" +
                        "— Stoplight Classroom");
    }

    @Async
    public void sendRejected(String to, String username) {
        send(to,
                "Stoplight Classroom — signup not approved",
                "Hi " + username + ",\n\n" +
                        "Unfortunately your teacher account request was not approved. " +
                        "If you believe this is a mistake, please contact your school administrator.\n\n" +
                        "— Stoplight Classroom");
    }

    private void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.debug("Skipping email '{}' — no recipient address", subject);
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || from.isEmpty()) {
            log.info("[email-disabled] would send to={} subject={}", to, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.debug("Sent email to={} subject={}", to, subject);
        } catch (MailException ex) {
            // Don't let mail failures break the signup or approval flow.
            log.warn("Failed to send email to={} subject={}: {}", to, subject, ex.getMessage());
        }
    }
}
