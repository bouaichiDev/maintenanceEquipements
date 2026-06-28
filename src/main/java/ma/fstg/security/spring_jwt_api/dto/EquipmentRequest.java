package ma.fstg.security.spring_jwt_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import ma.fstg.security.spring_jwt_api.entities.enums.EquipmentStatus;

@Data
public class EquipmentRequest {

    @NotBlank(message = "Le nom de l'équipement est obligatoire")
    private String name;

    private String room;

    private String type;

    private EquipmentStatus status;
}
