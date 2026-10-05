package dev.abrahamgracef.omniassist.email;

import dev.abrahamgracef.omniassist.admin.AuditService;
import dev.abrahamgracef.omniassist.google.gmail.GmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailDraftControllerTest {

    private EmailDraftService draftService;

    @Mock
    private GmailService gmailService;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private AuditService auditService;

    private EmailDraftController controller;

    @BeforeEach
    void setUp() {
        draftService = new EmailDraftService();
        controller = new EmailDraftController(draftService, gmailService, authorizedClientService, auditService);
    }

    @Test
    void testUpdateDraft() {
        EmailDraft draft = draftService.create("alice@example.com", "Initial Subject", "Initial Body");

        EmailDraftController.UpdateDraftRequest updateReq =
                new EmailDraftController.UpdateDraftRequest("bob@example.com", "Updated Subject", "Updated Body");

        EmailDraft updated = controller.updateDraft(draft.id(), updateReq);

        assertThat(updated.to()).isEqualTo("bob@example.com");
        assertThat(updated.subject()).isEqualTo("Updated Subject");
        assertThat(updated.body()).isEqualTo("Updated Body");
    }

    @Test
    void testCancelDraft() {
        EmailDraft draft = draftService.create("alice@example.com", "Subject", "Body");

        Map<String, String> response = controller.cancelDraft(draft.id());

        assertThat(response.get("status")).isEqualTo("cancelled");
        assertThatThrownBy(() -> draftService.get(draft.id()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSendDraft_Success() {
        EmailDraft draft = draftService.create("alice@example.com", "Project Status", "Everything is on track.");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user@example.com", "password", java.util.List.of());

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "mock-token-abc",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(token);
        when(authorizedClientService.loadAuthorizedClient("google", "user@example.com"))
                .thenReturn(authorizedClient);

        when(gmailService.sendEmail(eq("mock-token-abc"), eq("alice@example.com"), eq("Project Status"), eq("Everything is on track.")))
                .thenReturn("msg-12345");

        Map<String, String> result = controller.sendDraft(draft.id(), auth);

        assertThat(result.get("status")).isEqualTo("sent");
        assertThat(result.get("messageId")).isEqualTo("msg-12345");
        verify(auditService).log(eq("user@example.com"), eq("EMAIL_SENT"), any(), eq("SUCCESS"));
    }

    @Test
    void testSendDraft_UnauthenticatedThrowsException() {
        EmailDraft draft = draftService.create("alice@example.com", "Subject", "Body");

        assertThatThrownBy(() -> controller.sendDraft(draft.id(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google account is not connected.");
    }

    @Test
    void testSendDraft_FailureRestoresDraft() {
        EmailDraft draft = draftService.create("alice@example.com", "Fail Subject", "Fail Body");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user@example.com", "password", java.util.List.of());

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "mock-token-abc",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(token);
        when(authorizedClientService.loadAuthorizedClient("google", "user@example.com"))
                .thenReturn(authorizedClient);

        when(gmailService.sendEmail(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Gmail API unavailable"));

        assertThatThrownBy(() -> controller.sendDraft(draft.id(), auth))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Gmail API unavailable");

        // Assert draft was restored so user can retry
        assertThat(draftService.get(draft.id())).isNotNull();
    }
}
