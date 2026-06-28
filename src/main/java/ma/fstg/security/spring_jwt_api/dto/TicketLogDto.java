package ma.fstg.security.spring_jwt_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ma.fstg.security.spring_jwt_api.entities.TicketLog;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TicketLogDto {

    private Long id;
    private String action;
    private String oldValue;
    private String newValue;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;

    public static TicketLogDto from(TicketLog log) {
        return new TicketLogDto(
                log.getId(),
                log.getAction(),
                log.getOldValue(),
                log.getNewValue(),
                log.getUser() == null ? null : log.getUser().getId(),
                log.getUser() == null ? null : log.getUser().getUsername(),
                log.getCreatedAt());
    }
}
