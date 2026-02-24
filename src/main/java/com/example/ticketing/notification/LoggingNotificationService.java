package com.example.ticketing.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pre-built implementation — DO NOT MODIFY.
 *
 * Logs emails to the console instead of sending real mail.
 * Also stores sent messages in memory so tests can verify notification behavior.
 */
@Service
public class LoggingNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

    private final List<SentMessage> sentMessages = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("EMAIL SENT → to={}, subject={}", to, subject);
        log.debug("EMAIL BODY:\n{}", body);
        sentMessages.add(new SentMessage(to, subject, body));
    }

    /** Returns all emails sent since the last {@link #clear()}. Useful for test assertions. */
    public List<SentMessage> getSentMessages() {
        return Collections.unmodifiableList(new ArrayList<>(sentMessages));
    }

    /** Clears the sent message history. Call this in test @BeforeEach. */
    public void clear() {
        sentMessages.clear();
    }

    public record SentMessage(String to, String subject, String body) {}
}
