package dev.abrahamgracef.omniassist.meeting;

import dev.abrahamgracef.omniassist.user.CurrentUserService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meeting")
public class MeetingMinutesController {

    private final MeetingMinutesService meetingMinutesService;
    private final CurrentUserService currentUserService;

    public MeetingMinutesController(MeetingMinutesService meetingMinutesService, CurrentUserService currentUserService) {
        this.meetingMinutesService = meetingMinutesService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/process-notes")
    public MeetingMinutesService.ProcessedMinutesResult processNotes(
            Authentication authentication,
            @RequestBody ProcessNotesRequest request) {

        User user = currentUserService.getCurrentUser(authentication);
        return meetingMinutesService.processNotes(
                user,
                request.notes(),
                request.createTasks(),
                request.createEmailDraft()
        );
    }

    public record ProcessNotesRequest(String notes, boolean createTasks, boolean createEmailDraft) {}
}
