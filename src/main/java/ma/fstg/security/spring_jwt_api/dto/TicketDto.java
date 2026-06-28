package ma.fstg.security.spring_jwt_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ma.fstg.security.spring_jwt_api.entities.Ticket;
import ma.fstg.security.spring_jwt_api.entities.enums.Priority;
import ma.fstg.security.spring_jwt_api.entities.enums.TicketStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TicketDto {

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private Long userId;
    private String userName;
    private Long technicianId;
    private String technicianName;
    private String description;
    private Priority priority;
    private TicketStatus status;
    private String photoUrl;
    private String resolutionComment;
    private LocalDateTime dueDate;
    private boolean overdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketDto from(Ticket ticket) {
        boolean overdue = ticket.getDueDate() != null
                && ticket.getStatus() != TicketStatus.RESOLVED
                && ticket.getStatus() != TicketStatus.CLOSED
                && ticket.getDueDate().isBefore(LocalDateTime.now());

        return new TicketDto(
                ticket.getId(),
                ticket.getEquipment() == null ? null : ticket.getEquipment().getId(),
                ticket.getEquipment() == null ? null : ticket.getEquipment().getName(),
                ticket.getUser() == null ? null : ticket.getUser().getId(),
                ticket.getUser() == null ? null : ticket.getUser().getUsername(),
                ticket.getTechnician() == null ? null : ticket.getTechnician().getId(),
                ticket.getTechnician() == null ? null : ticket.getTechnician().getUsername(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getPhotoUrl(),
                ticket.getResolutionComment(),
                ticket.getDueDate(),
                overdue,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
