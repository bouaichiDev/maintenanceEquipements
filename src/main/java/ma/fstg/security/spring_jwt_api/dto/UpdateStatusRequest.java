package ma.fstg.security.spring_jwt_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ma.fstg.security.spring_jwt_api.entities.enums.TicketStatus;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Le statut est obligatoire")
    private TicketStatus status;

    private String comment;
}
