package com.rightfull.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Development and fallback implementation of EmailService.
 * Explicitly logs invitation dispatch and indicates that no third-party email provider is configured.
 */
@Service
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendInvitationEmail(String recipientEmail, String workspaceName, String role, String invitationToken, Instant expiresAt) {
        log.warn("No production email gateway configured (Resend/SendGrid/SES). Invitation token for recipient='{}' in workspace='{}' (role={}, expiresAt={}) is logged for development: token={}",
                recipientEmail, workspaceName, role, expiresAt, invitationToken);
    }
}

