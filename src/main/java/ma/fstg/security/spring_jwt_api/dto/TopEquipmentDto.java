package ma.fstg.security.spring_jwt_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopEquipmentDto {
    private Long equipmentId;
    private String equipmentName;
    private long ticketCount;
}
