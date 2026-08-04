package dev.abrahamgracef.omniassist.google.gmail;

public record EmailSummary(
        String id,
        String from,
        String subject,
        String snippet
) {}