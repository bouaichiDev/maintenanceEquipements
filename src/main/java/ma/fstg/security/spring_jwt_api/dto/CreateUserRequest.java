package ma.fstg.security.spring_jwt_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 4, message = "Le mot de passe doit contenir au moins 4 caractères")
    private String password;

    @NotNull(message = "Le rôle est obligatoire")
    private AssignableRole role;
}
