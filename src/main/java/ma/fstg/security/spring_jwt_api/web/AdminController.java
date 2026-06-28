package ma.fstg.security.spring_jwt_api.web;

import jakarta.validation.Valid;
import ma.fstg.security.spring_jwt_api.dto.ActivityLogDto;
import ma.fstg.security.spring_jwt_api.dto.OrgUserDto;
import ma.fstg.security.spring_jwt_api.dto.OrganizationSummaryDto;
import ma.fstg.security.spring_jwt_api.dto.PlatformStatsDto;
import ma.fstg.security.spring_jwt_api.dto.TicketDto;
import ma.fstg.security.spring_jwt_api.dto.UpdateActiveRequest;
import ma.fstg.security.spring_jwt_api.services.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/organizations")
    public Page<OrganizationSummaryDto> organizations(@RequestParam(required = false) Boolean active,
                                                      @RequestParam(required = false) String search,
                                                      @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return adminService.listOrganizations(active, search, pageable);
    }

    @PatchMapping("/organizations/{id}/status")
    public OrganizationSummaryDto setOrganizationStatus(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateActiveRequest request) {
        return adminService.setOrganizationStatus(id, request);
    }

    @GetMapping("/organizations/{id}")
    public OrganizationSummaryDto organization(@PathVariable Long id) {
        return adminService.getOrganization(id);
    }

    @GetMapping("/organizations/{id}/users")
    public List<OrgUserDto> organizationUsers(@PathVariable Long id) {
        return adminService.organizationUsers(id);
    }

    @GetMapping("/organizations/{id}/tickets")
    public Page<TicketDto> organizationTickets(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminService.organizationTickets(id, pageable);
    }

    @GetMapping("/stats")
    public PlatformStatsDto stats() {
        return adminService.platformStats();
    }

    @GetMapping("/activity")
    public List<ActivityLogDto> recentActivity() {
        return adminService.recentActivity();
    }
}
