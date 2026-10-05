package dev.abrahamgracef.omniassist.knowledge;

import dev.abrahamgracef.omniassist.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private DocumentNoteRepository repository;

    private KnowledgeService knowledgeService;
    private User testUser;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(repository);
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@omniassist.local");
        testUser.setDisplayName("Test User");
    }

    @Test
    void testCreateNote() {
        when(repository.save(any(DocumentNote.class))).thenAnswer(i -> {
            DocumentNote n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        DocumentNote note = knowledgeService.createNote(
                testUser,
                "API Architecture Notes",
                "We are using Spring AI with OpenAI-compatible Gemini endpoints.",
                "spring, architecture, gemini",
                "NOTE"
        );

        assertNotNull(note);
        assertEquals("API Architecture Notes", note.getTitle());
        assertTrue(note.getContent().contains("Spring AI"));
        assertEquals("NOTE", note.getSourceType());
        verify(repository, times(1)).save(any(DocumentNote.class));
    }

    @Test
    void testUploadDocument() throws Exception {
        when(repository.save(any(DocumentNote.class))).thenAnswer(i -> {
            DocumentNote n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "Meeting recap with executive team on October 2026.".getBytes(StandardCharsets.UTF_8)
        );

        DocumentNote note = knowledgeService.uploadDocument(testUser, file);
        assertNotNull(note);
        assertEquals("notes.txt", note.getTitle());
        assertEquals("UPLOADED_FILE", note.getSourceType());
        assertEquals("notes.txt", note.getFilename());
        assertTrue(note.getContent().contains("Meeting recap"));
    }

    @Test
    void testSearch() {
        when(repository.searchByUserAndKeyword(eq(testUser), eq("gemini"))).thenReturn(List.of(
                new DocumentNote()
        ));

        List<DocumentNote> results = knowledgeService.search(testUser, "gemini");
        assertEquals(1, results.size());
    }
}
