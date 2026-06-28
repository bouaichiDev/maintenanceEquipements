package ma.fstg.security.spring_jwt_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ma.fstg.security.spring_jwt_api.entities.User;

@Data
@AllArgsConstructor
public class OrgUserDto {

    private Long id;
    private String username;
    private boolean active;
    private String role;

    public static OrgUserDto from(User user) {
        String role = user.getRoles().isEmpty() ? null : user.getRoles().iterator().next().getName();
        return new OrgUserDto(user.getId(), user.getUsername(), user.isActive(), role);
    }
}
