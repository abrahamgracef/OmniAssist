package dev.abrahamgracef.omniassist.calendar;

import dev.abrahamgracef.omniassist.google.calendar.CalendarEvent;
import dev.abrahamgracef.omniassist.google.calendar.CreateEventRequest;
import dev.abrahamgracef.omniassist.google.calendar.GoogleCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingDraftControllerTest {

    private MeetingDraftService draftService;

    @Mock
    private GoogleCalendarService calendarService;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private dev.abrahamgracef.omniassist.admin.AuditService auditService;

    private MeetingDraftController controller;

    @BeforeEach
    void setUp() {
        draftService = new MeetingDraftService();
        controller = new MeetingDraftController(draftService, calendarService, authorizedClientService, auditService);
    }

    @Test
    void testUpdateDraft() {
        MeetingDraft draft = draftService.create(
                "Initial Title", "Notes", "Meet",
                "2026-10-06T10:00:00Z", "2026-10-06T11:00:00Z",
                List.of()
        );

        MeetingDraftController.UpdateDraftRequest updateReq =
                new MeetingDraftController.UpdateDraftRequest(
                        "Updated Title", "Updated Notes", "Room 101",
                        "2026-10-06T11:00:00Z", "2026-10-06T12:00:00Z",
                        List.of("alice@example.com")
                );

        MeetingDraft updated = controller.updateDraft(draft.id(), updateReq);
        assertThat(updated.title()).isEqualTo("Updated Title");
        assertThat(updated.location()).isEqualTo("Room 101");
        assertThat(updated.attendees()).containsExactly("alice@example.com");
    }

    @Test
    void testCancelDraft() {
        MeetingDraft draft = draftService.create(
                "To Cancel", "", "",
                "2026-10-06T10:00:00Z", "2026-10-06T11:00:00Z",
                List.of()
        );

        Map<String, String> response = controller.cancelDraft(draft.id());
        assertThat(response.get("status")).isEqualTo("cancelled");
        assertThatThrownBy(() -> draftService.get(draft.id()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testScheduleDraftSuccess() {
        MeetingDraft draft = draftService.create(
                "Final Demo", "Release candidate demo", "Google Meet",
                "2026-10-06T14:00:00Z", "2026-10-06T15:00:00Z",
                List.of("client@example.com")
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user@example.com", "pw", List.of());

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token-xyz",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        when(client.getAccessToken()).thenReturn(token);
        when(authorizedClientService.loadAuthorizedClient("google", "user@example.com")).thenReturn(client);

        CalendarEvent event = new CalendarEvent(
                "cal-evt-1", "Final Demo", "Release candidate demo", "Google Meet",
                "2026-10-06T14:00:00Z", "2026-10-06T15:00:00Z",
                "confirmed", "https://meet.link", List.of("client@example.com")
        );
        when(calendarService.createEvent(eq("token-xyz"), any(CreateEventRequest.class)))
                .thenReturn(event);

        Map<String, Object> result = controller.scheduleDraft(draft.id(), auth);

        assertThat(result.get("status")).isEqualTo("scheduled");
        assertThat(result.get("eventId")).isEqualTo("cal-evt-1");
        assertThat(result.get("htmlLink")).isEqualTo("https://meet.link");
    }

    @Test
    void testScheduleDraftRestoresOnFailure() {
        MeetingDraft draft = draftService.create(
                "Will Fail", "", "",
                "2026-10-06T14:00:00Z", "2026-10-06T15:00:00Z",
                List.of()
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user@example.com", "pw", List.of());

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token-xyz",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        when(client.getAccessToken()).thenReturn(token);
        when(authorizedClientService.loadAuthorizedClient("google", "user@example.com")).thenReturn(client);

        when(calendarService.createEvent(eq("token-xyz"), any(CreateEventRequest.class)))
                .thenThrow(new RuntimeException("Google API error"));

        assertThatThrownBy(() -> controller.scheduleDraft(draft.id(), auth))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Google API error");

        // Should be restored so user can retry
        assertThat(draftService.get(draft.id())).isEqualTo(draft);
    }
}
