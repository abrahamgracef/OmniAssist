package dev.abrahamgracef.omniassist.web;

import dev.abrahamgracef.omniassist.ai.AssistantService;
import dev.abrahamgracef.omniassist.conversation.Conversation;
import dev.abrahamgracef.omniassist.conversation.ConversationService;
import dev.abrahamgracef.omniassist.email.EmailDraft;
import dev.abrahamgracef.omniassist.email.EmailDraftService;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final AssistantService assistantService;
    private final ConversationService conversationService;
    private final EmailDraftService emailDraftService;


    public ChatApiController(
            AssistantService assistantService,
            ConversationService conversationService,
            EmailDraftService emailDraftService) {

        this.assistantService = assistantService;
        this.conversationService = conversationService;
        this.emailDraftService = emailDraftService;
    }


    @PostMapping("/conversations")
    public ConversationResponse createConversation() {

        Conversation conversation =
                conversationService.createConversation();

        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle()
        );
    }


    @PostMapping
    public ChatResponse chat(
            @RequestBody ChatRequest request) {

        String response = assistantService.chat(
                request.conversationId(),
                request.message()
        );

        EmailDraft draft =
                emailDraftService.consumeLatestDraft();

        return new ChatResponse(
                response,
                draft
        );
    }


    public record ChatRequest(
            UUID conversationId,
            String message) {
    }


    public record ChatResponse(
            String message,
            EmailDraft draft) {
    }


    public record ConversationResponse(
            UUID id,
            String title) {
    }
}