package ma.fstg.security.spring_jwt_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrganizationSummaryDto {
    private Long id;
    private String name;
    private boolean active;
    private LocalDateTime createdAt;
    private long userCount;
    private long ticketCount;
    private long equipmentCount;
}
