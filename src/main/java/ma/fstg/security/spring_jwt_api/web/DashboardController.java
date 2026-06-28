package ma.fstg.security.spring_jwt_api.web;

import ma.fstg.security.spring_jwt_api.dto.DashboardStatsDto;
import ma.fstg.security.spring_jwt_api.dto.PersonalDashboardDto;
import ma.fstg.security.spring_jwt_api.services.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('TECH', 'ADMIN')")
    public DashboardStatsDto stats() {
        return dashboardService.stats();
    }

    @GetMapping("/my")
    public PersonalDashboardDto myDashboard() {
        return dashboardService.myDashboard();
    }
}
