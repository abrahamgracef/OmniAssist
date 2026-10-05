package dev.abrahamgracef.omniassist.briefing;

import dev.abrahamgracef.omniassist.user.CurrentUserService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/briefing")
public class BriefingController {

    private final BriefingService briefingService;
    private final CurrentUserService currentUserService;

    public BriefingController(BriefingService briefingService, CurrentUserService currentUserService) {
        this.briefingService = briefingService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/today")
    public BriefingService.DailyBriefing getTodayBriefing(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        return briefingService.getTodayBriefing(user, authentication);
    }

    @PostMapping("/notify")
    public Map<String, String> pushNotification(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        briefingService.pushBriefingNotification(user, authentication);
        return Map.of("status", "sent");
    }
}
