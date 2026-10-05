package dev.abrahamgracef.omniassist.knowledge;

import dev.abrahamgracef.omniassist.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private final DocumentNoteRepository repository;

    public KnowledgeService(DocumentNoteRepository repository) {
        this.repository = repository;
    }

    public List<DocumentNote> getNotes(User user) {
        return repository.findByUserOrderByUpdatedAtDesc(user);
    }

    public Optional<DocumentNote> getNote(UUID id, User user) {
        return repository.findById(id).filter(n -> n.getUser().getId().equals(user.getId()));
    }

    public List<DocumentNote> search(User user, String query) {
        if (query == null || query.isBlank()) {
            return getNotes(user);
        }
        return repository.searchByUserAndKeyword(user, query.trim());
    }

    @Transactional
    public DocumentNote createNote(User user, String title, String content, String tags, String sourceType) {
        DocumentNote note = new DocumentNote();
        note.setUser(user);
        note.setTitle(title != null && !title.isBlank() ? title.trim() : "Untitled Note");
        note.setContent(content != null ? content.trim() : "");
        note.setTags(tags != null ? tags.trim() : null);
        note.setSourceType(sourceType != null ? sourceType : "NOTE");

        // Basic auto-summary preview (first 200 chars)
        if (note.getContent().length() > 200) {
            note.setSummary(note.getContent().substring(0, 197) + "...");
        } else {
            note.setSummary(note.getContent());
        }

        return repository.save(note);
    }

    @Transactional
    public DocumentNote uploadDocument(User user, MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "uploaded_file.txt";
        }

        String content;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().collect(Collectors.joining("\n"));
        }

        DocumentNote note = new DocumentNote();
        note.setUser(user);
        note.setTitle(filename);
        note.setContent(content);
        note.setFilename(filename);
        note.setSourceType("UPLOADED_FILE");

        String extension = "";
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = filename.substring(dotIdx + 1).toLowerCase();
        }
        note.setTags("file, " + extension);

        if (content.length() > 250) {
            note.setSummary(content.substring(0, 247) + "...");
        } else {
            note.setSummary(content);
        }

        return repository.save(note);
    }

    @Transactional
    public void deleteNote(UUID id, User user) {
        getNote(id, user).ifPresent(repository::delete);
    }
}
