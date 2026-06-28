package ma.fstg.security.spring_jwt_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ma.fstg.security.spring_jwt_api.entities.enums.Priority;

@Data
public class CreateTicketRequest {

    @NotNull(message = "L'équipement est obligatoire")
    private Long equipmentId;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull(message = "La priorité est obligatoire")
    private Priority priority;
}
