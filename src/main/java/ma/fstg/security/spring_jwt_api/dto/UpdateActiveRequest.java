package ma.fstg.security.spring_jwt_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateActiveRequest {

    @NotNull(message = "Le statut actif est obligatoire")
    private Boolean active;
}
