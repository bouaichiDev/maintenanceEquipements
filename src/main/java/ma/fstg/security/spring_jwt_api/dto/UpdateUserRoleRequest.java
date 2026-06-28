package ma.fstg.security.spring_jwt_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {

    @NotNull(message = "Le rôle est obligatoire")
    private AssignableRole role;
}
