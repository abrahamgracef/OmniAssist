package dev.abrahamgracef.omniassist.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    public static final String DEMO_EMAIL = "demo@omniassist.local";

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User getCurrentUser(Authentication authentication) {
        if (authentication != null &&
                authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof OAuth2User oauth2User) {

            String email = oauth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                String name = oauth2User.getAttribute("name");
                String picture = oauth2User.getAttribute("picture");

                return userRepository.findByEmail(email).map(existing -> {
                    boolean changed = false;
                    if (name != null && !name.equals(existing.getDisplayName())) {
                        existing.setDisplayName(name);
                        changed = true;
                    }
                    if (picture != null && !picture.equals(existing.getAvatarUrl())) {
                        existing.setAvatarUrl(picture);
                        changed = true;
                    }
                    return changed ? userRepository.save(existing) : existing;
                }).orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setDisplayName(name != null ? name : email);
                    newUser.setAvatarUrl(picture);
                    newUser.setRole("ROLE_USER");
                    newUser.setTimezone("UTC");
                    newUser.setTheme("dark");
                    return userRepository.save(newUser);
                });
            }
        }

        // Fallback for non-authenticated / local demo use
        return userRepository.findByEmail(DEMO_EMAIL).orElseGet(() -> {
            User demo = new User();
            demo.setEmail(DEMO_EMAIL);
            demo.setDisplayName("Demo User");
            demo.setRole("ROLE_USER");
            demo.setTimezone("UTC");
            demo.setTheme("dark");
            return userRepository.save(demo);
        });
    }
}
