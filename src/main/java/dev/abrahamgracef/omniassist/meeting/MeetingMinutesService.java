package dev.abrahamgracef.omniassist.meeting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abrahamgracef.omniassist.email.EmailDraft;
import dev.abrahamgracef.omniassist.email.EmailDraftService;
import dev.abrahamgracef.omniassist.task.Task;
import dev.abrahamgracef.omniassist.task.TaskPriority;
import dev.abrahamgracef.omniassist.task.TaskService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MeetingMinutesService {

    private final TaskService taskService;
    private final EmailDraftService emailDraftService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public MeetingMinutesService(
            TaskService taskService,
            EmailDraftService emailDraftService,
            ChatClient.Builder chatClientBuilder) {
        this.taskService = taskService;
        this.emailDraftService = emailDraftService;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    public ProcessedMinutesResult processNotes(User user, String notes, boolean createTasks, boolean createEmailDraft) {
        String promptText = """
            You are an expert executive meeting assistant. Analyze the following meeting notes or transcript:
            \"\"\"
            %s
            \"\"\"

            Extract and return ONLY a valid JSON object matching this schema (no markdown formatting, no backticks):
            {
              "summary": "2-3 sentence executive recap of the meeting",
              "decisions": ["decision 1", "decision 2"],
              "actionItems": [
                {
                  "title": "Clear action item title",
                  "assignee": "Name or You",
                  "priority": "HIGH or MEDIUM or LOW"
                }
              ]
            }
            """.formatted(notes);

        String jsonResponse = chatClient.prompt(new Prompt(promptText)).call().content();

        String summary = "Meeting recap processed.";
        List<String> decisions = new ArrayList<>();
        List<ActionItemDto> actionItems = new ArrayList<>();
        List<Task> createdTasks = new ArrayList<>();
        EmailDraft createdEmailDraft = null;

        try {
            String cleanJson = jsonResponse != null ? jsonResponse.trim() : "{}";
            if (cleanJson.startsWith("```")) {
                int firstBrace = cleanJson.indexOf('{');
                int lastBrace = cleanJson.lastIndexOf('}');
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    cleanJson = cleanJson.substring(firstBrace, lastBrace + 1);
                }
            }

            JsonNode root = objectMapper.readTree(cleanJson);
            if (root.has("summary")) {
                summary = root.get("summary").asText();
            }
            if (root.has("decisions") && root.get("decisions").isArray()) {
                for (JsonNode d : root.get("decisions")) {
                    decisions.add(d.asText());
                }
            }
            if (root.has("actionItems") && root.get("actionItems").isArray()) {
                for (JsonNode item : root.get("actionItems")) {
                    String title = item.has("title") ? item.get("title").asText() : "Action item";
                    String assignee = item.has("assignee") ? item.get("assignee").asText() : "Team";
                    String priorityStr = item.has("priority") ? item.get("priority").asText() : "MEDIUM";
                    TaskPriority priority = TaskPriority.MEDIUM;
                    try {
                        priority = TaskPriority.valueOf(priorityStr.toUpperCase());
                    } catch (Exception ignored) {
                    }

                    actionItems.add(new ActionItemDto(title, assignee, priority.name()));

                    if (createTasks) {
                        Task t = taskService.createTask(
                                user,
                                title + (assignee != null && !assignee.equalsIgnoreCase("You") ? " (" + assignee + ")" : ""),
                                "Action item generated from meeting minutes",
                                LocalDateTime.now().plusDays(2),
                                priority,
                                "Meeting Action",
                                null
                        );
                        createdTasks.add(t);
                    }
                }
            }
        } catch (Exception e) {
            summary = "Summary: " + (jsonResponse != null ? jsonResponse : "Processed notes.");
        }

        if (createEmailDraft) {
            StringBuilder body = new StringBuilder();
            body.append("Hi Team,\n\nHere is a quick recap of our meeting today:\n\n");
            body.append("Summary:\n").append(summary).append("\n\n");
            if (!decisions.isEmpty()) {
                body.append("Key Decisions:\n");
                for (String d : decisions) body.append("- ").append(d).append("\n");
                body.append("\n");
            }
            if (!actionItems.isEmpty()) {
                body.append("Action Items:\n");
                for (ActionItemDto a : actionItems) {
                    body.append("- ").append(a.title()).append(" [Owner: ").append(a.assignee()).append("]\n");
                }
                body.append("\n");
            }
            body.append("Best regards,\n").append(user.getDisplayName());

            createdEmailDraft = emailDraftService.create(
                    "team@example.com",
                    "Meeting Recap & Action Items",
                    body.toString()
            );
        }

        return new ProcessedMinutesResult(
                summary,
                decisions,
                actionItems,
                createdTasks.size(),
                createdEmailDraft != null ? createdEmailDraft.id().toString() : null
        );
    }

    public record ActionItemDto(String title, String assignee, String priority) {}

    public record ProcessedMinutesResult(
            String summary,
            List<String> decisions,
            List<ActionItemDto> actionItems,
            int tasksCreatedCount,
            String emailDraftId
    ) {}
}
