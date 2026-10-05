package dev.abrahamgracef.omniassist.ai.memory;

import dev.abrahamgracef.omniassist.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MemoryService {

    private final UserMemoryRepository repository;

    public MemoryService(UserMemoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveMemory(User user, String category, String key, String value) {
        UserMemory memory = repository.findByUserAndMemoryKey(user, key)
                .orElseGet(() -> {
                    UserMemory m = new UserMemory();
                    m.setUser(user);
                    m.setMemoryKey(key);
                    return m;
                });

        memory.setCategory(category != null ? category : "FACT");
        memory.setMemoryValue(value);
        repository.save(memory);
    }

    public List<UserMemory> getMemories(User user) {
        return repository.findByUserOrderByUpdatedAtDesc(user);
    }

    public String formatMemoriesForPrompt(User user) {
        List<UserMemory> memories = getMemories(user);
        if (memories.isEmpty()) {
            return "";
        }

        return "\n\nUSER PREFERENCES & LONG-TERM MEMORY:\n" +
                memories.stream()
                        .map(m -> "- [" + m.getCategory() + "] " + m.getMemoryKey() + ": " + m.getMemoryValue())
                        .collect(Collectors.joining("\n"));
    }
}
