package dev.abrahamgracef.omniassist.briefing;

import dev.abrahamgracef.omniassist.google.calendar.CalendarEvent;
import dev.abrahamgracef.omniassist.google.calendar.GoogleCalendarService;
import dev.abrahamgracef.omniassist.notification.NotificationService;
import dev.abrahamgracef.omniassist.task.Task;
import dev.abrahamgracef.omniassist.task.TaskPriority;
import dev.abrahamgracef.omniassist.task.TaskService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BriefingService {

    private final TaskService taskService;
    private final NotificationService notificationService;
    private final GoogleCalendarService calendarService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ChatClient chatClient;

    public BriefingService(
            TaskService taskService,
            NotificationService notificationService,
            GoogleCalendarService calendarService,
            OAuth2AuthorizedClientService authorizedClientService,
            ChatClient.Builder chatClientBuilder) {
        this.taskService = taskService;
        this.notificationService = notificationService;
        this.calendarService = calendarService;
        this.authorizedClientService = authorizedClientService;
        this.chatClient = chatClientBuilder.build();
    }

    public DailyBriefing getTodayBriefing(User user, Authentication authentication) {
        LocalDate today = LocalDate.now();
        List<Task> pendingTasks = taskService.getDailyPlannerTasks(user, today);
        long urgentCount = pendingTasks.stream()
                .filter(t -> t.getPriority() == TaskPriority.URGENT || t.getPriority() == TaskPriority.HIGH)
                .count();

        List<CalendarEvent> upcomingEvents = new ArrayList<>();
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", authentication.getName());
                if (client != null && client.getAccessToken() != null) {
                    upcomingEvents = calendarService.getUpcomingEvents(client.getAccessToken().getTokenValue(), 10, 1);
                }
            } catch (Exception ignored) {
            }
        }

        // Build brief prompt for Gemini executive synthesis
        StringBuilder promptContext = new StringBuilder();
        promptContext.append("DATE: ").append(today.format(DateTimeFormatter.ISO_DATE)).append("\n");
        promptContext.append("UPCOMING MEETINGS TODAY (").append(upcomingEvents.size()).append("):\n");
        for (CalendarEvent e : upcomingEvents) {
            promptContext.append("- ").append(e.summary()).append(" at ").append(e.start()).append("\n");
        }
        promptContext.append("PENDING TASKS (").append(pendingTasks.size()).append(", Urgent: ").append(urgentCount).append("):\n");
        for (Task t : pendingTasks) {
            promptContext.append("- [").append(t.getPriority()).append("] ").append(t.getTitle()).append("\n");
        }

        String aiSummary = "You have " + upcomingEvents.size() + " meetings and " + pendingTasks.size() + " tasks scheduled for today.";
        try {
            String promptText = """
                You are an executive chief of staff. Provide a concise, motivational 2-3 sentence morning briefing digest
                based on the user's agenda:
                %s

                Highlight their first meeting and the #1 task to tackle first. Keep it snappy and professional.
                """.formatted(promptContext.toString());

            String response = chatClient.prompt(new Prompt(promptText)).call().content();
            if (response != null && !response.isBlank()) {
                aiSummary = response.trim();
            }
        } catch (Exception ignored) {
        }

        return new DailyBriefing(
                today.toString(),
                user.getDisplayName(),
                aiSummary,
                upcomingEvents.size(),
                pendingTasks.size(),
                urgentCount,
                upcomingEvents,
                pendingTasks
        );
    }

    public void pushBriefingNotification(User user, Authentication authentication) {
        DailyBriefing briefing = getTodayBriefing(user, authentication);
        notificationService.createNotification(
                user,
                "🌅 Morning Executive Briefing",
                briefing.executiveSummary(),
                "REMINDER",
                "/#tasks"
        );
    }

    public record DailyBriefing(
            String date,
            String userName,
            String executiveSummary,
            int meetingCount,
            int taskCount,
            long urgentTaskCount,
            List<CalendarEvent> todayMeetings,
            List<Task> todayTasks
    ) {}
}
