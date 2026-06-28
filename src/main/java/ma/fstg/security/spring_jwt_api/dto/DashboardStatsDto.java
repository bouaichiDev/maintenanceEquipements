package ma.fstg.security.spring_jwt_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DashboardStatsDto {

    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long closedTickets;
    private long highPriority;
    private long mediumPriority;
    private long lowPriority;
    private long totalEquipments;
    private long overdueTickets;
    private List<TopEquipmentDto> topEquipments;
}
