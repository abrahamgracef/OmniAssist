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

    public GmailController(GmailService gmailService) {
        this.gmailService = gmailService;
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

    public record SendEmailRequest(
            String to,
            String subject,
            String body
    ) {}
}
