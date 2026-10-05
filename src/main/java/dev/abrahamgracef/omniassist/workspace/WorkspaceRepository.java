package dev.abrahamgracef.omniassist.workspace;

import dev.abrahamgracef.omniassist.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    List<Workspace> findByUserOrderByCreatedAtAsc(User user);
    List<Workspace> findByUserIdOrderByCreatedAtAsc(UUID userId);
}
