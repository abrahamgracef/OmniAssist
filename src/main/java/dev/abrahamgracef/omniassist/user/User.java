package dev.abrahamgracef.omniassist.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String displayName;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String role = "ROLE_USER";

    private String avatarUrl;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(nullable = false)
    private String theme = "dark";

    @Column(nullable = false)
    private int workStartHour = 9;

    @Column(nullable = false)
    private int workEndHour = 17;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (role == null) {
            role = "ROLE_USER";
        }
        if (timezone == null) {
            timezone = "UTC";
        }
        if (theme == null) {
            theme = "dark";
        }
    }
}