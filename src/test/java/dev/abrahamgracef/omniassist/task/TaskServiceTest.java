package dev.abrahamgracef.omniassist.task;

import dev.abrahamgracef.omniassist.admin.AuditService;
import dev.abrahamgracef.omniassist.notification.NotificationService;
import dev.abrahamgracef.omniassist.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    private TaskService taskService;
    private User testUser;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, notificationService, auditService);
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@omniassist.local");
        testUser.setDisplayName("Test User");
    }

    @Test
    void testCreateTask() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        Task task = taskService.createTask(
                testUser,
                "Finalize Q3 Report",
                "Draft slides and financial tables",
                LocalDateTime.now().plusDays(2),
                TaskPriority.HIGH,
                "Finance",
                null
        );

        assertNotNull(task);
        assertEquals("Finalize Q3 Report", task.getTitle());
        assertEquals(TaskPriority.HIGH, task.getPriority());
        assertEquals(TaskStatus.TODO, task.getStatus());
        verify(auditService, times(1)).log(eq("test@omniassist.local"), eq("TASK_CREATED"), anyString(), eq("SUCCESS"));
        verify(notificationService, times(1)).createNotification(eq(testUser), anyString(), anyString(), eq("TASK"), anyString());
    }

    @Test
    void testToggleTaskStatus() {
        Task task = new Task();
        UUID taskId = UUID.randomUUID();
        task.setId(taskId);
        task.setUser(testUser);
        task.setTitle("Review Pull Request");
        task.setStatus(TaskStatus.TODO);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task toggled = taskService.toggleTask(taskId, testUser);
        assertEquals(TaskStatus.COMPLETED, toggled.getStatus());
        assertNotNull(toggled.getCompletedAt());
        verify(auditService, times(1)).log(eq("test@omniassist.local"), eq("TASK_COMPLETED"), anyString(), eq("SUCCESS"));

        Task unCompleted = taskService.toggleTask(taskId, testUser);
        assertEquals(TaskStatus.TODO, unCompleted.getStatus());
        assertNull(unCompleted.getCompletedAt());
    }

    @Test
    void testGetDailyPlannerTasks() {
        when(taskRepository.findTodayAndPendingTasks(eq(testUser), any())).thenReturn(List.of(
                new Task(), new Task()
        ));

        List<Task> planner = taskService.getDailyPlannerTasks(testUser, LocalDate.now());
        assertEquals(2, planner.size());
    }
}
