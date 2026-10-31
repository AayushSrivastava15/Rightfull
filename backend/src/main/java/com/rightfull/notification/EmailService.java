package com.rightfull.notification;

import java.time.Instant;

/**
 * Contract for asynchronous email notifications and team invitation dispatches.
 */
public interface EmailService {

    void sendInvitationEmail(String recipientEmail, String workspaceName, String role, String invitationToken, Instant expiresAt);
}

