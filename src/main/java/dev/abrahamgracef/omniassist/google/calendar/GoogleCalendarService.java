package dev.abrahamgracef.omniassist.google.calendar;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GoogleCalendarService {

    private final RestClient restClient;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a");

    public GoogleCalendarService(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://www.googleapis.com/calendar/v3")
                .build();
    }

    /**
     * Retrieve upcoming events starting from now up to daysAhead days.
     */
    public List<CalendarEvent> getUpcomingEvents(
            String accessToken,
            int maxResults,
            int daysAhead) {

        int safeLimit = Math.max(1, Math.min(maxResults, 50));
        Instant now = Instant.now();
        Instant maxTime = daysAhead > 0 ? now.plus(Duration.ofDays(daysAhead)) : null;

        return getEventsInRange(accessToken, now, maxTime, safeLimit);
    }

    /**
     * Retrieve events within a specific time window.
     */
    public List<CalendarEvent> getEventsInRange(
            String accessToken,
            Instant timeMin,
            Instant timeMax,
            int maxResults) {

        Map response = restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/calendars/primary/events")
                            .queryParam("singleEvents", true)
                            .queryParam("orderBy", "startTime")
                            .queryParam("maxResults", Math.max(1, Math.min(maxResults, 250)));

                    if (timeMin != null) {
                        builder.queryParam("timeMin", timeMin.toString());
                    }
                    if (timeMax != null) {
                        builder.queryParam("timeMax", timeMax.toString());
                    }

                    return builder.build();
                })
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("items") == null) {
            return List.of();
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
        List<CalendarEvent> events = new ArrayList<>();

        for (Map<String, Object> item : items) {
            events.add(mapToCalendarEvent(item));
        }

        return events;
    }

    /**
     * Retrieve a single event by ID.
     */
    public CalendarEvent getEvent(String accessToken, String eventId) {
        Map response = restClient.get()
                .uri("/calendars/primary/events/{id}", eventId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Event not found with ID: " + eventId);
        }

        return mapToCalendarEvent(response);
    }

    /**
     * Create a new calendar event.
     */
    public CalendarEvent createEvent(String accessToken, CreateEventRequest request) {
        if (request.summary() == null || request.summary().isBlank()) {
            throw new IllegalArgumentException("Event summary / title cannot be blank.");
        }
        if (request.startDateTime() == null || request.startDateTime().isBlank()) {
            throw new IllegalArgumentException("Event start date/time cannot be blank.");
        }
        if (request.endDateTime() == null || request.endDateTime().isBlank()) {
            throw new IllegalArgumentException("Event end date/time cannot be blank.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("summary", request.summary());

        if (request.description() != null && !request.description().isBlank()) {
            body.put("description", request.description());
        }
        if (request.location() != null && !request.location().isBlank()) {
            body.put("location", request.location());
        }

        body.put("start", formatDateTimeMap(request.startDateTime()));
        body.put("end", formatDateTimeMap(request.endDateTime()));

        if (request.attendees() != null && !request.attendees().isEmpty()) {
            List<Map<String, String>> attendeeMaps = request.attendees().stream()
                    .filter(a -> a != null && !a.isBlank())
                    .map(a -> Map.of("email", a.trim()))
                    .toList();
            if (!attendeeMaps.isEmpty()) {
                body.put("attendees", attendeeMaps);
            }
        }

        if (request.recurrence() != null && !request.recurrence().isEmpty()) {
            body.put("recurrence", request.recurrence());
        }

        Map response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/calendars/primary/events")
                        .queryParam("conferenceDataVersion", 1)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("id") == null) {
            throw new IllegalStateException("Google Calendar did not return an event ID");
        }

        return mapToCalendarEvent(response);
    }

    /**
     * Update an existing event.
     */
    public CalendarEvent updateEvent(String accessToken, String eventId, UpdateEventRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();

        if (request.summary() != null && !request.summary().isBlank()) {
            body.put("summary", request.summary());
        }
        if (request.description() != null) {
            body.put("description", request.description());
        }
        if (request.location() != null) {
            body.put("location", request.location());
        }
        if (request.startDateTime() != null && !request.startDateTime().isBlank()) {
            body.put("start", formatDateTimeMap(request.startDateTime()));
        }
        if (request.endDateTime() != null && !request.endDateTime().isBlank()) {
            body.put("end", formatDateTimeMap(request.endDateTime()));
        }
        if (request.attendees() != null) {
            List<Map<String, String>> attendeeMaps = request.attendees().stream()
                    .filter(a -> a != null && !a.isBlank())
                    .map(a -> Map.of("email", a.trim()))
                    .toList();
            body.put("attendees", attendeeMaps);
        }

        Map response = restClient.patch()
                .uri("/calendars/primary/events/{id}", eventId)
                .header("Authorization", "Bearer " + accessToken)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Failed to update Google Calendar event: " + eventId);
        }

        return mapToCalendarEvent(response);
    }

    /**
     * Delete an event by ID.
     */
    public void deleteEvent(String accessToken, String eventId) {
        restClient.delete()
                .uri("/calendars/primary/events/{id}", eventId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Check if a proposed time range conflicts with any existing scheduled events.
     */
    public List<CalendarEvent> findConflicts(
            String accessToken,
            String startDateTimeStr,
            String endDateTimeStr) {

        ZoneId zoneId = ZoneId.systemDefault();
        Instant reqStart = parseInstant(startDateTimeStr, zoneId);
        Instant reqEnd = parseInstant(endDateTimeStr, zoneId);

        if (reqStart == null || reqEnd == null) {
            return List.of();
        }

        Instant searchStart = reqStart.minus(Duration.ofHours(2));
        Instant searchEnd = reqEnd.plus(Duration.ofHours(2));

        List<CalendarEvent> events = getEventsInRange(accessToken, searchStart, searchEnd, 50);
        List<CalendarEvent> conflicts = new ArrayList<>();

        for (CalendarEvent event : events) {
            if ("cancelled".equalsIgnoreCase(event.status())) {
                continue;
            }
            Instant eStart = parseInstant(event.start(), zoneId);
            Instant eEnd = parseInstant(event.end(), zoneId);

            if (eStart != null && eEnd != null) {
                if (eStart.isBefore(reqEnd) && eEnd.isAfter(reqStart)) {
                    conflicts.add(event);
                }
            }
        }

        return conflicts;
    }

    /**
     * Calculate and suggest free time slots for a specified date and meeting duration.
     */
    public List<TimeSlot> findFreeSlots(
            String accessToken,
            String dateStr,
            int durationMinutes,
            Integer startHourParam,
            Integer endHourParam) {

        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate targetDate = parseTargetDate(dateStr, zoneId);

        int startHour = (startHourParam != null && startHourParam >= 0 && startHourParam < 24)
                ? startHourParam : 9;
        int endHour = (endHourParam != null && endHourParam > startHour && endHourParam <= 24)
                ? endHourParam : 17;
        int requiredDuration = Math.max(15, durationMinutes > 0 ? durationMinutes : 30);

        ZonedDateTime windowStart = targetDate.atTime(LocalTime.of(startHour, 0)).atZone(zoneId);
        ZonedDateTime windowEnd = targetDate.atTime(LocalTime.of(endHour == 24 ? 23 : endHour, endHour == 24 ? 59 : 0)).atZone(zoneId);

        // If checking today, don't suggest past hours
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        if (targetDate.isEqual(now.toLocalDate()) && now.isAfter(windowStart)) {
            // Round now up to next 15-minute mark
            int minute = now.getMinute();
            int remainder = minute % 15;
            ZonedDateTime roundedNow = now.plusMinutes(remainder == 0 ? 0 : (15 - remainder)).withSecond(0).withNano(0);
            windowStart = roundedNow;
        }

        if (!windowStart.isBefore(windowEnd)) {
            return List.of();
        }

        List<CalendarEvent> events = getEventsInRange(
                accessToken,
                windowStart.toInstant(),
                windowEnd.toInstant(),
                100
        );

        // Collect busy intervals
        List<Interval> busyIntervals = new ArrayList<>();
        for (CalendarEvent event : events) {
            if ("cancelled".equalsIgnoreCase(event.status())) {
                continue;
            }
            Instant sInst = parseInstant(event.start(), zoneId);
            Instant eInst = parseInstant(event.end(), zoneId);
            if (sInst != null && eInst != null && eInst.isAfter(sInst)) {
                ZonedDateTime bStart = sInst.atZone(zoneId);
                ZonedDateTime bEnd = eInst.atZone(zoneId);

                // Clamp to window
                if (bStart.isBefore(windowStart)) bStart = windowStart;
                if (bEnd.isAfter(windowEnd)) bEnd = windowEnd;

                if (bEnd.isAfter(bStart)) {
                    busyIntervals.add(new Interval(bStart, bEnd));
                }
            }
        }

        // Sort busy intervals
        busyIntervals.sort(Comparator.comparing(Interval::start));

        // Merge overlapping busy intervals
        List<Interval> mergedBusy = new ArrayList<>();
        for (Interval current : busyIntervals) {
            if (mergedBusy.isEmpty()) {
                mergedBusy.add(current);
            } else {
                Interval last = mergedBusy.get(mergedBusy.size() - 1);
                if (!current.start().isAfter(last.end())) {
                    ZonedDateTime newEnd = current.end().isAfter(last.end()) ? current.end() : last.end();
                    mergedBusy.set(mergedBusy.size() - 1, new Interval(last.start(), newEnd));
                } else {
                    mergedBusy.add(current);
                }
            }
        }

        // Calculate free gaps
        List<TimeSlot> slots = new ArrayList<>();
        ZonedDateTime pointer = windowStart;

        for (Interval busy : mergedBusy) {
            if (busy.start().isAfter(pointer)) {
                long gapMinutes = Duration.between(pointer, busy.start()).toMinutes();
                if (gapMinutes >= requiredDuration) {
                    slots.add(createSlot(pointer, busy.start(), gapMinutes));
                }
            }
            if (busy.end().isAfter(pointer)) {
                pointer = busy.end();
            }
        }

        if (windowEnd.isAfter(pointer)) {
            long gapMinutes = Duration.between(pointer, windowEnd).toMinutes();
            if (gapMinutes >= requiredDuration) {
                slots.add(createSlot(pointer, windowEnd, gapMinutes));
            }
        }

        return slots;
    }

    /**
     * Automatically schedule focus blocks for pending tasks into available calendar gaps.
     */
    public List<CalendarEvent> autoBlockFocusTime(
            String accessToken,
            List<dev.abrahamgracef.omniassist.task.Task> tasks,
            LocalDate date,
            int defaultBlockMinutes) {

        int blockMins = defaultBlockMinutes > 0 ? defaultBlockMinutes : 45;
        List<TimeSlot> slots = findFreeSlots(accessToken, date.toString(), blockMins, 9, 17);

        List<CalendarEvent> createdEvents = new ArrayList<>();
        int slotIndex = 0;

        for (dev.abrahamgracef.omniassist.task.Task task : tasks) {
            if (slotIndex >= slots.size()) {
                break;
            }
            TimeSlot slot = slots.get(slotIndex);

            Instant slotStart = Instant.parse(slot.start());
            Instant blockEnd = slotStart.plus(Duration.ofMinutes(blockMins));

            CreateEventRequest request = new CreateEventRequest(
                    "🎯 Focus Work: " + task.getTitle(),
                    "Reserved deep work time block for task: " + (task.getDescription() != null ? task.getDescription() : task.getTitle()),
                    "Focus Mode",
                    slot.start(),
                    blockEnd.toString(),
                    List.of()
            );

            try {
                CalendarEvent event = createEvent(accessToken, request);
                createdEvents.add(event);
                slotIndex++;
            } catch (Exception ignored) {
            }
        }

        return createdEvents;
    }

    private TimeSlot createSlot(ZonedDateTime start, ZonedDateTime end, long minutes) {
        String formatted = String.format("%s - %s (%d mins)",
                start.format(TIME_FORMATTER),
                end.format(TIME_FORMATTER),
                minutes);
        return new TimeSlot(
                start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                minutes,
                formatted
        );
    }

    private LocalDate parseTargetDate(String dateStr, ZoneId zoneId) {
        if (dateStr == null || dateStr.isBlank() || "today".equalsIgnoreCase(dateStr.trim())) {
            return LocalDate.now(zoneId);
        }
        String clean = dateStr.trim().toLowerCase();
        if ("tomorrow".equals(clean)) {
            return LocalDate.now(zoneId).plusDays(1);
        }
        try {
            return LocalDate.parse(clean);
        } catch (Exception e) {
            return LocalDate.now(zoneId);
        }
    }

    private Instant parseInstant(String dateTimeStr, ZoneId zoneId) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        String trimmed = dateTimeStr.trim();
        // If YYYY-MM-DD
        if (trimmed.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return LocalDate.parse(trimmed).atStartOfDay(zoneId).toInstant();
        }
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (Exception ex) {
            try {
                return Instant.parse(trimmed);
            } catch (Exception ex2) {
                try {
                    String iso = trimmed.replace(" ", "T");
                    return LocalDateTime.parse(iso).atZone(zoneId).toInstant();
                } catch (Exception ex3) {
                    return null;
                }
            }
        }
    }

    private CalendarEvent mapToCalendarEvent(Map<String, Object> map) {
        String id = (String) map.get("id");
        String summary = (String) map.getOrDefault("summary", "(No title)");
        String description = (String) map.getOrDefault("description", "");
        String location = (String) map.getOrDefault("location", "");
        String status = (String) map.getOrDefault("status", "confirmed");
        String htmlLink = (String) map.getOrDefault("htmlLink", "");

        String start = extractDateTime(map.get("start"));
        String end = extractDateTime(map.get("end"));

        List<String> attendees = new ArrayList<>();
        if (map.get("attendees") instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> attMap && attMap.get("email") != null) {
                    attendees.add(attMap.get("email").toString());
                }
            }
        }

        return new CalendarEvent(
                id,
                summary,
                description,
                location,
                start,
                end,
                status,
                htmlLink,
                attendees
        );
    }

    private String extractDateTime(Object dateObj) {
        if (dateObj instanceof Map<?, ?> dateMap) {
            if (dateMap.get("dateTime") != null) {
                return dateMap.get("dateTime").toString();
            }
            if (dateMap.get("date") != null) {
                return dateMap.get("date").toString();
            }
        }
        return "";
    }

    private Map<String, String> formatDateTimeMap(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Date/time cannot be empty");
        }
        String trimmed = input.trim();
        if (trimmed.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return Map.of("date", trimmed);
        }
        if (trimmed.endsWith("Z") || trimmed.matches(".*[+-]\\d{2}:?\\d{2}$")) {
            return Map.of("dateTime", trimmed);
        }
        try {
            String isoString = trimmed.replace(" ", "T");
            LocalDateTime ldt = LocalDateTime.parse(isoString.length() == 16 ? isoString + ":00" : isoString);
            ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
            return Map.of("dateTime", zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } catch (Exception e) {
            return Map.of("dateTime", trimmed);
        }
    }

    private record Interval(ZonedDateTime start, ZonedDateTime end) {}
}
