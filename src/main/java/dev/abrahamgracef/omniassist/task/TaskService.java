package dev.abrahamgracef.omniassist.task;

import dev.abrahamgracef.omniassist.notification.NotificationService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final dev.abrahamgracef.omniassist.admin.AuditService auditService;

    public TaskService(TaskRepository taskRepository,
                       NotificationService notificationService,
                       dev.abrahamgracef.omniassist.admin.AuditService auditService) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public Task createTask(User user, String title, String description, LocalDateTime dueDate,
                           TaskPriority priority, String category, String recurringRule) {
        Task task = new Task();
        task.setUser(user);
        task.setTitle(title.trim());
        task.setDescription(description != null ? description.trim() : null);
        task.setDueDate(dueDate);
        task.setPriority(priority != null ? priority : TaskPriority.MEDIUM);
        task.setStatus(TaskStatus.TODO);
        task.setCategory(category != null ? category.trim() : "General");
        task.setRecurringRule(recurringRule);

        Task saved = taskRepository.save(task);

        auditService.log(user.getEmail(), "TASK_CREATED", "Task: " + saved.getTitle(), "SUCCESS");

        if (priority == TaskPriority.URGENT || priority == TaskPriority.HIGH) {
            notificationService.createNotification(
                    user,
                    "High Priority Task Created",
                    "Task \"" + saved.getTitle() + "\" has been created.",
                    "TASK",
                    "/#tasks"
            );
        }

        return saved;
    }

    public List<Task> getTasks(User user, TaskStatus status, TaskPriority priority) {
        List<Task> tasks;
        if (status != null) {
            tasks = taskRepository.findByUserAndStatusOrderByDueDateAscCreatedAtDesc(user, status);
        } else {
            tasks = taskRepository.findByUserOrderByDueDateAscCreatedAtDesc(user);
        }

        if (priority != null) {
            return tasks.stream().filter(t -> t.getPriority() == priority).toList();
        }
        return tasks;
    }

    public List<Task> getDailyPlannerTasks(User user, LocalDate date) {
        LocalDateTime endOfDay = (date != null ? date : LocalDate.now()).atTime(LocalTime.MAX);
        return taskRepository.findTodayAndPendingTasks(user, endOfDay);
    }

    public Optional<Task> getTask(UUID id, User user) {
        return taskRepository.findById(id).filter(t -> t.getUser().getId().equals(user.getId()));
    }

    @Transactional
    public Task updateTask(UUID id, User user, String title, String description,
                           LocalDateTime dueDate, TaskPriority priority, TaskStatus status, String category) {
        Task task = getTask(id, user).orElseThrow(() -> new IllegalArgumentException("Task not found or access denied"));

        if (title != null && !title.isBlank()) task.setTitle(title.trim());
        if (description != null) task.setDescription(description.trim());
        if (dueDate != null) task.setDueDate(dueDate);
        if (priority != null) task.setPriority(priority);
        if (status != null) {
            task.setStatus(status);
            if (status == TaskStatus.COMPLETED) {
                task.setCompletedAt(LocalDateTime.now());
            } else {
                task.setCompletedAt(null);
            }
        }
        if (category != null) task.setCategory(category.trim());

        return taskRepository.save(task);
    }

    @Transactional
    public Task toggleTask(UUID id, User user) {
        Task task = getTask(id, user).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        if (task.getStatus() == TaskStatus.COMPLETED) {
            task.setStatus(TaskStatus.TODO);
            task.setCompletedAt(null);
        } else {
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            auditService.log(user.getEmail(), "TASK_COMPLETED", "Task completed: " + task.getTitle(), "SUCCESS");
        }
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(UUID id, User user) {
        getTask(id, user).ifPresent(taskRepository::delete);
    }

    public List<Task> findByTitle(User user, String query) {
        return taskRepository.findByUserAndTitleContainingIgnoreCase(user, query);
    }
}
