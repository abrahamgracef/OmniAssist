package dev.abrahamgracef.omniassist.email;

import dev.abrahamgracef.omniassist.user.CurrentUserService;
import dev.abrahamgracef.omniassist.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gmail/templates")
public class EmailTemplateController {

    private final EmailTemplateService templateService;
    private final CurrentUserService currentUserService;

    public EmailTemplateController(EmailTemplateService templateService, CurrentUserService currentUserService) {
        this.templateService = templateService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<TemplateDto> getTemplates(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        return templateService.getTemplates(user).stream()
                .map(TemplateDto::from)
                .toList();
    }

    @PostMapping
    public TemplateDto createTemplate(
            Authentication authentication,
            @RequestBody CreateTemplateRequest request) {

        User user = currentUserService.getCurrentUser(authentication);
        EmailTemplate created = templateService.createTemplate(
                user,
                request.name(),
                request.subject(),
                request.body(),
                request.category()
        );
        return TemplateDto.from(created);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteTemplate(
            @PathVariable UUID id,
            Authentication authentication) {

        User user = currentUserService.getCurrentUser(authentication);
        templateService.deleteTemplate(id, user);
        return Map.of("status", "deleted");
    }

    public record CreateTemplateRequest(
            String name,
            String subject,
            String body,
            String category
    ) {}

    public record TemplateDto(
            UUID id,
            String name,
            String subject,
            String body,
            String category,
            LocalDateTime createdAt
    ) {
        public static TemplateDto from(EmailTemplate t) {
            return new TemplateDto(
                    t.getId(),
                    t.getName(),
                    t.getSubject(),
                    t.getBody(),
                    t.getCategory(),
                    t.getCreatedAt()
            );
        }
    }
}
