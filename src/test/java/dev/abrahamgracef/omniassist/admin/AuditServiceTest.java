package dev.abrahamgracef.omniassist.admin;

import dev.abrahamgracef.omniassist.conversation.ConversationRepository;
import dev.abrahamgracef.omniassist.knowledge.DocumentNoteRepository;
import dev.abrahamgracef.omniassist.task.TaskRepository;
import dev.abrahamgracef.omniassist.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DocumentNoteRepository noteRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository, userRepository, conversationRepository, taskRepository, noteRepository);
    }

    @Test
    void testLog() {
        auditService.log("user@omniassist.local", "EMAIL_SENT", "Sent email to Bob", "SUCCESS");
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testGetSystemMetrics() {
        when(userRepository.count()).thenReturn(5L);
        when(conversationRepository.count()).thenReturn(12L);
        when(taskRepository.count()).thenReturn(30L);
        when(noteRepository.count()).thenReturn(8L);
        when(auditLogRepository.count()).thenReturn(50L);
        when(auditLogRepository.countByAction("EMAIL_SENT")).thenReturn(15L);

        Map<String, Object> metrics = auditService.getSystemMetrics();
        assertEquals(5L, metrics.get("totalUsers"));
        assertEquals(12L, metrics.get("totalConversations"));
        assertEquals(30L, metrics.get("totalTasks"));
        assertEquals(8L, metrics.get("totalKnowledgeDocuments"));
        assertEquals(50L, metrics.get("totalAuditEvents"));

        Map<?, ?> breakdown = (Map<?, ?>) metrics.get("actionBreakdown");
        assertEquals(15L, breakdown.get("EMAIL_SENT"));
    }
}
