package ma.fstg.security.spring_jwt_api.web;

import ma.fstg.security.spring_jwt_api.dto.ActivityLogDto;
import ma.fstg.security.spring_jwt_api.services.ActivityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH')")
    public Page<ActivityLogDto> list(@RequestParam(required = false) String type,
                                     @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return activityService.list(type, pageable);
    }
}
