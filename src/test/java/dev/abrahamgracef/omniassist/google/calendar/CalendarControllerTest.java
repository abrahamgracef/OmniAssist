package dev.abrahamgracef.omniassist.google.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock
    private GoogleCalendarService calendarService;

    @Mock
    private dev.abrahamgracef.omniassist.task.TaskService taskService;

    @Mock
    private dev.abrahamgracef.omniassist.user.CurrentUserService currentUserService;

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    private CalendarController controller;

    @BeforeEach
    public void setUp() {
        controller = new CalendarController(calendarService, taskService, currentUserService);

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "valid-test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        lenient().when(authorizedClient.getAccessToken()).thenReturn(token);
    }

    @Test
    void testGetUpcomingEvents() {
        CalendarEvent event = new CalendarEvent(
                "evt-1", "Standup", "Daily team sync", "Meet",
                "2026-10-06T09:00:00Z", "2026-10-06T09:30:00Z",
                "confirmed", "https://link", List.of("alice@example.com")
        );
        when(calendarService.getUpcomingEvents("valid-test-token", 10, 7))
                .thenReturn(List.of(event));

        List<CalendarEvent> result = controller.getUpcomingEvents(authorizedClient, 10, 7);
        assertThat(result).containsExactly(event);
    }

    @Test
    void testGetEventById() {
        CalendarEvent event = new CalendarEvent(
                "evt-2", "Sprint Review", "", "",
                "2026-10-06T15:00:00Z", "2026-10-06T16:00:00Z",
                "confirmed", "", List.of()
        );
        when(calendarService.getEvent("valid-test-token", "evt-2"))
                .thenReturn(event);

        CalendarEvent result = controller.getEvent(authorizedClient, "evt-2");
        assertThat(result).isEqualTo(event);
    }

    @Test
    void testCreateEvent() {
        CreateEventRequest request = new CreateEventRequest(
                "One on One", "Monthly 1:1", "Room 3",
                "2026-10-07T11:00:00Z", "2026-10-07T11:45:00Z",
                List.of("bob@example.com")
        );
        CalendarEvent created = new CalendarEvent(
                "evt-3", "One on One", "Monthly 1:1", "Room 3",
                "2026-10-07T11:00:00Z", "2026-10-07T11:45:00Z",
                "confirmed", "", List.of("bob@example.com")
        );
        when(calendarService.createEvent("valid-test-token", request))
                .thenReturn(created);

        CalendarEvent result = controller.createEvent(authorizedClient, request);
        assertThat(result).isEqualTo(created);
    }

    @Test
    void testUpdateEvent() {
        UpdateEventRequest request = new UpdateEventRequest(
                "Rescheduled 1:1", null, null,
                "2026-10-07T14:00:00Z", "2026-10-07T14:45:00Z",
                null
        );
        CalendarEvent updated = new CalendarEvent(
                "evt-3", "Rescheduled 1:1", "Monthly 1:1", "Room 3",
                "2026-10-07T14:00:00Z", "2026-10-07T14:45:00Z",
                "confirmed", "", List.of("bob@example.com")
        );
        when(calendarService.updateEvent("valid-test-token", "evt-3", request))
                .thenReturn(updated);

        CalendarEvent result = controller.updateEvent(authorizedClient, "evt-3", request);
        assertThat(result).isEqualTo(updated);
    }

    @Test
    void testDeleteEvent() {
        doNothing().when(calendarService).deleteEvent("valid-test-token", "evt-delete");

        Map<String, String> response = controller.deleteEvent(authorizedClient, "evt-delete");
        assertThat(response.get("status")).isEqualTo("deleted");
        assertThat(response.get("eventId")).isEqualTo("evt-delete");
    }

    @Test
    void testGetFreeSlots() {
        TimeSlot slot = new TimeSlot("start", "end", 30, "9:00 AM - 9:30 AM (30 mins)");
        when(calendarService.findFreeSlots("valid-test-token", "2026-10-06", 30, 9, 17))
                .thenReturn(List.of(slot));

        List<TimeSlot> slots = controller.getFreeSlots(authorizedClient, "2026-10-06", 30, 9, 17);
        assertThat(slots).containsExactly(slot);
    }

    @Test
    void testSummarizeUpcoming() {
        CalendarEvent event = new CalendarEvent(
                "e-sum", "Sprint Demo", "", "",
                "2026-10-06T10:00:00Z", "2026-10-06T11:00:00Z",
                "confirmed", "", List.of()
        );
        when(calendarService.getUpcomingEvents("valid-test-token", 20, 7))
                .thenReturn(List.of(event));

        Map<String, Object> summary = controller.summarizeUpcoming(authorizedClient, 7);
        assertThat(summary.get("totalEvents")).isEqualTo(1);
        assertThat(summary.get("nextMeeting")).isEqualTo(event);
    }
}
