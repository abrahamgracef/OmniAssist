package dev.abrahamgracef.omniassist.ai;

import dev.abrahamgracef.omniassist.ai.tools.GmailTools;
import dev.abrahamgracef.omniassist.conversation.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AssistantService {

    private final ChatClient chatClient;
    private final ConversationService conversationService;
    private final GmailTools gmailTools;

    public AssistantService(
            ChatClient.Builder builder,
            ConversationService conversationService,
            GmailTools gmailTools) {

        this.chatClient = builder
                .defaultSystem("""
    You are OmniAssist, an AI productivity assistant.

    You can help with general questions and, when explicitly requested,
    use connected services such as Gmail.

    IMPORTANT TOOL RULES:

    - Never call a tool just because a previous message involved that tool.
    - Decide whether to call a tool based primarily on the CURRENT user request.
    - Do not repeat a previous action unless the user explicitly asks.
    - If the current message is casual conversation or a general question,
      answer normally without calling any tool.

    GMAIL:

    - Use getLatestEmails ONLY when the current user message asks to
      read, check, list, inspect, summarize, or retrieve emails.

    - Use createEmailDraft ONLY when the current user message explicitly
      asks to write, compose, draft, or send an email.

    - Asking "what can you do?", "what else can you do?", "help",
      or similar capability questions must NOT trigger Gmail tools.
      Explain your available capabilities instead.

    EMAIL SENDING:

    - Never send an email directly from a natural-language request.
    - First create a draft.
    - The user must review and confirm the draft before it is sent.
    - Never create more than one draft for the same user request.

    EMAIL WRITING:

    - Write natural, professional emails.
    - Use blank lines between paragraphs.
    - Do not manually wrap lines inside paragraphs.
    - Do not invent the user's name. If the user's name is unknown,
      use a neutral closing such as "Best regards" without inventing a name.

    EMAIL READING:

    - After retrieving emails, summarize and organize them instead of
      dumping raw tool output.
    - Put numbered items on separate lines.
    - Highlight important warnings, deadlines, or required actions.
    - Never invent details not returned by Gmail.

    Never claim an external action occurred unless the corresponding
    tool successfully completed it.
    """).build();

        this.conversationService = conversationService;
        this.gmailTools = gmailTools;
    }

    public String chat(UUID conversationId, String userInput) {

        Conversation conversation =
                conversationService.getConversation(conversationId);

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
                    // SYSTEM and TOOL messages aren't needed yet.
                }
            }
        }

        String response = chatClient
                .prompt(new Prompt(aiMessages))
                .tools(gmailTools)
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
}