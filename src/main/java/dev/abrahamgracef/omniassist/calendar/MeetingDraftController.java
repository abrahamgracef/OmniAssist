package dev.abrahamgracef.omniassist.calendar;

import dev.abrahamgracef.omniassist.google.calendar.CalendarEvent;
import dev.abrahamgracef.omniassist.google.calendar.CreateEventRequest;
import dev.abrahamgracef.omniassist.google.calendar.GoogleCalendarService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/calendar/draft")
public class MeetingDraftController {

    private final MeetingDraftService draftService;
    private final GoogleCalendarService calendarService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final dev.abrahamgracef.omniassist.admin.AuditService auditService;

    public MeetingDraftController(
            MeetingDraftService draftService,
            GoogleCalendarService calendarService,
            OAuth2AuthorizedClientService authorizedClientService,
            dev.abrahamgracef.omniassist.admin.AuditService auditService) {

        this.draftService = draftService;
        this.calendarService = calendarService;
        this.authorizedClientService = authorizedClientService;
        this.auditService = auditService;
    }

    @PutMapping("/{id}")
    public MeetingDraft updateDraft(
            @PathVariable UUID id,
            @RequestBody UpdateDraftRequest request) {

        return draftService.update(
                id,
                request.title(),
                request.description(),
                request.location(),
                request.startDateTime(),
                request.endDateTime(),
                request.attendees()
        );
    }

    @DeleteMapping("/{id}")
    public Map<String, String> cancelDraft(@PathVariable UUID id) {
        draftService.delete(id);
        return Map.of("status", "cancelled");
    }

    @PostMapping("/{id}/schedule")
    public Map<String, Object> scheduleDraft(
            @PathVariable UUID id,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Google account is not connected.");
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                "google",
                authentication.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("Google account is not connected.");
        }

        MeetingDraft draft = draftService.takeForScheduling(id);

        try {
            CreateEventRequest createRequest = new CreateEventRequest(
                    draft.title(),
                    draft.description(),
                    draft.location(),
                    draft.startDateTime(),
                    draft.endDateTime(),
                    draft.attendees()
            );

            CalendarEvent event = calendarService.createEvent(
                    client.getAccessToken().getTokenValue(),
                    createRequest
            );

            auditService.log(authentication.getName(), "MEETING_SCHEDULED", "Event: " + draft.title() + " at " + draft.startDateTime(), "SUCCESS");

            return Map.of(
                    "status", "scheduled",
                    "eventId", event.id(),
                    "htmlLink", event.htmlLink() != null ? event.htmlLink() : "",
                    "event", event
            );
        } catch (Exception exception) {
            draftService.restore(draft);
            throw exception;
        }
    }

    public record UpdateDraftRequest(
            String title,
            String description,
            String location,
            String startDateTime,
            String endDateTime,
            List<String> attendees
    ) {
    }
}
