package dev.abrahamgracef.omniassist.google.calendar;

import java.util.List;

public record CreateEventRequest(
        String summary,
        String description,
        String location,
        String startDateTime,
        String endDateTime,
        List<String> attendees,
        List<String> recurrence
) {
    public CreateEventRequest(
            String summary,
            String description,
            String location,
            String startDateTime,
            String endDateTime,
            List<String> attendees
    ) {
        this(summary, description, location, startDateTime, endDateTime, attendees, null);
    }
}
