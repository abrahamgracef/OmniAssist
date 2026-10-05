package dev.abrahamgracef.omniassist.google.calendar;

import java.util.List;

public record CalendarEvent(
        String id,
        String summary,
        String description,
        String location,
        String start,
        String end,
        String status,
        String htmlLink,
        List<String> attendees
) {
}
