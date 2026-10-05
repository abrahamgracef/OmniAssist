package dev.abrahamgracef.omniassist.admin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String action; // EMAIL_SENT, MEETING_SCHEDULED, TASK_CREATED, TASK_COMPLETED, NOTE_SAVED, LOGIN

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private String status = "SUCCESS";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "SUCCESS";
        }
    }
}
