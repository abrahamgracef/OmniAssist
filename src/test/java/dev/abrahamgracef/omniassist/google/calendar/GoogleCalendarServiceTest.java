package dev.abrahamgracef.omniassist.google.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GoogleCalendarServiceTest {

    private GoogleCalendarService service;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        service = new GoogleCalendarService(builder);
    }

    @Test
    void testGetUpcomingEvents() {
        String jsonResponse = """
            {
              "items": [
                {
                  "id": "event1",
                  "summary": "Team Standup",
                  "description": "Daily sync",
                  "location": "Google Meet",
                  "status": "confirmed",
                  "htmlLink": "https://calendar.google.com/event1",
                  "start": { "dateTime": "2026-10-06T09:00:00Z" },
                  "end": { "dateTime": "2026-10-06T09:30:00Z" },
                  "attendees": [
                    { "email": "alice@example.com" },
                    { "email": "bob@example.com" }
                  ]
                }
              ]
            }
            """;

        mockServer.expect(requestTo(containsString("/calendars/primary/events")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer dummy-token"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<CalendarEvent> events = service.getUpcomingEvents("dummy-token", 5, 7);

        assertThat(events).hasSize(1);
        CalendarEvent event = events.get(0);
        assertThat(event.id()).isEqualTo("event1");
        assertThat(event.summary()).isEqualTo("Team Standup");
        assertThat(event.location()).isEqualTo("Google Meet");
        assertThat(event.attendees()).containsExactly("alice@example.com", "bob@example.com");

        mockServer.verify();
    }

    @Test
    void testCreateEvent() {
        String jsonResponse = """
            {
              "id": "new-event-123",
              "summary": "Architecture Review",
              "description": "Review API spec",
              "location": "Room 404",
              "status": "confirmed",
              "htmlLink": "https://calendar.google.com/event123",
              "start": { "dateTime": "2026-10-06T14:00:00Z" },
              "end": { "dateTime": "2026-10-06T15:00:00Z" },
              "attendees": [{ "email": "john@example.com" }]
            }
            """;

        mockServer.expect(requestTo(containsString("/calendars/primary/events")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer dummy-token"))
                .andExpect(jsonPath("$.summary").value("Architecture Review"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        CreateEventRequest request = new CreateEventRequest(
                "Architecture Review",
                "Review API spec",
                "Room 404",
                "2026-10-06T14:00:00Z",
                "2026-10-06T15:00:00Z",
                List.of("john@example.com")
        );

        CalendarEvent created = service.createEvent("dummy-token", request);

        assertThat(created).isNotNull();
        assertThat(created.id()).isEqualTo("new-event-123");
        assertThat(created.summary()).isEqualTo("Architecture Review");

        mockServer.verify();
    }

    @Test
    void testUpdateEvent() {
        String jsonResponse = """
            {
              "id": "event-to-update",
              "summary": "Rescheduled Sync",
              "start": { "dateTime": "2026-10-06T16:00:00Z" },
              "end": { "dateTime": "2026-10-06T17:00:00Z" }
            }
            """;

        mockServer.expect(requestTo(containsString("/calendars/primary/events/event-to-update")))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("Authorization", "Bearer dummy-token"))
                .andExpect(jsonPath("$.summary").value("Rescheduled Sync"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        UpdateEventRequest request = new UpdateEventRequest(
                "Rescheduled Sync",
                null,
                null,
                "2026-10-06T16:00:00Z",
                "2026-10-06T17:00:00Z",
                null
        );

        CalendarEvent updated = service.updateEvent("dummy-token", "event-to-update", request);

        assertThat(updated.summary()).isEqualTo("Rescheduled Sync");
        mockServer.verify();
    }

    @Test
    void testDeleteEvent() {
        mockServer.expect(requestTo(containsString("/calendars/primary/events/event-to-delete")))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer dummy-token"))
                .andRespond(withNoContent());

        service.deleteEvent("dummy-token", "event-to-delete");
        mockServer.verify();
    }

    @Test
    void testFindFreeSlotsWithBusySchedule() {
        // Mocking events for a specific future date so now() does not truncate the morning
        String jsonResponse = """
            {
              "items": [
                {
                  "id": "m1",
                  "summary": "Morning Standup",
                  "start": { "dateTime": "2028-10-06T09:00:00Z" },
                  "end": { "dateTime": "2028-10-06T10:00:00Z" }
                },
                {
                  "id": "m2",
                  "summary": "Sprint Planning",
                  "start": { "dateTime": "2028-10-06T11:00:00Z" },
                  "end": { "dateTime": "2028-10-06T12:00:00Z" }
                }
              ]
            }
            """;

        mockServer.expect(requestTo(containsString("/calendars/primary/events")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<TimeSlot> slots = service.findFreeSlots(
                "dummy-token",
                "2028-10-06",
                30,
                9,
                17
        );

        assertThat(slots).isNotEmpty();
        // There should be a slot between 10:00 and 11:00 (60 mins)
        boolean hasSlotBetweenStandupAndPlanning = slots.stream()
                .anyMatch(s -> s.durationMinutes() >= 30);
        assertThat(hasSlotBetweenStandupAndPlanning).isTrue();

        mockServer.verify();
    }
}
