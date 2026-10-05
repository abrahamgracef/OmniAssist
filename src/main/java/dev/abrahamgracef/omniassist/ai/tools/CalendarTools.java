package dev.abrahamgracef.omniassist.ai.tools;

import dev.abrahamgracef.omniassist.calendar.MeetingDraft;
import dev.abrahamgracef.omniassist.calendar.MeetingDraftService;
import dev.abrahamgracef.omniassist.google.calendar.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CalendarTools {

    private final GoogleCalendarService calendarService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final MeetingDraftService meetingDraftService;
    private final dev.abrahamgracef.omniassist.task.TaskService taskService;
    private final dev.abrahamgracef.omniassist.user.CurrentUserService currentUserService;

    public CalendarTools(
            GoogleCalendarService calendarService,
            OAuth2AuthorizedClientService authorizedClientService,
            MeetingDraftService meetingDraftService,
            dev.abrahamgracef.omniassist.task.TaskService taskService,
            dev.abrahamgracef.omniassist.user.CurrentUserService currentUserService) {

        this.calendarService = calendarService;
        this.authorizedClientService = authorizedClientService;
        this.meetingDraftService = meetingDraftService;
        this.taskService = taskService;
        this.currentUserService = currentUserService;
    }

    private String getAccessToken() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {

            throw new IllegalStateException(
                    "Google account is not connected. Please sign in with Google."
            );
        }

        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient(
                        "google",
                        authentication.getName()
                );

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException(
                    "Google account is not connected. Please sign in with Google."
            );
        }

        return client.getAccessToken().getTokenValue();
    }

    @Tool(description = """
        Retrieve upcoming calendar events and meetings.

        Call this tool when the current user message asks to view, check, list,
        summarize, or inspect upcoming meetings, schedule, or calendar events.

        Parameters:
        - maxResults: Maximum number of events to retrieve (e.g., 5, 10).
        - daysAhead: Number of days into the future to look (e.g., 1 for today/tomorrow, 7 for a week).
        """)
    public List<CalendarEvent> listUpcomingEvents(int maxResults, Integer daysAhead) {
        int limit = maxResults > 0 ? maxResults : 10;
        int days = (daysAhead != null && daysAhead > 0) ? daysAhead : 7;
        return calendarService.getUpcomingEvents(getAccessToken(), limit, days);
    }

    @Tool(description = """
        Create a meeting draft for review.

        Call this tool when the current user message asks to schedule, book, set up, or draft a meeting.
        The created draft will be displayed as an interactive card in the UI for the user to review, edit, or confirm.

        Parameters:
        - title: Summary or title of the meeting.
        - startDateTime: Meeting start ISO date-time (e.g., '2026-10-06T10:00:00+05:30' or '2026-10-06T10:00:00Z').
        - endDateTime: Meeting end ISO date-time (e.g., '2026-10-06T11:00:00+05:30' or '2026-10-06T11:00:00Z').
        - description: Agenda, notes, or details for the meeting.
        - location: Location or 'Google Meet'.
        - attendees: List of attendee email addresses.
        """)
    public MeetingDraft createMeetingDraft(
            String title,
            String startDateTime,
            String endDateTime,
            String description,
            String location,
            List<String> attendees) {

        return meetingDraftService.create(
                title,
                description,
                location,
                startDateTime,
                endDateTime,
                attendees
        );
    }

    @Tool(description = """
        Directly schedule and create a meeting event in Google Calendar without a draft.

        Call this tool only when the user explicitly instructs to schedule or add the event immediately without drafting.

        Parameters:
        - title: Summary or title of the meeting.
        - startDateTime: ISO date-time string.
        - endDateTime: ISO date-time string.
        - description: Notes or agenda.
        - location: Location or meeting link.
        - attendees: List of attendee email addresses.
        """)
    public CalendarEvent createCalendarEvent(
            String title,
            String startDateTime,
            String endDateTime,
            String description,
            String location,
            List<String> attendees) {

        CreateEventRequest request = new CreateEventRequest(
                title,
                description,
                location,
                startDateTime,
                endDateTime,
                attendees
        );

        return calendarService.createEvent(getAccessToken(), request);
    }

    @Tool(description = """
        Suggest available free time slots for a given date.

        Call this tool when the user asks when they are free, checks availability,
        or asks for open slots or meeting times on a particular date.

        Parameters:
        - date: Target date in YYYY-MM-DD format (or 'today', 'tomorrow').
        - durationMinutes: Desired meeting length in minutes (e.g., 30, 45, 60).
        - startHour: Beginning of working hours (0-23, default 9 for 9:00 AM).
        - endHour: End of working hours (0-23, default 17 for 5:00 PM).
        """)
    public List<TimeSlot> suggestFreeTimeSlots(
            String date,
            int durationMinutes,
            Integer startHour,
            Integer endHour) {

        int duration = durationMinutes > 0 ? durationMinutes : 30;
        return calendarService.findFreeSlots(
                getAccessToken(),
                date,
                duration,
                startHour,
                endHour
        );
    }

    @Tool(description = """
        Update or reschedule an existing calendar event.

        Call this tool when the user asks to modify, reschedule, move, or update an existing event.
        If the eventId is not specified by the user, first call listUpcomingEvents to find the matching event ID.

        Parameters:
        - eventId: The Google Calendar event ID.
        - title: New or updated title (pass null to keep unchanged).
        - startDateTime: New start date-time (pass null to keep unchanged).
        - endDateTime: New end date-time (pass null to keep unchanged).
        - description: New description (pass null to keep unchanged).
        - location: New location (pass null to keep unchanged).
        - attendees: Updated attendee email list (pass null to keep unchanged).
        """)
    public CalendarEvent updateCalendarEvent(
            String eventId,
            String title,
            String startDateTime,
            String endDateTime,
            String description,
            String location,
            List<String> attendees) {

        UpdateEventRequest request = new UpdateEventRequest(
                title,
                description,
                location,
                startDateTime,
                endDateTime,
                attendees
        );

        return calendarService.updateEvent(getAccessToken(), eventId, request);
    }

    @Tool(description = """
        Delete or cancel an existing calendar event by its event ID.

        Call this tool when the user asks to delete, cancel, or remove a meeting.
        If the eventId is not specified by the user, first call listUpcomingEvents to find the matching event ID.

        Parameters:
        - eventId: The Google Calendar event ID to delete.
        """)
    public String deleteCalendarEvent(String eventId) {
        calendarService.deleteEvent(getAccessToken(), eventId);
        return "Successfully deleted calendar event with ID: " + eventId;
    }

    @Tool(description = """
        Check if a proposed meeting time conflicts with any existing events on the user's Google Calendar.

        Parameters:
        - startDateTime: ISO-8601 start time (e.g., '2026-10-06T10:00:00+05:30' or '2026-10-06T10:00:00Z').
        - endDateTime: ISO-8601 end time (e.g., '2026-10-06T10:30:00+05:30' or '2026-10-06T10:30:00Z').
        """)
    public String checkScheduleConflicts(String startDateTime, String endDateTime) {
        List<CalendarEvent> conflicts = calendarService.findConflicts(getAccessToken(), startDateTime, endDateTime);
        if (conflicts.isEmpty()) {
            return "No conflicts detected for the specified time slot!";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Warning: Found ").append(conflicts.size()).append(" conflicting event(s):\n");
        for (CalendarEvent e : conflicts) {
            sb.append("- \"").append(e.summary()).append("\" (").append(e.start()).append(" to ").append(e.end()).append(")\n");
        }
        return sb.toString();
    }

    @Tool(description = """
        Automatically block dedicated focus time on Google Calendar for the user's pending daily tasks.
        Finds open schedule gaps between meetings and reserves focus blocks.

        Parameters:
        - date: ISO date string (e.g., '2026-10-06') or 'today'.
        """)
    public String timeBlockPendingTasks(String date) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        dev.abrahamgracef.omniassist.user.User user = currentUserService.getCurrentUser(auth);

        java.time.LocalDate targetDate = java.time.LocalDate.now();
        if (date != null && !date.isBlank() && !"today".equalsIgnoreCase(date.trim())) {
            try {
                targetDate = java.time.LocalDate.parse(date.trim());
            } catch (Exception ignored) {
            }
        }

        List<dev.abrahamgracef.omniassist.task.Task> tasks = taskService.getDailyPlannerTasks(user, targetDate);
        if (tasks.isEmpty()) {
            return "No pending tasks found to schedule for " + targetDate + ".";
        }

        List<CalendarEvent> scheduled = calendarService.autoBlockFocusTime(getAccessToken(), tasks, targetDate, 45);
        if (scheduled.isEmpty()) {
            return "Could not find open time slots between your existing meetings to schedule focus blocks.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Successfully auto-blocked ").append(scheduled.size()).append(" focus session(s) on your calendar:\n");
        for (CalendarEvent e : scheduled) {
            sb.append("• \"").append(e.summary()).append("\" (").append(e.start()).append(" to ").append(e.end()).append(")\n");
        }
        return sb.toString();
    }
}
