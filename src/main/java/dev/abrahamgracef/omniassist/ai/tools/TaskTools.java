package dev.abrahamgracef.omniassist.ai.tools;

import dev.abrahamgracef.omniassist.task.Task;
import dev.abrahamgracef.omniassist.task.TaskPriority;
import dev.abrahamgracef.omniassist.task.TaskService;
import dev.abrahamgracef.omniassist.task.TaskStatus;
import dev.abrahamgracef.omniassist.user.CurrentUserService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class TaskTools {

    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    public TaskTools(TaskService taskService, CurrentUserService currentUserService) {
        this.taskService = taskService;
        this.currentUserService = currentUserService;
    }

    private User resolveUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return currentUserService.getCurrentUser(auth);
    }

    @Tool(description = """
            Create a new task, to-do item, or action item.
            Parameters:
            - title: The name/summary of the task (required).
            - description: Detailed notes or subtasks (optional).
            - dueDate: Due date/time in ISO-8601 format e.g. '2026-10-06T17:00:00' or date '2026-10-06' (optional).
            - priority: One of LOW, MEDIUM, HIGH, URGENT (optional, default MEDIUM).
            - category: Category such as 'Work', 'Personal', 'Meeting Prep', 'Follow-up' (optional).
            """)
    public String createTask(String title, String description, String dueDate, String priority, String category) {
        User user = resolveUser();

        LocalDateTime parsedDue = null;
        if (dueDate != null && !dueDate.isBlank()) {
            try {
                if (dueDate.length() == 10) {
                    parsedDue = LocalDate.parse(dueDate).atTime(17, 0);
                } else {
                    parsedDue = LocalDateTime.parse(dueDate, DateTimeFormatter.ISO_DATE_TIME);
                }
            } catch (Exception ignored) {
            }
        }

        TaskPriority parsedPriority = TaskPriority.MEDIUM;
        if (priority != null && !priority.isBlank()) {
            try {
                parsedPriority = TaskPriority.valueOf(priority.trim().toUpperCase());
            } catch (Exception ignored) {
            }
        }

        Task task = taskService.createTask(
                user,
                title,
                description,
                parsedDue,
                parsedPriority,
                category != null ? category : "General",
                null
        );

        return "Successfully created task: [ID: %s] \"%s\" (Priority: %s, Due: %s)".formatted(
                task.getId(),
                task.getTitle(),
                task.getPriority(),
                task.getDueDate() != null ? task.getDueDate() : "None"
        );
    }

    @Tool(description = """
            List the user's tasks or to-do items.
            Parameters:
            - status: Optional filter: 'TODO', 'IN_PROGRESS', or 'COMPLETED'. Leave empty/null to list pending tasks.
            - priority: Optional filter: 'LOW', 'MEDIUM', 'HIGH', or 'URGENT'.
            """)
    public String listTasks(String status, String priority) {
        User user = resolveUser();

        TaskStatus taskStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                taskStatus = TaskStatus.valueOf(status.trim().toUpperCase());
            } catch (Exception ignored) {
            }
        } else {
            taskStatus = TaskStatus.TODO;
        }

        TaskPriority taskPriority = null;
        if (priority != null && !priority.isBlank()) {
            try {
                taskPriority = TaskPriority.valueOf(priority.trim().toUpperCase());
            } catch (Exception ignored) {
            }
        }

        List<Task> tasks = taskService.getTasks(user, taskStatus, taskPriority);

        if (tasks.isEmpty()) {
            return "No tasks found matching status=" + taskStatus + (taskPriority != null ? " and priority=" + taskPriority : "") + ".";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(tasks.size()).append(" task(s):\n");
        for (Task t : tasks) {
            sb.append("- [ID: ").append(t.getId()).append("] ")
                    .append(t.getTitle())
                    .append(" | Priority: ").append(t.getPriority())
                    .append(" | Status: ").append(t.getStatus())
                    .append(" | Due: ").append(t.getDueDate() != null ? t.getDueDate().toString() : "None")
                    .append(" | Category: ").append(t.getCategory() != null ? t.getCategory() : "General")
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = """
            Mark a task as completed or toggle its status.
            Parameters:
            - taskIdOrTitle: The UUID of the task, or a matching title/keyword of the task.
            """)
    public String completeTask(String taskIdOrTitle) {
        User user = resolveUser();

        try {
            UUID id = UUID.fromString(taskIdOrTitle.trim());
            Task toggled = taskService.toggleTask(id, user);
            return "Task updated: \"" + toggled.getTitle() + "\" is now " + toggled.getStatus() + ".";
        } catch (IllegalArgumentException notUuid) {
            List<Task> matches = taskService.findByTitle(user, taskIdOrTitle.trim());
            if (matches.isEmpty()) {
                return "No task found matching: \"" + taskIdOrTitle + "\".";
            }
            Task target = matches.get(0);
            Task toggled = taskService.toggleTask(target.getId(), user);
            return "Task updated: \"" + toggled.getTitle() + "\" is now " + toggled.getStatus() + ".";
        }
    }

    @Tool(description = """
            Get the daily planner combining today's pending tasks and deadlines.
            Parameters:
            - date: Optional ISO date string (YYYY-MM-DD). If omitted, uses today.
            """)
    public String getDailyPlanner(String date) {
        User user = resolveUser();
        LocalDate targetDate = LocalDate.now();
        if (date != null && !date.isBlank()) {
            try {
                targetDate = LocalDate.parse(date.trim());
            } catch (Exception ignored) {
            }
        }

        List<Task> tasks = taskService.getDailyPlannerTasks(user, targetDate);

        if (tasks.isEmpty()) {
            return "Daily Planner for " + targetDate + ": No pending or overdue tasks! You are all caught up.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Daily Planner for ").append(targetDate).append(":\n");
        for (Task t : tasks) {
            sb.append("• [").append(t.getPriority()).append("] ")
                    .append(t.getTitle())
                    .append(" (Due: ").append(t.getDueDate() != null ? t.getDueDate().toString() : "Today/Open")
                    .append(", Category: ").append(t.getCategory()).append(")\n");
        }
        return sb.toString();
    }
}
