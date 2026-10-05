package dev.abrahamgracef.omniassist.briefing;

import dev.abrahamgracef.omniassist.google.calendar.GoogleCalendarService;
import dev.abrahamgracef.omniassist.notification.NotificationService;
import dev.abrahamgracef.omniassist.task.Task;
import dev.abrahamgracef.omniassist.task.TaskPriority;
import dev.abrahamgracef.omniassist.task.TaskService;
import dev.abrahamgracef.omniassist.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BriefingServiceTest {

    @Mock
    private TaskService taskService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private GoogleCalendarService calendarService;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    private BriefingService briefingService;
    private User testUser;

    @BeforeEach
    public void setUp() {
        briefingService = new BriefingService(
                taskService,
                notificationService,
                calendarService,
                authorizedClientService,
                chatClientBuilder
        );

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("executive@omniassist.local");
        testUser.setDisplayName("Alex Mercer");
    }

    @Test
    void testGetTodayBriefingCompilesTasksAndSummary() {
        Task t1 = new Task();
        t1.setId(UUID.randomUUID());
        t1.setTitle("Approve Q4 Budget");
        t1.setPriority(TaskPriority.URGENT);

        Task t2 = new Task();
        t2.setId(UUID.randomUUID());
        t2.setTitle("Sync with Engineering Leads");
        t2.setPriority(TaskPriority.HIGH);

        when(taskService.getDailyPlannerTasks(eq(testUser), any(LocalDate.class)))
                .thenReturn(List.of(t1, t2));

        when(chatClientBuilder.build().prompt(any(Prompt.class)).call().content())
                .thenReturn("Good morning Alex! Start your day by approving the Q4 budget.");

        BriefingService.DailyBriefing briefing = briefingService.getTodayBriefing(testUser, null);

        assertNotNull(briefing);
        assertEquals("Alex Mercer", briefing.userName());
        assertEquals(2, briefing.taskCount());
        assertEquals(2, briefing.urgentTaskCount());
        assertTrue(briefing.executiveSummary().contains("budget") || briefing.executiveSummary().contains("Good morning"));
    }

    @Test
    void testPushBriefingNotificationCreatesReminder() {
        when(taskService.getDailyPlannerTasks(eq(testUser), any(LocalDate.class)))
                .thenReturn(List.of());

        briefingService.pushBriefingNotification(testUser, null);

        verify(notificationService, times(1)).createNotification(
                eq(testUser),
                contains("Morning Executive Briefing"),
                anyString(),
                eq("REMINDER"),
                anyString()
        );
    }
}
