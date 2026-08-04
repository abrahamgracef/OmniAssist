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

    public GmailTools(
            GmailService gmailService,
            OAuth2AuthorizedClientService authorizedClientService,
            EmailDraftService emailDraftService) {

        this.gmailService = gmailService;
        this.authorizedClientService = authorizedClientService;
        this.emailDraftService = emailDraftService;
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
}