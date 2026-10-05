package dev.abrahamgracef.omniassist.google.gmail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTriageServiceTest {

    @Mock
    private GmailService gmailService;

    private EmailTriageService triageService;

    @BeforeEach
    public void setUp() {
        triageService = new EmailTriageService(gmailService);
    }

    @Test
    void testTriageInboxClassifiesCategoriesCorrectly() {
        EmailSummary e1 = new EmailSummary(
                "msg-1",
                "newsletter@techworld.com",
                "Newsletter: Tech Trends This Week",
                "Check out the latest tech news"
        );

        EmailSummary e2 = new EmailSummary(
                "msg-2",
                "cfo@company.com",
                "Re: Budget Approval Request",
                "I am waiting on the audit report before finalizing."
        );

        EmailSummary e3 = new EmailSummary(
                "msg-3",
                "client@clientcorp.com",
                "Urgent: Client Project Deliverable Status",
                "Please send the revised proposal by 3pm today."
        );

        when(gmailService.getLatestEmails(eq("mock-token"), anyInt()))
                .thenReturn(List.of(e1, e2, e3));

        EmailTriageService.TriagedInbox result = triageService.triageInbox("mock-token", 10);

        assertNotNull(result);
        assertEquals(3, result.totalEmails());
        assertEquals(1, result.informationalCount());
        assertEquals(1, result.waitingCount());
        assertEquals(1, result.actionNeededCount());

        EmailTriageService.TriagedEmail actionItem = result.actionNeeded().get(0);
        assertEquals("ACTION_NEEDED", actionItem.category());
        assertFalse(actionItem.suggestedQuickReplies().isEmpty());
    }
}
