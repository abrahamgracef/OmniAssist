package dev.abrahamgracef.omniassist.ai.memory;

import dev.abrahamgracef.omniassist.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_memories")
@Getter
@Setter
@NoArgsConstructor
public class UserMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String category = "FACT"; // PREFERENCE, FACT, COLLABORATOR, HABIT

    @Column(nullable = false)
    private String memoryKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String memoryValue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (category == null) {
            category = "FACT";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
