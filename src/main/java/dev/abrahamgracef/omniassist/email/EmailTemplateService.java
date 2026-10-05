package dev.abrahamgracef.omniassist.email;

import dev.abrahamgracef.omniassist.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;

    public EmailTemplateService(EmailTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<EmailTemplate> getTemplates(User user) {
        List<EmailTemplate> list = templateRepository.findByUserOrderByCreatedAtDesc(user);
        if (list.isEmpty()) {
            seedDefaults(user);
            return templateRepository.findByUserOrderByCreatedAtDesc(user);
        }
        return list;
    }

    @Transactional
    public void seedDefaults(User user) {
        EmailTemplate t1 = new EmailTemplate();
        t1.setUser(user);
        t1.setName("Meeting Follow-up");
        t1.setSubject("Follow-up: Our meeting discussion");
        t1.setBody("Hi {{name}},\n\nThank you for taking the time to meet today. As discussed, here are the key action items:\n- \n- \n\nPlease let me know if you have any questions.\n\nBest regards,");
        t1.setCategory("Meeting");
        templateRepository.save(t1);

        EmailTemplate t2 = new EmailTemplate();
        t2.setUser(user);
        t2.setName("Project Status Update");
        t2.setSubject("Update: {{project_name}} Weekly Progress");
        t2.setBody("Hi Team,\n\nHere is a quick summary of progress for this week:\n- Completed: \n- In Progress: \n- Next steps: \n\nBest regards,");
        t2.setCategory("Status");
        templateRepository.save(t2);

        EmailTemplate t3 = new EmailTemplate();
        t3.setUser(user);
        t3.setName("Quick Check-in");
        t3.setSubject("Quick check-in regarding {{topic}}");
        t3.setBody("Hi {{name}},\n\nHope you're having a productive week! Just checking in to see if you had any updates on {{topic}}.\n\nLooking forward to hearing from you,\n");
        t3.setCategory("Follow-up");
        templateRepository.save(t3);
    }

    @Transactional
    public EmailTemplate createTemplate(User user, String name, String subject, String body, String category) {
        EmailTemplate template = new EmailTemplate();
        template.setUser(user);
        template.setName(name);
        template.setSubject(subject);
        template.setBody(body);
        template.setCategory(category != null ? category : "General");
        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(UUID id, User user) {
        templateRepository.findById(id).ifPresent(t -> {
            if (t.getUser().getId().equals(user.getId())) {
                templateRepository.delete(t);
            }
        });
    }

    public Optional<EmailTemplate> getTemplate(UUID id, User user) {
        return templateRepository.findById(id).filter(t -> t.getUser().getId().equals(user.getId()));
    }
}
