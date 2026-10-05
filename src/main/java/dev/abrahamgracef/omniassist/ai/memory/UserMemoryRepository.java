package dev.abrahamgracef.omniassist.ai.memory;

import dev.abrahamgracef.omniassist.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserMemoryRepository extends JpaRepository<UserMemory, UUID> {
    List<UserMemory> findByUserOrderByUpdatedAtDesc(User user);
    Optional<UserMemory> findByUserAndMemoryKey(User user, String memoryKey);
}
