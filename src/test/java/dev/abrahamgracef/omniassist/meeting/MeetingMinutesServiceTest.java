package dev.abrahamgracef.omniassist.meeting;

import dev.abrahamgracef.omniassist.email.EmailDraft;
import dev.abrahamgracef.omniassist.email.EmailDraftService;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingMinutesServiceTest {

    @Mock
    private TaskService taskService;

    @Mock
    private EmailDraftService emailDraftService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    private MeetingMinutesService meetingMinutesService;
    private User testUser;

    @BeforeEach
    public void setUp() {
        meetingMinutesService = new MeetingMinutesService(
                taskService,
                emailDraftService,
                chatClientBuilder
        );

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("teamlead@omniassist.local");
        testUser.setDisplayName("Sarah Connor");
    }

    @Test
    void testProcessNotesExtractsTasksAndCreatesEmailDraft() {
        String mockAiJson = """
            {
              "summary": "Team agreed to migrate DB to PostgreSQL and finalize sprint scope.",
              "decisions": ["Approved PostgreSQL migration", "Sprint freeze set for Friday"],
              "actionItems": [
                {
                  "title": "Setup PostgreSQL dev cluster",
                  "assignee": "DevOps",
                  "priority": "HIGH"
                },
                {
                  "title": "Review DB migration script",
                  "assignee": "Sarah",
                  "priority": "MEDIUM"
                }
              ]
            }
            """;

        when(chatClientBuilder.build().prompt(any(Prompt.class)).call().content())
                .thenReturn(mockAiJson);

        Task mockTask = new Task();
        mockTask.setId(UUID.randomUUID());
        mockTask.setTitle("Setup PostgreSQL dev cluster");
        mockTask.setPriority(TaskPriority.HIGH);

        when(taskService.createTask(
                eq(testUser),
                anyString(),
                anyString(),
                any(LocalDateTime.class),
                any(TaskPriority.class),
                eq("Meeting Action"),
                isNull()
        )).thenReturn(mockTask);

        UUID draftId = UUID.randomUUID();
        EmailDraft mockDraft = new EmailDraft(draftId, "team@example.com", "Meeting Recap & Action Items", "Body");

        when(emailDraftService.create(anyString(), anyString(), anyString()))
                .thenReturn(mockDraft);

        MeetingMinutesService.ProcessedMinutesResult result = meetingMinutesService.processNotes(
                testUser,
                "Discussion on database and sprint deliverables.",
                true,
                true
        );

        assertNotNull(result);
        assertTrue(result.summary().contains("PostgreSQL"));
        assertEquals(2, result.decisions().size());
        assertEquals(2, result.tasksCreatedCount());
        assertEquals(draftId.toString(), result.emailDraftId());

        verify(taskService, times(2)).createTask(
                eq(testUser),
                anyString(),
                anyString(),
                any(LocalDateTime.class),
                any(TaskPriority.class),
                eq("Meeting Action"),
                isNull()
        );
        verify(emailDraftService, times(1)).create(anyString(), anyString(), anyString());
    }
}
