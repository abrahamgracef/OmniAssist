package dev.abrahamgracef.omniassist.ai.tools;

import dev.abrahamgracef.omniassist.calendar.MeetingDraft;
import dev.abrahamgracef.omniassist.calendar.MeetingDraftService;
import dev.abrahamgracef.omniassist.google.calendar.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarToolsTest {

    @Mock
    private GoogleCalendarService calendarService;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private dev.abrahamgracef.omniassist.task.TaskService taskService;

    @Mock
    private dev.abrahamgracef.omniassist.user.CurrentUserService currentUserService;

    private MeetingDraftService draftService;
    private CalendarTools tools;

    @BeforeEach
    void setUp() {
        draftService = new MeetingDraftService();
        tools = new CalendarTools(calendarService, authorizedClientService, draftService, taskService, currentUserService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateMeetingDraftDoesNotRequireGoogleAuth() {
        MeetingDraft draft = tools.createMeetingDraft(
                "Quick Sync",
                "2026-10-06T10:00:00Z",
                "2026-10-06T10:30:00Z",
                "Discuss PR",
                "Google Meet",
                List.of("colleague@example.com")
        );

        assertThat(draft).isNotNull();
        assertThat(draft.title()).isEqualTo("Quick Sync");
        assertThat(draftService.consumeLatestDraft()).isEqualTo(draft);
    }

    @Test
    void testThrowsWhenNotAuthenticated() {
        assertThatThrownBy(() -> tools.listUpcomingEvents(5, 7))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google account is not connected");
    }

    @Test
    void testListUpcomingEventsWhenAuthenticated() {
        setupMockAuth("test-user", "mock-token");

        CalendarEvent event = new CalendarEvent(
                "e1", "Demo", "Description", "Location",
                "2026-10-06T10:00:00Z", "2026-10-06T11:00:00Z",
                "confirmed", "https://link", List.of()
        );
        when(calendarService.getUpcomingEvents("mock-token", 5, 7))
                .thenReturn(List.of(event));

        List<CalendarEvent> results = tools.listUpcomingEvents(5, 7);
        assertThat(results).containsExactly(event);
    }

    @Test
    void testDirectCreateCalendarEvent() {
        setupMockAuth("test-user", "mock-token");

        CalendarEvent event = new CalendarEvent(
                "e2", "Sync", "Desc", "Loc",
                "2026-10-06T15:00:00Z", "2026-10-06T15:30:00Z",
                "confirmed", "", List.of()
        );
        when(calendarService.createEvent(eq("mock-token"), any(CreateEventRequest.class)))
                .thenReturn(event);

        CalendarEvent created = tools.createCalendarEvent(
                "Sync",
                "2026-10-06T15:00:00Z",
                "2026-10-06T15:30:00Z",
                "Desc",
                "Loc",
                List.of("dev@example.com")
        );

        assertThat(created).isEqualTo(event);
    }

    @Test
    void testSuggestFreeTimeSlots() {
        setupMockAuth("test-user", "mock-token");

        TimeSlot slot = new TimeSlot("start", "end", 60, "10:00 AM - 11:00 AM (60 mins)");
        when(calendarService.findFreeSlots("mock-token", "2026-10-06", 60, 9, 17))
                .thenReturn(List.of(slot));

        List<TimeSlot> slots = tools.suggestFreeTimeSlots("2026-10-06", 60, 9, 17);
        assertThat(slots).containsExactly(slot);
    }

    @Test
    void testDeleteCalendarEvent() {
        setupMockAuth("test-user", "mock-token");

        doNothing().when(calendarService).deleteEvent("mock-token", "evt-123");

        String result = tools.deleteCalendarEvent("evt-123");
        assertThat(result).contains("Successfully deleted calendar event with ID: evt-123");
        verify(calendarService).deleteEvent("mock-token", "evt-123");
    }

    @Test
    void testTimeBlockPendingTasksNoTasks() {
        when(taskService.getDailyPlannerTasks(any(), any())).thenReturn(List.of());
        String result = tools.timeBlockPendingTasks("2026-10-06");
        assertThat(result).contains("No pending tasks found to schedule");
    }

    @Test
    void testTimeBlockPendingTasksRequiresAuthWhenTasksExist() {
        dev.abrahamgracef.omniassist.task.Task t = new dev.abrahamgracef.omniassist.task.Task();
        t.setTitle("Important Task");
        when(taskService.getDailyPlannerTasks(any(), any())).thenReturn(List.of(t));

        assertThatThrownBy(() -> tools.timeBlockPendingTasks("2026-10-06"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google account is not connected");
    }

    private void setupMockAuth(String username, String tokenValue) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, "pass", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        when(client.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientService.loadAuthorizedClient("google", username)).thenReturn(client);
    }
}
