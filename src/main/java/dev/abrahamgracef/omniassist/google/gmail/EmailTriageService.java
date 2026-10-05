package dev.abrahamgracef.omniassist.google.gmail;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmailTriageService {

    private final GmailService gmailService;

    public EmailTriageService(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    public TriagedInbox triageInbox(String accessToken, int maxEmails) {
        List<EmailSummary> emails = gmailService.getLatestEmails(accessToken, Math.max(1, Math.min(maxEmails, 15)));

        List<TriagedEmail> actionNeeded = new ArrayList<>();
        List<TriagedEmail> waitingOnOthers = new ArrayList<>();
        List<TriagedEmail> informational = new ArrayList<>();

        for (EmailSummary email : emails) {
            String subject = email.subject() != null ? email.subject().toLowerCase() : "";
            String snippet = email.snippet() != null ? email.snippet().toLowerCase() : "";
            String from = email.from() != null ? email.from().toLowerCase() : "";

            // Heuristic + AI categorization
            if (from.contains("no-reply") || from.contains("newsletter") || from.contains("notification") ||
                subject.contains("newsletter") || subject.contains("receipt") || subject.contains("digest")) {
                informational.add(new TriagedEmail(email, "INFORMATIONAL", List.of()));
            } else if (subject.startsWith("re:") && (snippet.contains("waiting") || snippet.contains("let you know") || snippet.contains("will update"))) {
                waitingOnOthers.add(new TriagedEmail(email, "WAITING_ON_OTHERS", List.of("Follow up on status", "Acknowledge")));
            } else {
                // Default to Action Needed with 3 quick response suggestions
                List<String> quickReplies = List.of(
                        "Thanks, looking into this now.",
                        "Confirmed, will update shortly.",
                        "Can we discuss during our next sync?"
                );
                actionNeeded.add(new TriagedEmail(email, "ACTION_NEEDED", quickReplies));
            }
        }

        return new TriagedInbox(
                emails.size(),
                actionNeeded.size(),
                waitingOnOthers.size(),
                informational.size(),
                actionNeeded,
                waitingOnOthers,
                informational
        );
    }

    public record TriagedEmail(
            EmailSummary email,
            String category,
            List<String> suggestedQuickReplies
    ) {}

    public record TriagedInbox(
            int totalEmails,
            int actionNeededCount,
            int waitingCount,
            int informationalCount,
            List<TriagedEmail> actionNeeded,
            List<TriagedEmail> waitingOnOthers,
            List<TriagedEmail> informational
    ) {}
}
