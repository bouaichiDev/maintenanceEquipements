package ma.fstg.security.spring_jwt_api.services;

import ma.fstg.security.spring_jwt_api.dto.UserProfileDto;
import ma.fstg.security.spring_jwt_api.entities.User;
import ma.fstg.security.spring_jwt_api.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileDto getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + username));

        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.isActive(),
                user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toList())
        );
    }
}
