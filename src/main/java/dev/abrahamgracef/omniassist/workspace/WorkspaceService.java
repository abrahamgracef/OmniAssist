package dev.abrahamgracef.omniassist.workspace;

import dev.abrahamgracef.omniassist.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public Workspace getOrCreateDefaultWorkspace(User user) {
        List<Workspace> workspaces = workspaceRepository.findByUserOrderByCreatedAtAsc(user);
        if (!workspaces.isEmpty()) {
            return workspaces.get(0);
        }

        Workspace defaultWorkspace = new Workspace();
        defaultWorkspace.setUser(user);
        defaultWorkspace.setName("Personal Workspace");
        defaultWorkspace.setType("PERSONAL");
        return workspaceRepository.save(defaultWorkspace);
    }

    public List<Workspace> getWorkspaces(User user) {
        List<Workspace> workspaces = workspaceRepository.findByUserOrderByCreatedAtAsc(user);
        if (workspaces.isEmpty()) {
            return List.of(getOrCreateDefaultWorkspace(user));
        }
        return workspaces;
    }

    @Transactional
    public Workspace createWorkspace(User user, String name, String type) {
        Workspace workspace = new Workspace();
        workspace.setUser(user);
        workspace.setName((name == null || name.isBlank()) ? "Untitled Workspace" : name.trim());
        workspace.setType((type == null || type.isBlank()) ? "PERSONAL" : type.trim().toUpperCase());
        return workspaceRepository.save(workspace);
    }

    @Transactional
    public void deleteWorkspace(UUID workspaceId, User user) {
        workspaceRepository.findById(workspaceId).ifPresent(ws -> {
            if (ws.getUser().getId().equals(user.getId())) {
                workspaceRepository.delete(ws);
            }
        });
    }
}
