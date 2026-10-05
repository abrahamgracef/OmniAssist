package dev.abrahamgracef.omniassist.web;

import dev.abrahamgracef.omniassist.ai.AssistantService;
import dev.abrahamgracef.omniassist.calendar.MeetingDraft;
import dev.abrahamgracef.omniassist.calendar.MeetingDraftService;
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
    private final MeetingDraftService meetingDraftService;
    private final dev.abrahamgracef.omniassist.user.CurrentUserService currentUserService;

    public ChatApiController(
            AssistantService assistantService,
            ConversationService conversationService,
            EmailDraftService emailDraftService,
            MeetingDraftService meetingDraftService,
            dev.abrahamgracef.omniassist.user.CurrentUserService currentUserService) {

        this.assistantService = assistantService;
        this.conversationService = conversationService;
        this.emailDraftService = emailDraftService;
        this.meetingDraftService = meetingDraftService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/conversations")
    public java.util.List<ConversationResponse> listConversations(org.springframework.security.core.Authentication authentication) {
        dev.abrahamgracef.omniassist.user.User user = currentUserService.getCurrentUser(authentication);
        return conversationService.getUserConversations(user).stream()
                .map(c -> new ConversationResponse(c.getId(), c.getTitle()))
                .toList();
    }

    @PostMapping("/conversations")
    public ConversationResponse createConversation(org.springframework.security.core.Authentication authentication) {
        dev.abrahamgracef.omniassist.user.User user = currentUserService.getCurrentUser(authentication);
        Conversation conversation = conversationService.createConversation(user);

        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle()
        );
    }

    @GetMapping("/conversations/{id}/messages")
    public java.util.List<MessageDto> getConversationMessages(@PathVariable UUID id) {
        return conversationService.getHistory(id).stream()
                .map(m -> new MessageDto(m.getRole().name(), m.getContent(), m.getCreatedAt()))
                .toList();
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

        MeetingDraft meetingDraft =
                meetingDraftService.consumeLatestDraft();

        java.util.List<String> chips = assistantService.generateSuggestionChips(request.message(), response);

        return new ChatResponse(
                response,
                draft,
                meetingDraft,
                chips
        );
    }

    public record ChatRequest(
            UUID conversationId,
            String message) {
    }

    public record ChatResponse(
            String message,
            EmailDraft draft,
            MeetingDraft meetingDraft,
            java.util.List<String> suggestionChips) {
    }

    public record ConversationResponse(
            UUID id,
            String title) {
    }

    public record MessageDto(
            String role,
            String content,
            java.time.LocalDateTime createdAt) {
    }
}