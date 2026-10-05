package dev.abrahamgracef.omniassist.knowledge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentRagServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    private DocumentRagService ragService;

    @BeforeEach
    void setUp() {
        ragService = new DocumentRagService(chatClientBuilder);
    }

    @Test
    void testAnswerEmptyDocument() {
        DocumentNote note = new DocumentNote();
        note.setContent("");

        DocumentRagService.DocumentQaResult result = ragService.answerDocumentQuestion(note, "What is the policy?");

        assertNotNull(result);
        assertEquals("The document contains no readable text.", result.answer());
        assertTrue(result.citations().isEmpty());
    }

    @Test
    void testAnswerDocumentQuestionWithChunksAndGeminiResponse() {
        DocumentNote note = new DocumentNote();
        note.setId(UUID.randomUUID());
        note.setTitle("OmniAssist Security Architecture");
        note.setContent("""
            Section 1: Authentication and Authorization.
            OmniAssist integrates OAuth2 for Google services and enforces strict token encryption.
            All user secrets are stored in encrypted form with AES-256 GCM.
            
            Section 2: Networking and Cloud Deployment.
            Services communicate across VPC peering with TLS 1.3 enforced for all transport traffic.
            Database backups run nightly at 02:00 UTC.
            """);

        when(chatClientBuilder.build().prompt(any(Prompt.class)).call().content())
                .thenReturn("OmniAssist uses AES-256 GCM for storing encrypted user secrets (Section 1).");

        DocumentRagService.DocumentQaResult result = ragService.answerDocumentQuestion(note, "What encryption standard is used?");

        assertNotNull(result);
        assertTrue(result.answer().contains("AES-256 GCM"));
        assertFalse(result.citations().isEmpty());
        assertTrue(result.citations().get(0).section().contains("Section"));
    }
}
