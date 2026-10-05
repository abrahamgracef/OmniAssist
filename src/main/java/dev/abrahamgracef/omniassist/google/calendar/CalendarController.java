package dev.abrahamgracef.omniassist.google.calendar;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final GoogleCalendarService calendarService;
    private final dev.abrahamgracef.omniassist.task.TaskService taskService;
    private final dev.abrahamgracef.omniassist.user.CurrentUserService currentUserService;

    public CalendarController(
            GoogleCalendarService calendarService,
            dev.abrahamgracef.omniassist.task.TaskService taskService,
            dev.abrahamgracef.omniassist.user.CurrentUserService currentUserService) {
        this.calendarService = calendarService;
        this.taskService = taskService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/events")
    public List<CalendarEvent> getUpcomingEvents(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient googleClient,
            @RequestParam(defaultValue = "10") int maxResults,
            @RequestParam(defaultValue = "7") int daysAhead) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        return calendarService.getUpcomingEvents(accessToken, maxResults, daysAhead);
    }

    @GetMapping("/events/{id}")
    public CalendarEvent getEvent(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient googleClient,
            @PathVariable String id) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        return calendarService.getEvent(accessToken, id);
    }

    @PostMapping("/events")
    public CalendarEvent createEvent(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient googleClient,
            @RequestBody CreateEventRequest request) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        return calendarService.createEvent(accessToken, request);
    }

    @PatchMapping("/events/{id}")
    public CalendarEvent updateEvent(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient googleClient,
            @PathVariable String id,
            @RequestBody UpdateEventRequest request) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        return calendarService.updateEvent(accessToken, id, request);
    }

    @PutMapping("/events/{id}")
    public CalendarEvent replaceEvent(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient googleClient,
            @PathVariable String id,
            @RequestBody UpdateEventRequest request) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        return calendarService.updateEvent(accessToken, id, request);
    }

    @DeleteMapping("/events/{id}")
    public Map<String, String> deleteEvent(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient googleClient,
            @PathVariable String id) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        calendarService.deleteEvent(accessToken, id);

        return Map.of(
                "status", "deleted",
                "eventId", id
        );
    }

    @GetMapping("/free-slots")
    public List<TimeSlot> getFreeSlots(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient googleClient,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "30") int durationMinutes,
            @RequestParam(required = false) Integer startHour,
            @RequestParam(required = false) Integer endHour) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        return calendarService.findFreeSlots(accessToken, date, durationMinutes, startHour, endHour);
    }

    @GetMapping("/summary")
    public Map<String, Object> summarizeUpcoming(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient googleClient,
            @RequestParam(defaultValue = "7") int daysAhead) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        List<CalendarEvent> events = calendarService.getUpcomingEvents(accessToken, 20, daysAhead);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("daysAhead", daysAhead);
        summary.put("totalEvents", events.size());
        summary.put("events", events);

        if (!events.isEmpty()) {
            summary.put("nextMeeting", events.get(0));
        }

        return summary;
    }

    @GetMapping("/conflicts")
    public List<CalendarEvent> checkConflicts(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient googleClient,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        return calendarService.findConflicts(accessToken, startDateTime, endDateTime);
    }

    @PostMapping("/auto-block")
    public List<CalendarEvent> autoBlock(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient googleClient,
            org.springframework.security.core.Authentication authentication,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "45") int durationMinutes) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        dev.abrahamgracef.omniassist.user.User user = currentUserService.getCurrentUser(authentication);
        java.time.LocalDate targetDate = date != null && !date.isBlank()
                ? java.time.LocalDate.parse(date.trim())
                : java.time.LocalDate.now();

        java.util.List<dev.abrahamgracef.omniassist.task.Task> pendingTasks =
                taskService.getDailyPlannerTasks(user, targetDate);

        return calendarService.autoBlockFocusTime(accessToken, pendingTasks, targetDate, durationMinutes);
    }
}
