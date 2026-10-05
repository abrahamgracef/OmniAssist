package dev.abrahamgracef.omniassist.user;

import dev.abrahamgracef.omniassist.conversation.ConversationService;
import dev.abrahamgracef.omniassist.email.EmailTemplateService;
import dev.abrahamgracef.omniassist.knowledge.KnowledgeService;
import dev.abrahamgracef.omniassist.task.TaskPriority;
import dev.abrahamgracef.omniassist.task.TaskService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class DataExportController {

    private final CurrentUserService currentUserService;
    private final TaskService taskService;
    private final KnowledgeService knowledgeService;
    private final EmailTemplateService emailTemplateService;
    private final ConversationService conversationService;

    public DataExportController(
            CurrentUserService currentUserService,
            TaskService taskService,
            KnowledgeService knowledgeService,
            EmailTemplateService emailTemplateService,
            ConversationService conversationService) {
        this.currentUserService = currentUserService;
        this.taskService = taskService;
        this.knowledgeService = knowledgeService;
        this.emailTemplateService = emailTemplateService;
        this.conversationService = conversationService;
    }

    @GetMapping("/export")
    public Map<String, Object> exportUserData(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("profile", UserSettingsController.UserSettingsDto.from(user));
        data.put("tasks", taskService.getTasks(user, null, null));
        data.put("knowledgeNotes", knowledgeService.getNotes(user));
        data.put("emailTemplates", emailTemplateService.getTemplates(user));
        data.put("conversations", conversationService.getUserConversations(user));

        return data;
    }

    @PostMapping("/import")
    public Map<String, Object> importUserData(
            Authentication authentication,
            @RequestBody Map<String, Object> payload) {

        User user = currentUserService.getCurrentUser(authentication);
        int tasksImported = 0;
        int notesImported = 0;

        if (payload.containsKey("tasks") && payload.get("tasks") instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> item) {
                    String title = (String) item.get("title");
                    String desc = (String) item.get("description");
                    String category = (String) item.get("category");
                    if (title != null && !title.isBlank()) {
                        taskService.createTask(user, title, desc, null, TaskPriority.MEDIUM, category, null);
                        tasksImported++;
                    }
                }
            }
        }

        if (payload.containsKey("notes") && payload.get("notes") instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> item) {
                    String title = (String) item.get("title");
                    String content = (String) item.get("content");
                    String tags = (String) item.get("tags");
                    if (title != null && !title.isBlank()) {
                        knowledgeService.createNote(user, title, content != null ? content : "", tags, "NOTE");
                        notesImported++;
                    }
                }
            }
        }

        return Map.of(
                "status", "success",
                "tasksImported", tasksImported,
                "notesImported", notesImported
        );
    }
}
