package dev.abrahamgracef.omniassist.conversation;

import dev.abrahamgracef.omniassist.user.User;
import dev.abrahamgracef.omniassist.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository) {

        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Conversation createConversation() {

        // Temporary single user for MVP.
        // We'll replace this when authentication is added.
        User user = userRepository.findByEmail("demo@omniassist.local")
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setDisplayName("Demo User");
                    newUser.setEmail("demo@omniassist.local");
                    return userRepository.save(newUser);
                });

        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle("New Chat");

        return conversationRepository.save(conversation);
    }

    public Conversation getConversation(UUID id) {
        return conversationRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Conversation not found"));
    }

    @Transactional
    public void saveMessage(
            Conversation conversation,
            MessageRole role,
            String content) {

        Message message = new Message();

        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);

        messageRepository.save(message);
    }

    public List<Message> getHistory(UUID conversationId) {
        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
}