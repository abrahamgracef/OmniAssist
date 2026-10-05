package dev.abrahamgracef.omniassist.knowledge;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DocumentRagService {

    private final ChatClient chatClient;

    public DocumentRagService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public DocumentQaResult answerDocumentQuestion(DocumentNote document, String question) {
        if (document == null || document.getContent() == null || document.getContent().isBlank()) {
            return new DocumentQaResult(question, "The document contains no readable text.", List.of());
        }

        List<String> chunks = splitIntoChunks(document.getContent(), 400, 80);
        if (chunks.isEmpty()) {
            chunks = List.of(document.getContent());
        }

        // Rank chunks by keyword relevance
        List<ScoredChunk> scoredChunks = new ArrayList<>();
        Set<String> queryTokens = extractTokens(question);

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            int score = scoreChunk(chunk, queryTokens);
            scoredChunks.add(new ScoredChunk(i + 1, chunk, score));
        }

        scoredChunks.sort((a, b) -> Integer.compare(b.score(), a.score()));
        List<ScoredChunk> topChunks = scoredChunks.subList(0, Math.min(3, scoredChunks.size()));

        StringBuilder contextBuilder = new StringBuilder();
        List<CitedChunk> citations = new ArrayList<>();

        for (ScoredChunk sc : topChunks) {
            contextBuilder.append("[Section ").append(sc.index()).append("]:\n")
                    .append(sc.text()).append("\n\n");
            citations.add(new CitedChunk("Section " + sc.index(), sc.text()));
        }

        String promptText = """
            You are an expert technical document analyst answering questions strictly based on the provided document excerpts.
            
            DOCUMENT TITLE: %s
            DOCUMENT EXCERPTS:
            %s
            
            USER QUESTION:
            %s
            
            Provide a clear, accurate, and direct answer based ONLY on the excerpts above. Cite specific section numbers if applicable.
            """.formatted(document.getTitle(), contextBuilder.toString(), question);

        String answer;
        try {
            answer = chatClient.prompt(new Prompt(promptText)).call().content();
        } catch (Exception e) {
            answer = "Based on document excerpts: " + topChunks.get(0).text();
        }

        return new DocumentQaResult(question, answer != null ? answer.trim() : "", citations);
    }

    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\\s*\n");

        StringBuilder current = new StringBuilder();
        for (String p : paragraphs) {
            if (current.length() + p.length() > chunkSize && current.length() > 0) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(p).append("\n\n");
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    private Set<String> extractTokens(String text) {
        if (text == null) return Set.of();
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9 ]", "").split("\\s+");
        return new HashSet<>(Arrays.asList(words));
    }

    private int scoreChunk(String chunk, Set<String> queryTokens) {
        String lower = chunk.toLowerCase();
        int score = 0;
        for (String token : queryTokens) {
            if (token.length() > 2 && lower.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private record ScoredChunk(int index, String text, int score) {}

    public record CitedChunk(String section, String excerpt) {}

    public record DocumentQaResult(
            String question,
            String answer,
            List<CitedChunk> citations
    ) {}
}
