package ma.fstg.security.spring_jwt_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PersonalDashboardDto {

    private String role;

    private long createdTotal;
    private long createdOpen;
    private long createdInProgress;
    private long createdResolved;
    private long createdClosed;
    private long createdOverdue;

    private Long assignedTotal;
    private Long assignedOpen;
    private Long assignedInProgress;
    private Long assignedResolved;
    private Long assignedOverdue;
    private Long unassignedOpen;
}
