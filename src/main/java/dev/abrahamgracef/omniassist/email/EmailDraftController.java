package dev.abrahamgracef.omniassist.email;

import dev.abrahamgracef.omniassist.google.gmail.GmailService;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/email")
public class EmailDraftController {

    private final EmailDraftService draftService;
    private final GmailService gmailService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public EmailDraftController(
            EmailDraftService draftService,
            GmailService gmailService,
            OAuth2AuthorizedClientService authorizedClientService) {

        this.draftService = draftService;
        this.gmailService = gmailService;
        this.authorizedClientService = authorizedClientService;
    }


    @PutMapping("/draft/{id}")
    public EmailDraft updateDraft(
            @PathVariable UUID id,
            @RequestBody UpdateDraftRequest request) {

        return draftService.update(
                id,
                request.to(),
                request.subject(),
                request.body()
        );
    }


    @DeleteMapping("/draft/{id}")
    public Map<String, String> cancelDraft(
            @PathVariable UUID id) {

        draftService.delete(id);

        return Map.of(
                "status", "cancelled"
        );
    }


    @PostMapping("/draft/{id}/send")
    public Map<String, String> sendDraft(
            @PathVariable UUID id,
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "Google account is not connected."
            );
        }

        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient(
                        "google",
                        authentication.getName()
                );

        if (client == null ||
                client.getAccessToken() == null) {

            throw new IllegalStateException(
                    "Google account is not connected."
            );
        }

        /*
         * Remove FIRST.
         * A second request with the same ID cannot retrieve it.
         */
        EmailDraft draft =
                draftService.takeForSending(id);

        try {

            String messageId =
                    gmailService.sendEmail(
                            client.getAccessToken().getTokenValue(),
                            draft.to(),
                            draft.subject(),
                            draft.body()
                    );

            return Map.of(
                    "status", "sent",
                    "messageId", messageId
            );

        } catch (Exception exception) {

            /*
             * Gmail failed, so allow the user to retry.
             */
            draftService.restore(draft);

            throw exception;
        }
    }


    public record UpdateDraftRequest(
            String to,
            String subject,
            String body
    ) {
    }
}