package dev.abrahamgracef.omniassist.ai;

import dev.abrahamgracef.omniassist.ai.memory.MemoryService;
import dev.abrahamgracef.omniassist.ai.tools.CalendarTools;
import dev.abrahamgracef.omniassist.ai.tools.GmailTools;
import dev.abrahamgracef.omniassist.ai.tools.KnowledgeTools;
import dev.abrahamgracef.omniassist.ai.tools.TaskTools;
import dev.abrahamgracef.omniassist.conversation.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AssistantService {

    private final ChatClient chatClient;
    private final ConversationService conversationService;
    private final GmailTools gmailTools;
    private final CalendarTools calendarTools;
    private final TaskTools taskTools;
    private final KnowledgeTools knowledgeTools;
    private final dev.abrahamgracef.omniassist.ai.tools.BriefingTools briefingTools;
    private final MemoryService memoryService;

    public AssistantService(
            ChatClient.Builder builder,
            ConversationService conversationService,
            GmailTools gmailTools,
            CalendarTools calendarTools,
            TaskTools taskTools,
            KnowledgeTools knowledgeTools,
            dev.abrahamgracef.omniassist.ai.tools.BriefingTools briefingTools,
            MemoryService memoryService) {

        this.chatClient = builder.build();
        this.conversationService = conversationService;
        this.gmailTools = gmailTools;
        this.calendarTools = calendarTools;
        this.taskTools = taskTools;
        this.knowledgeTools = knowledgeTools;
        this.briefingTools = briefingTools;
        this.memoryService = memoryService;
    }

    public String chat(UUID conversationId, String userInput) {

        Conversation conversation =
                conversationService.getConversation(conversationId);

        // Auto-update conversation title on first message if default
        if ("New Chat".equalsIgnoreCase(conversation.getTitle()) && userInput != null && !userInput.isBlank()) {
            String shortTitle = userInput.trim();
            if (shortTitle.length() > 32) {
                shortTitle = shortTitle.substring(0, 29) + "...";
            }
            conversationService.updateTitle(conversationId, shortTitle);
        }

        // Save current user message.
        conversationService.saveMessage(
                conversation,
                MessageRole.USER,
                userInput
        );

        // Retrieve complete history.
        List<Message> history =
                conversationService.getHistory(conversationId);

        List<org.springframework.ai.chat.messages.Message> aiMessages =
                new ArrayList<>();

        // Add dynamic system prompt with current date/time context and user memories
        String systemPrompt = buildSystemPrompt(conversation.getUser());
        aiMessages.add(new SystemMessage(systemPrompt));

        for (Message message : history) {

            switch (message.getRole()) {

                case USER ->
                        aiMessages.add(
                                new UserMessage(message.getContent())
                        );

                case ASSISTANT ->
                        aiMessages.add(
                                new AssistantMessage(message.getContent())
                        );

                default -> {
                    // Other message roles aren't needed in chat history.
                }
            }
        }

        String response = chatClient
                .prompt(new Prompt(aiMessages))
                .tools(gmailTools, calendarTools, taskTools, knowledgeTools, briefingTools)
                .call()
                .content();

        // Save AI response.
        conversationService.saveMessage(
                conversation,
                MessageRole.ASSISTANT,
                response
        );

        return response;
    }

    public List<String> generateSuggestionChips(String userInput, String response) {
        String lowerInput = (userInput != null ? userInput : "").toLowerCase();
        String lowerResp = (response != null ? response : "").toLowerCase();

        if (lowerInput.contains("schedule") || lowerInput.contains("meeting") || lowerResp.contains("meeting") || lowerResp.contains("calendar")) {
            return List.of(
                    "Find free slots tomorrow",
                    "What else is on my schedule this week?",
                    "Check my pending tasks"
            );
        } else if (lowerInput.contains("task") || lowerInput.contains("to-do") || lowerInput.contains("planner") || lowerResp.contains("task")) {
            return List.of(
                    "Show my daily planner",
                    "Add a high-priority task",
                    "What meetings do I have today?"
            );
        } else if (lowerInput.contains("email") || lowerInput.contains("draft") || lowerResp.contains("draft") || lowerResp.contains("gmail")) {
            return List.of(
                    "Check my latest unread emails",
                    "Show my sent emails",
                    "Create a meeting follow-up draft"
            );
        } else if (lowerInput.contains("note") || lowerInput.contains("document") || lowerInput.contains("knowledge")) {
            return List.of(
                    "Search notes for project updates",
                    "Save this summary as a note",
                    "Show my daily planner"
            );
        }

        return List.of(
                "What's on my schedule today?",
                "Show my daily planner and tasks",
                "Check latest unread emails",
                "Find 30m free slots tomorrow"
        );
    }

    private String buildSystemPrompt(dev.abrahamgracef.omniassist.user.User user) {
        String currentDateTime = ZonedDateTime.now(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy HH:mm:ss (z)"));

        String memoriesBlock = user != null ? memoryService.formatMemoriesForPrompt(user) : "";

        return """
    You are OmniAssist, an intelligent AI productivity suite assistant.

    You can orchestrate across Gmail, Google Calendar, Personal Tasks & Daily Planner, and the internal Knowledge Base.

    CURRENT SYSTEM DATE & TIME: %s%s

    IMPORTANT TOOL RULES:
    - Never call a tool just because a previous message involved that tool.
    - Decide whether to call a tool based primarily on the CURRENT user request.
    - If the user asks multi-part instructions (e.g., 'check my schedule and create a task for slides'), decompose the task and call each relevant tool.
    - If the current message is casual conversation or a general question, answer normally without calling tools.

    GMAIL:
    - Use getLatestEmails ONLY when asked to read/view emails.
    - Use getSentEmails when asked about sent items.
    - Use createEmailDraft ONLY when asked to compose or draft an email.

    GOOGLE CALENDAR & SMART SCHEDULING:
    - Compute relative dates ('today', 'tomorrow', 'next Monday') relative to CURRENT SYSTEM DATE & TIME.
    - Prefer calling createMeetingDraft so user can visually inspect the interactive card.
    - Call checkScheduleConflicts to check if proposed times conflict with existing meetings.
    - Use suggestFreeTimeSlots when user asks for open or available times.

    TASKS & DAILY PLANNER:
    - Use createTask when user asks to add, create, or remember a task or to-do.
    - Use listTasks to retrieve pending or completed tasks.
    - Use completeTask to mark tasks done.
    - Use getDailyPlanner when user asks for their daily plan, agenda, or today's priorities.

    KNOWLEDGE BASE & NOTES:
    - Use searchKnowledgeBase when user asks about saved notes, facts, or uploaded documentation.
    - Use saveNote when user asks to save a note, record instructions, or store knowledge.

    Never claim an external action occurred unless the corresponding tool successfully completed it.
    """.formatted(currentDateTime, memoriesBlock);
    }
}