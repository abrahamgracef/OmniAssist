package dev.abrahamgracef.omniassist.admin;

import dev.abrahamgracef.omniassist.conversation.ConversationRepository;
import dev.abrahamgracef.omniassist.knowledge.DocumentNoteRepository;
import dev.abrahamgracef.omniassist.task.TaskRepository;
import dev.abrahamgracef.omniassist.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final TaskRepository taskRepository;
    private final DocumentNoteRepository noteRepository;

    public AuditService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            TaskRepository taskRepository,
            DocumentNoteRepository noteRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.taskRepository = taskRepository;
        this.noteRepository = noteRepository;
    }

    @Transactional
    public void log(String userEmail, String action, String details, String status) {
        AuditLog log = new AuditLog();
        log.setUserEmail(userEmail != null ? userEmail : "anonymous");
        log.setAction(action);
        log.setDetails(details);
        log.setStatus(status != null ? status : "SUCCESS");
        auditLogRepository.save(log);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalUsers", userRepository.count());
        metrics.put("totalConversations", conversationRepository.count());
        metrics.put("totalTasks", taskRepository.count());
        metrics.put("totalKnowledgeDocuments", noteRepository.count());
        metrics.put("totalAuditEvents", auditLogRepository.count());

        Map<String, Long> actions = new LinkedHashMap<>();
        actions.put("EMAIL_SENT", auditLogRepository.countByAction("EMAIL_SENT"));
        actions.put("MEETING_SCHEDULED", auditLogRepository.countByAction("MEETING_SCHEDULED"));
        actions.put("TASK_CREATED", auditLogRepository.countByAction("TASK_CREATED"));
        actions.put("TASK_COMPLETED", auditLogRepository.countByAction("TASK_COMPLETED"));
        actions.put("NOTE_SAVED", auditLogRepository.countByAction("NOTE_SAVED"));
        metrics.put("actionBreakdown", actions);

        return metrics;
    }
}
