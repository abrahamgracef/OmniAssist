package dev.abrahamgracef.omniassist.email;

import java.util.UUID;

public record EmailDraft(
        UUID id,
        String to,
        String subject,
        String body
) {
}