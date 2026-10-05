package dev.abrahamgracef.omniassist.task;

import dev.abrahamgracef.omniassist.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByUserOrderByDueDateAscCreatedAtDesc(User user);

    List<Task> findByUserAndStatusOrderByDueDateAscCreatedAtDesc(User user, TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.user = :user AND t.status != 'COMPLETED' AND (t.dueDate <= :endOfDay OR t.dueDate IS NULL) ORDER BY t.priority DESC, t.dueDate ASC")
    List<Task> findTodayAndPendingTasks(@Param("user") User user, @Param("endOfDay") LocalDateTime endOfDay);

    List<Task> findByUserAndTitleContainingIgnoreCase(User user, String title);
}
