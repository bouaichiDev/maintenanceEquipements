package ma.fstg.security.spring_jwt_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ma.fstg.security.spring_jwt_api.entities.Equipment;
import ma.fstg.security.spring_jwt_api.entities.enums.EquipmentStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EquipmentDto {

    private Long id;
    private String name;
    private String room;
    private String type;
    private EquipmentStatus status;
    private LocalDateTime createdAt;

    public static EquipmentDto from(Equipment equipment) {
        return new EquipmentDto(
                equipment.getId(),
                equipment.getName(),
                equipment.getRoom(),
                equipment.getType(),
                equipment.getStatus(),
                equipment.getCreatedAt());
    }
}
