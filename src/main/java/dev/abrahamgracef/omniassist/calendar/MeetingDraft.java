package dev.abrahamgracef.omniassist.calendar;

import java.util.List;
import java.util.UUID;

public record MeetingDraft(
        UUID id,
        String title,
        String description,
        String location,
        String startDateTime,
        String endDateTime,
        List<String> attendees
) {
}
