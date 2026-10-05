package dev.abrahamgracef.omniassist.task;

import dev.abrahamgracef.omniassist.user.CurrentUserService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    public TaskController(TaskService taskService, CurrentUserService currentUserService) {
        this.taskService = taskService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<TaskDto> getTasks(
            Authentication authentication,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority) {

        User user = currentUserService.getCurrentUser(authentication);
        return taskService.getTasks(user, status, priority).stream()
                .map(TaskDto::from)
                .toList();
    }

    @GetMapping("/daily-planner")
    public List<TaskDto> getDailyPlanner(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        User user = currentUserService.getCurrentUser(authentication);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return taskService.getDailyPlannerTasks(user, targetDate).stream()
                .map(TaskDto::from)
                .toList();
    }

    @PostMapping
    public TaskDto createTask(
            Authentication authentication,
            @RequestBody CreateTaskRequest request) {

        User user = currentUserService.getCurrentUser(authentication);
        Task created = taskService.createTask(
                user,
                request.title(),
                request.description(),
                request.dueDate(),
                request.priority(),
                request.category(),
                request.recurringRule()
        );
        return TaskDto.from(created);
    }

    @PutMapping("/{id}")
    public TaskDto updateTask(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestBody UpdateTaskRequest request) {

        User user = currentUserService.getCurrentUser(authentication);
        Task updated = taskService.updateTask(
                id,
                user,
                request.title(),
                request.description(),
                request.dueDate(),
                request.priority(),
                request.status(),
                request.category()
        );
        return TaskDto.from(updated);
    }

    @PatchMapping("/{id}/toggle")
    public TaskDto toggleTask(
            @PathVariable UUID id,
            Authentication authentication) {

        User user = currentUserService.getCurrentUser(authentication);
        Task toggled = taskService.toggleTask(id, user);
        return TaskDto.from(toggled);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteTask(
            @PathVariable UUID id,
            Authentication authentication) {

        User user = currentUserService.getCurrentUser(authentication);
        taskService.deleteTask(id, user);
        return Map.of("status", "deleted", "id", id.toString());
    }

    public record CreateTaskRequest(
            String title,
            String description,
            LocalDateTime dueDate,
            TaskPriority priority,
            String category,
            String recurringRule
    ) {}

    public record UpdateTaskRequest(
            String title,
            String description,
            LocalDateTime dueDate,
            TaskPriority priority,
            TaskStatus status,
            String category
    ) {}

    public record TaskDto(
            UUID id,
            String title,
            String description,
            LocalDateTime dueDate,
            String priority,
            String status,
            String category,
            String recurringRule,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
        public static TaskDto from(Task t) {
            return new TaskDto(
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    t.getDueDate(),
                    t.getPriority().name(),
                    t.getStatus().name(),
                    t.getCategory(),
                    t.getRecurringRule(),
                    t.getCreatedAt(),
                    t.getCompletedAt()
            );
        }
    }
}
