package dev.abrahamgracef.omniassist.notification;

import dev.abrahamgracef.omniassist.user.CurrentUserService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService, CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<NotificationDto> getNotifications(Authentication authentication,
                                                  @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        User user = currentUserService.getCurrentUser(authentication);
        List<Notification> list = unreadOnly ?
                notificationService.getUnreadNotifications(user) :
                notificationService.getAllNotifications(user);

        return list.stream().map(NotificationDto::from).toList();
    }

    @PatchMapping("/{id}/read")
    public Map<String, String> markAsRead(@PathVariable UUID id, Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        notificationService.markAsRead(id, user);
        return Map.of("status", "success");
    }

    @PostMapping("/read-all")
    public Map<String, String> markAllAsRead(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        notificationService.markAllAsRead(user);
        return Map.of("status", "success");
    }

    public record NotificationDto(
            UUID id,
            String title,
            String message,
            String type,
            String actionUrl,
            boolean read,
            LocalDateTime createdAt
    ) {
        public static NotificationDto from(Notification n) {
            return new NotificationDto(
                    n.getId(),
                    n.getTitle(),
                    n.getMessage(),
                    n.getType(),
                    n.getActionUrl(),
                    n.isRead(),
                    n.getCreatedAt()
            );
        }
    }
}
