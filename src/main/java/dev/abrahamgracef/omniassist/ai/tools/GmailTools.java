package dev.abrahamgracef.omniassist.ai.tools;

import dev.abrahamgracef.omniassist.email.EmailDraft;
import dev.abrahamgracef.omniassist.email.EmailDraftService;
import dev.abrahamgracef.omniassist.google.gmail.EmailSummary;
import dev.abrahamgracef.omniassist.google.gmail.GmailService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GmailTools {

    private final GmailService gmailService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final EmailDraftService emailDraftService;
    private final dev.abrahamgracef.omniassist.google.gmail.EmailTriageService emailTriageService;

    public GmailTools(
            GmailService gmailService,
            OAuth2AuthorizedClientService authorizedClientService,
            EmailDraftService emailDraftService,
            dev.abrahamgracef.omniassist.google.gmail.EmailTriageService emailTriageService) {

        this.gmailService = gmailService;
        this.authorizedClientService = authorizedClientService;
        this.emailDraftService = emailDraftService;
        this.emailTriageService = emailTriageService;
    }
    @Tool(description = """
        Create ONE email draft for review.

        Call this tool only when the CURRENT user message explicitly
        asks to write, compose, draft, or send an email.

        Do not call this tool for general questions, capability questions,
        email-reading requests, or based only on previous conversation context.

        This tool does NOT send email.
        """)
    public EmailDraft createEmailDraft(
            String to,
            String subject,
            String body) {

        String cleanBody = body
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n");

        return emailDraftService.create(
                to,
                subject,
                cleanBody
        );
    }
    private String getAccessToken() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        System.out.println("=== GMAIL TOOL DEBUG ===");
        System.out.println("Authentication: " + authentication);

        if (authentication != null) {
            System.out.println("Authenticated: " + authentication.isAuthenticated());
            System.out.println("Name: " + authentication.getName());
            System.out.println("Principal: " + authentication.getPrincipal());
            System.out.println(
                    "Principal class: " +
                            authentication.getPrincipal().getClass().getName()
            );
        }

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {

            throw new IllegalStateException(
                    "Google account is not connected."
            );
        }

        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient(
                        "google",
                        authentication.getName()
                );

        System.out.println("Authorized client: " + client);

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException(
                    "Google account is not connected."
            );
        }

        System.out.println("Google OAuth client found successfully");

        return client.getAccessToken().getTokenValue();
    }

    @Tool(description = """
        Retrieve the user's latest Gmail emails.

        Call this tool only when the CURRENT user message explicitly
        asks to read, check, view, list, inspect, or summarize emails.

        Do not call this tool for general conversation or capability questions.
        """)
    public List<EmailSummary> getLatestEmails(int count) {

        int safeCount = Math.max(1, Math.min(count, 10));

        return gmailService.getLatestEmails(
                getAccessToken(),
                safeCount
        );
    }

    @Tool(description = """
        Retrieve the user's sent Gmail messages.
        Call this tool when the user asks to see what emails they sent or check sent history.
        """)
    public List<EmailSummary> getSentEmails(int count) {
        int safeCount = Math.max(1, Math.min(count, 10));
        return gmailService.getSentMessages(
                getAccessToken(),
                safeCount
        );
    }

    @Tool(description = """
        Triage the user's unread inbox into 3 smart buckets:
        1) Action Needed (direct questions, approvals, deadlines)
        2) Waiting on Others (follow-up status)
        3) Informational (newsletters, notifications)
        Also suggests 1-click quick replies.
        """)
    public String triageUnreadEmails() {
        var result = emailTriageService.triageInbox(getAccessToken(), 10);
        StringBuilder sb = new StringBuilder();
        sb.append("📥 Inbox Zero Triage Report (").append(result.totalEmails()).append(" emails analyzed):\n\n");

        sb.append("🔴 ACTION NEEDED (").append(result.actionNeededCount()).append("):\n");
        if (result.actionNeeded().isEmpty()) {
            sb.append("None! You are caught up.\n");
        } else {
            for (var item : result.actionNeeded()) {
                sb.append("• \"").append(item.email().subject()).append("\" from ").append(item.email().from()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("🟡 WAITING ON OTHERS (").append(result.waitingCount()).append("):\n");
        for (var item : result.waitingOnOthers()) {
            sb.append("• \"").append(item.email().subject()).append("\" from ").append(item.email().from()).append("\n");
        }
        sb.append("\n");

        sb.append("🟢 INFORMATIONAL / FYI (").append(result.informationalCount()).append(" emails)\n");

        return sb.toString();
    }
}