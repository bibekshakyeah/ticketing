package com.example.ticketing.notification;

/**
 * Pre-built interface — DO NOT MODIFY.
 *
 * Sends notifications to customers. In production this would send real emails
 * via SMTP/SendGrid/SES. For this exercise, the provided implementation logs
 * to the console.
 */
public interface NotificationService {

    /**
     * Send an order confirmation email to the customer.
     *
     * @param to       the customer's email address
     * @param subject  the email subject line
     * @param body     the email body (plain text)
     */
    void sendEmail(String to, String subject, String body);
}
