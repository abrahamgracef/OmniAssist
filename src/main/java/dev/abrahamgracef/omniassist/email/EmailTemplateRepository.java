package dev.abrahamgracef.omniassist.email;

import dev.abrahamgracef.omniassist.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    List<EmailTemplate> findByUserOrderByCreatedAtDesc(User user);
    List<EmailTemplate> findByUserAndCategoryOrderByCreatedAtDesc(User user, String category);
}
