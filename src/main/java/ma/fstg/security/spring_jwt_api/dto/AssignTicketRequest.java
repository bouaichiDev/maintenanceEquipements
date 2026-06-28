package ma.fstg.security.spring_jwt_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignTicketRequest {

    @NotNull(message = "Le technicien est obligatoire")
    private Long technicianId;
}
