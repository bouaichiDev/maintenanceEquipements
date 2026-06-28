package ma.fstg.security.spring_jwt_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlatformStatsDto {
    private long totalOrganizations;
    private long activeOrganizations;
    private long totalUsers;
    private long totalTickets;
}
