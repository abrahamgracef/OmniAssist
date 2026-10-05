package dev.abrahamgracef.omniassist.google.calendar;

public record TimeSlot(
        String start,
        String end,
        long durationMinutes,
        String formattedSlot
) {
}
