package dev.abrahamgracef.omniassist.ai.tools;

import dev.abrahamgracef.omniassist.knowledge.DocumentNote;
import dev.abrahamgracef.omniassist.knowledge.DocumentRagService;
import dev.abrahamgracef.omniassist.knowledge.KnowledgeService;
import dev.abrahamgracef.omniassist.user.CurrentUserService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeTools {

    private final KnowledgeService knowledgeService;
    private final CurrentUserService currentUserService;
    private final dev.abrahamgracef.omniassist.meeting.MeetingMinutesService meetingMinutesService;
    private final DocumentRagService documentRagService;

    public KnowledgeTools(
            KnowledgeService knowledgeService,
            CurrentUserService currentUserService,
            dev.abrahamgracef.omniassist.meeting.MeetingMinutesService meetingMinutesService,
            DocumentRagService documentRagService) {
        this.knowledgeService = knowledgeService;
        this.currentUserService = currentUserService;
        this.meetingMinutesService = meetingMinutesService;
        this.documentRagService = documentRagService;
    }

    private User resolveUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return currentUserService.getCurrentUser(auth);
    }

    @Tool(description = """
            Search the user's internal knowledge base, uploaded documents, and saved notes.
            Use this tool when the user asks questions about their notes, saved facts, project documents, or knowledge.
            Parameters:
            - query: Keywords or topics to search for.
            """)
    public String searchKnowledgeBase(String query) {
        User user = resolveUser();
        List<DocumentNote> results = knowledgeService.search(user, query);

        if (results.isEmpty()) {
            return "No matching notes or documents found in knowledge base for: \"" + query + "\".";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.size()).append(" document(s)/note(s):\n\n");
        for (DocumentNote doc : results) {
            sb.append("### ").append(doc.getTitle()).append(" (Tags: ").append(doc.getTags()).append(")\n");
            // Snippet preview (up to 500 chars)
            String preview = doc.getContent();
            if (preview.length() > 500) {
                preview = preview.substring(0, 497) + "...";
            }
            sb.append(preview).append("\n\n");
        }
        return sb.toString();
    }

    @Tool(description = """
            Save a note, important fact, checklist, or knowledge snippet to the user's knowledge base.
            Parameters:
            - title: Title of the note.
            - content: Full content or body of the note.
            - tags: Comma-separated tags (e.g. 'work, meeting, ideas').
            """)
    public String saveNote(String title, String content, String tags) {
        User user = resolveUser();
        DocumentNote note = knowledgeService.createNote(user, title, content, tags, "NOTE");
        return "Successfully saved note: \"" + note.getTitle() + "\" (ID: " + note.getId() + ")";
    }

    @Tool(description = """
            List recent notes and documents from the user's knowledge base.
            """)
    public String listKnowledgeNotes() {
        User user = resolveUser();
        List<DocumentNote> notes = knowledgeService.getNotes(user);

        if (notes.isEmpty()) {
            return "The user's knowledge base is currently empty.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("User Knowledge Base (").append(notes.size()).append(" items):\n");
        for (DocumentNote n : notes) {
            sb.append("- [").append(n.getSourceType()).append("] ")
                    .append(n.getTitle())
                    .append(" (Tags: ").append(n.getTags() != null ? n.getTags() : "none").append(")\n");
        }
        return sb.toString();
    }

    @Tool(description = """
            Process meeting notes or raw discussion transcript. Automatically extracts executive summary,
            decisions, and action items, and converts action items into to-do tasks in the Daily Planner.
            Parameters:
            - notesOrTranscript: The raw meeting notes, text transcript, or agenda discussion.
            """)
    public String processMeetingMinutes(String notesOrTranscript) {
        User user = resolveUser();
        var result = meetingMinutesService.processNotes(user, notesOrTranscript, true, true);

        StringBuilder sb = new StringBuilder();
        sb.append("📋 Meeting Recap Processed:\n\n");
        sb.append("Summary: ").append(result.summary()).append("\n\n");
        if (!result.decisions().isEmpty()) {
            sb.append("Decisions:\n");
            for (String d : result.decisions()) sb.append("✓ ").append(d).append("\n");
            sb.append("\n");
        }
        sb.append("Generated ").append(result.tasksCreatedCount()).append(" task(s) in your Daily Planner.\n");
        if (result.emailDraftId() != null) {
            sb.append("Drafted follow-up recap email ready for review.");
        }
        return sb.toString();
    }

    @Tool(description = """
            Ask targeted questions about a specific document or file in the user's knowledge base.
            Uses deep citation RAG to extract relevant sections.
            Parameters:
            - documentTitleOrKeyword: The title, filename, or topic keyword of the document.
            - question: The specific question to ask about the document contents.
            """)
    public String queryDocumentInDepth(String documentTitleOrKeyword, String question) {
        User user = resolveUser();
        List<DocumentNote> matches = knowledgeService.search(user, documentTitleOrKeyword);
        if (matches.isEmpty()) {
            return "No document found matching \"" + documentTitleOrKeyword + "\" in knowledge base.";
        }

        DocumentNote target = matches.get(0);
        var result = documentRagService.answerDocumentQuestion(target, question);

        StringBuilder sb = new StringBuilder();
        sb.append("Answer (from \"").append(target.getTitle()).append("\"):\n\n");
        sb.append(result.answer()).append("\n\n");
        if (!result.citations().isEmpty()) {
            sb.append("Citations / Excerpts:\n");
            for (var c : result.citations()) {
                sb.append("• [").append(c.section()).append("]: \"")
                        .append(c.excerpt().length() > 150 ? c.excerpt().substring(0, 147) + "..." : c.excerpt())
                        .append("\"\n");
            }
        }
        return sb.toString();
    }
}
