package ma.fstg.security.spring_jwt_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ma.fstg.security.spring_jwt_api.entities.ActivityLog;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ActivityLogDto {

    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;

    public static ActivityLogDto from(ActivityLog log) {
        return new ActivityLogDto(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getUser() == null ? null : log.getUser().getId(),
                log.getUser() == null ? null : log.getUser().getUsername(),
                log.getCreatedAt());
    }
}
