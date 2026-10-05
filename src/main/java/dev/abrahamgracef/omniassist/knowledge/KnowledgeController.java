package dev.abrahamgracef.omniassist.knowledge;

import dev.abrahamgracef.omniassist.user.CurrentUserService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final CurrentUserService currentUserService;
    private final DocumentRagService documentRagService;

    public KnowledgeController(
            KnowledgeService knowledgeService,
            CurrentUserService currentUserService,
            DocumentRagService documentRagService) {
        this.knowledgeService = knowledgeService;
        this.currentUserService = currentUserService;
        this.documentRagService = documentRagService;
    }

    @GetMapping
    public List<DocumentNoteDto> getNotes(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        return knowledgeService.getNotes(user).stream()
                .map(DocumentNoteDto::from)
                .toList();
    }

    @GetMapping("/search")
    public List<DocumentNoteDto> search(Authentication authentication, @RequestParam String q) {
        User user = currentUserService.getCurrentUser(authentication);
        return knowledgeService.search(user, q).stream()
                .map(DocumentNoteDto::from)
                .toList();
    }

    @PostMapping("/notes")
    public DocumentNoteDto createNote(
            Authentication authentication,
            @RequestBody CreateNoteRequest request) {

        User user = currentUserService.getCurrentUser(authentication);
        DocumentNote note = knowledgeService.createNote(
                user,
                request.title(),
                request.content(),
                request.tags(),
                "NOTE"
        );
        return DocumentNoteDto.from(note);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {

        User user = currentUserService.getCurrentUser(authentication);
        try {
            DocumentNote note = knowledgeService.uploadDocument(user, file);
            return ResponseEntity.ok(DocumentNoteDto.from(note));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to process file: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteNote(
            @PathVariable UUID id,
            Authentication authentication) {

        User user = currentUserService.getCurrentUser(authentication);
        knowledgeService.deleteNote(id, user);
        return Map.of("status", "deleted", "id", id.toString());
    }

    @PostMapping("/{id}/qa")
    public ResponseEntity<?> askDocument(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestBody AskDocumentRequest request) {

        User user = currentUserService.getCurrentUser(authentication);
        return knowledgeService.getNote(id, user)
                .map(doc -> ResponseEntity.ok(documentRagService.answerDocumentQuestion(doc, request.question())))
                .orElse(ResponseEntity.notFound().build());
    }

    public record AskDocumentRequest(String question) {}

    public record CreateNoteRequest(String title, String content, String tags) {}

    public record DocumentNoteDto(
            UUID id,
            String title,
            String content,
            String summary,
            String tags,
            String sourceType,
            String filename,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static DocumentNoteDto from(DocumentNote n) {
            return new DocumentNoteDto(
                    n.getId(),
                    n.getTitle(),
                    n.getContent(),
                    n.getSummary(),
                    n.getTags(),
                    n.getSourceType(),
                    n.getFilename(),
                    n.getCreatedAt(),
                    n.getUpdatedAt()
            );
        }
    }
}
