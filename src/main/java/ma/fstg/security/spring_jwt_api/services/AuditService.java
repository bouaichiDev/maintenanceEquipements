package ma.fstg.security.spring_jwt_api.services;

import ma.fstg.security.spring_jwt_api.dto.ActivityLogDto;
import ma.fstg.security.spring_jwt_api.entities.ActivityLog;
import ma.fstg.security.spring_jwt_api.entities.Organization;
import ma.fstg.security.spring_jwt_api.entities.User;
import ma.fstg.security.spring_jwt_api.realtime.RealtimeEventPublisher;
import ma.fstg.security.spring_jwt_api.repositories.ActivityLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final ActivityLogRepository activityLogRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public AuditService(ActivityLogRepository activityLogRepository,
                        RealtimeEventPublisher realtimeEventPublisher) {
        this.activityLogRepository = activityLogRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    public void record(User user, String action, String entityType, Long entityId, String details) {
        Organization organization = user == null ? null : user.getOrganization();
        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setOrganization(organization);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        ActivityLog saved = activityLogRepository.save(log);

        if (organization != null) {
            realtimeEventPublisher.publishActivity(organization.getId(), ActivityLogDto.from(saved));
        }
    }
}
