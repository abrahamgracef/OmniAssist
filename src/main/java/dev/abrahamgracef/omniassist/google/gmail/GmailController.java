package dev.abrahamgracef.omniassist.google.gmail;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    private final GmailService gmailService;
    private final EmailTriageService emailTriageService;

    public GmailController(GmailService gmailService, EmailTriageService emailTriageService) {
        this.gmailService = gmailService;
        this.emailTriageService = emailTriageService;
    }

    @GetMapping("/latest")
    public List<EmailSummary> latestEmails(
            @RegisteredOAuth2AuthorizedClient("google")
            OAuth2AuthorizedClient googleClient) {

        String accessToken =
                googleClient.getAccessToken().getTokenValue();

        return gmailService.getLatestEmails(
                accessToken,
                5
        );
    }
    @PostMapping("/send")
    public Map<String, String> sendEmail(
            @RegisteredOAuth2AuthorizedClient("google")
            OAuth2AuthorizedClient googleClient,
            @RequestBody SendEmailRequest request) {

        String accessToken =
                googleClient.getAccessToken().getTokenValue();

        String messageId = gmailService.sendEmail(
                accessToken,
                request.to(),
                request.subject(),
                request.body()
        );

        return Map.of(
                "status", "sent",
                "messageId", messageId
        );
    }

    @GetMapping("/sent")
    public List<EmailSummary> sentEmails(
            @RegisteredOAuth2AuthorizedClient("google")
            OAuth2AuthorizedClient googleClient,
            @RequestParam(defaultValue = "10") int maxResults) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        return gmailService.getSentMessages(accessToken, maxResults);
    }

    @PostMapping("/reply")
    public Map<String, String> replyEmail(
            @RegisteredOAuth2AuthorizedClient("google")
            OAuth2AuthorizedClient googleClient,
            @RequestBody ReplyEmailRequest request) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        String messageId = gmailService.replyToEmail(
                accessToken,
                request.messageId(),
                request.threadId(),
                request.to(),
                request.subject(),
                request.body()
        );

        return Map.of(
                "status", "sent",
                "messageId", messageId
        );
    }

    @PostMapping("/forward")
    public Map<String, String> forwardEmail(
            @RegisteredOAuth2AuthorizedClient("google")
            OAuth2AuthorizedClient googleClient,
            @RequestBody ForwardEmailRequest request) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        String messageId = gmailService.forwardEmail(
                accessToken,
                request.originalMessageId(),
                request.to(),
                request.body()
        );

        return Map.of(
                "status", "sent",
                "messageId", messageId
        );
    }

    @GetMapping("/triage")
    public EmailTriageService.TriagedInbox triage(
            @RegisteredOAuth2AuthorizedClient("google")
            OAuth2AuthorizedClient googleClient,
            @RequestParam(defaultValue = "10") int maxEmails) {

        String accessToken = googleClient.getAccessToken().getTokenValue();
        return emailTriageService.triageInbox(accessToken, maxEmails);
    }

    public record SendEmailRequest(
            String to,
            String subject,
            String body
    ) {}

    public record ReplyEmailRequest(
            String messageId,
            String threadId,
            String to,
            String subject,
            String body
    ) {}

    public record ForwardEmailRequest(
            String originalMessageId,
            String to,
            String body
    ) {}
}
