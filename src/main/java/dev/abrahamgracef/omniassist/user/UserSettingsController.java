package dev.abrahamgracef.omniassist.user;

import dev.abrahamgracef.omniassist.workspace.Workspace;
import dev.abrahamgracef.omniassist.workspace.WorkspaceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserSettingsController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;

    public UserSettingsController(CurrentUserService currentUserService,
                                  UserRepository userRepository,
                                  WorkspaceService workspaceService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.workspaceService = workspaceService;
    }

    @GetMapping("/settings")
    public UserSettingsDto getSettings(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        return UserSettingsDto.from(user);
    }

    @PutMapping("/settings")
    public UserSettingsDto updateSettings(@RequestBody UpdateSettingsRequest request,
                                          Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);

        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.theme() != null && !request.theme().isBlank()) {
            user.setTheme(request.theme().trim().toLowerCase());
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            user.setTimezone(request.timezone().trim());
        }
        if (request.workStartHour() >= 0 && request.workStartHour() <= 23) {
            user.setWorkStartHour(request.workStartHour());
        }
        if (request.workEndHour() >= 0 && request.workEndHour() <= 23) {
            user.setWorkEndHour(request.workEndHour());
        }

        user = userRepository.save(user);
        return UserSettingsDto.from(user);
    }

    @GetMapping("/workspaces")
    public List<WorkspaceDto> getWorkspaces(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        return workspaceService.getWorkspaces(user).stream()
                .map(WorkspaceDto::from)
                .toList();
    }

    @PostMapping("/workspaces")
    public WorkspaceDto createWorkspace(@RequestBody CreateWorkspaceRequest request,
                                        Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        Workspace ws = workspaceService.createWorkspace(user, request.name(), request.type());
        return WorkspaceDto.from(ws);
    }

    @DeleteMapping("/workspaces/{id}")
    public Map<String, String> deleteWorkspace(@PathVariable UUID id,
                                               Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        workspaceService.deleteWorkspace(id, user);
        return Map.of("status", "success");
    }

    public record UpdateSettingsRequest(
            String displayName,
            String theme,
            String timezone,
            int workStartHour,
            int workEndHour
    ) {}

    public record UserSettingsDto(
            UUID id,
            String displayName,
            String email,
            String role,
            String avatarUrl,
            String timezone,
            String theme,
            int workStartHour,
            int workEndHour
    ) {
        public static UserSettingsDto from(User u) {
            return new UserSettingsDto(
                    u.getId(),
                    u.getDisplayName(),
                    u.getEmail(),
                    u.getRole(),
                    u.getAvatarUrl(),
                    u.getTimezone(),
                    u.getTheme(),
                    u.getWorkStartHour(),
                    u.getWorkEndHour()
            );
        }
    }

    public record CreateWorkspaceRequest(String name, String type) {}

    public record WorkspaceDto(UUID id, String name, String type) {
        public static WorkspaceDto from(Workspace ws) {
            return new WorkspaceDto(ws.getId(), ws.getName(), ws.getType());
        }
    }
}
