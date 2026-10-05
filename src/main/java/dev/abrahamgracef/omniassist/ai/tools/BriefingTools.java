package dev.abrahamgracef.omniassist.ai.tools;

import dev.abrahamgracef.omniassist.briefing.BriefingService;
import dev.abrahamgracef.omniassist.user.CurrentUserService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class BriefingTools {

    private final BriefingService briefingService;
    private final CurrentUserService currentUserService;

    public BriefingTools(BriefingService briefingService, CurrentUserService currentUserService) {
        this.briefingService = briefingService;
        this.currentUserService = currentUserService;
    }

    @Tool(description = """
            Get the morning executive briefing digest summarizing today's upcoming meetings,
            high-priority tasks, and strategic advice for the day.
            Call this when the user asks for their morning briefing, daily digest, or executive summary of today.
            """)
    public String getDailyExecutiveBriefing() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = currentUserService.getCurrentUser(auth);

        var briefing = briefingService.getTodayBriefing(user, auth);

        return """
                🌅 Morning Executive Briefing for %s:
                %s

                Summary:
                - Meetings Today: %d
                - Pending Tasks: %d (Urgent: %d)
                """.formatted(
                briefing.userName(),
                briefing.executiveSummary(),
                briefing.meetingCount(),
                briefing.taskCount(),
                briefing.urgentTaskCount()
        );
    }
}
