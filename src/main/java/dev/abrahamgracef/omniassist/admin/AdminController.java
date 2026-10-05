package dev.abrahamgracef.omniassist.admin;

import dev.abrahamgracef.omniassist.user.CurrentUserService;
import dev.abrahamgracef.omniassist.user.User;
import dev.abrahamgracef.omniassist.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuditService auditService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public AdminController(AuditService auditService, UserRepository userRepository, CurrentUserService currentUserService) {
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    private boolean checkAdminOrDev(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        // Allow demo user or users with ROLE_ADMIN
        return "ROLE_ADMIN".equalsIgnoreCase(user.getRole()) ||
                CurrentUserService.DEMO_EMAIL.equalsIgnoreCase(user.getEmail());
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics(Authentication authentication) {
        if (!checkAdminOrDev(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(auditService.getSystemMetrics());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(Authentication authentication) {
        if (!checkAdminOrDev(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(auditService.getRecentLogs());
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(Authentication authentication) {
        if (!checkAdminOrDev(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(userRepository.findAll());
    }
}
