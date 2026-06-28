package ma.fstg.security.spring_jwt_api.security;

import ma.fstg.security.spring_jwt_api.entities.Organization;
import ma.fstg.security.spring_jwt_api.entities.User;
import ma.fstg.security.spring_jwt_api.exceptions.AccessDeniedException;
import ma.fstg.security.spring_jwt_api.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_TECH = "ROLE_TECH";
    public static final String ROLE_USER = "ROLE_USER";

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Aucun utilisateur authentifié");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Utilisateur introuvable : " + username));
    }

    public boolean isSuperAdmin() {
        return hasRole(getCurrentUser(), ROLE_SUPER_ADMIN);
    }

    public boolean isAdmin() {
        return hasRole(getCurrentUser(), ROLE_ADMIN);
    }

    public boolean isTechnician() {
        return hasRole(getCurrentUser(), ROLE_TECH);
    }

    public Long getCurrentOrganizationId() {
        User user = getCurrentUser();
        Organization organization = user.getOrganization();
        if (organization == null) {
            throw new AccessDeniedException("Le compte n'est rattaché à aucune organisation");
        }
        return organization.getId();
    }

    public boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }
}
