package dev.abrahamgracef.omniassist.google.calendar;

import java.util.List;

public record UpdateEventRequest(
        String summary,
        String description,
        String location,
        String startDateTime,
        String endDateTime,
        List<String> attendees
) {
}
