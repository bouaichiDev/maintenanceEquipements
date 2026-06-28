package ma.fstg.security.spring_jwt_api.dto;

import java.util.List;

public record UserProfileDto(
        Long id,
        String username,
        boolean active,
        List<String> roles
) {
}
