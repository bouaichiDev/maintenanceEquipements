package ma.fstg.security.spring_jwt_api.services;

import jakarta.persistence.criteria.Predicate;
import ma.fstg.security.spring_jwt_api.dto.ActivityLogDto;
import ma.fstg.security.spring_jwt_api.entities.ActivityLog;
import ma.fstg.security.spring_jwt_api.repositories.ActivityLogRepository;
import ma.fstg.security.spring_jwt_api.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ActivityService {

    private final ActivityLogRepository activityLogRepository;
    private final CurrentUserProvider currentUserProvider;

    public ActivityService(ActivityLogRepository activityLogRepository,
                           CurrentUserProvider currentUserProvider) {
        this.activityLogRepository = activityLogRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDto> list(String entityType, Pageable pageable) {
        Long organizationId = currentUserProvider.getCurrentOrganizationId();

        Specification<ActivityLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType.toUpperCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return activityLogRepository.findAll(spec, pageable).map(ActivityLogDto::from);
    }
}
