package ma.fstg.security.spring_jwt_api.web;

import jakarta.validation.Valid;
import ma.fstg.security.spring_jwt_api.dto.CreateUserRequest;
import ma.fstg.security.spring_jwt_api.dto.OrgUserDto;
import ma.fstg.security.spring_jwt_api.dto.UpdateActiveRequest;
import ma.fstg.security.spring_jwt_api.dto.UpdateUserRoleRequest;
import ma.fstg.security.spring_jwt_api.services.OrgUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/org/users")
@PreAuthorize("hasRole('ADMIN')")
public class OrgUserController {

    private final OrgUserService orgUserService;

    public OrgUserController(OrgUserService orgUserService) {
        this.orgUserService = orgUserService;
    }

    @GetMapping
    public Page<OrgUserDto> list(@RequestParam(required = false) String role,
                                 @RequestParam(required = false) Boolean active,
                                 @RequestParam(required = false) String search,
                                 @PageableDefault(size = 10, sort = "username") Pageable pageable) {
        return orgUserService.list(role, active, search, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrgUserDto create(@Valid @RequestBody CreateUserRequest request) {
        return orgUserService.create(request);
    }

    @PatchMapping("/{id}/role")
    public OrgUserDto updateRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return orgUserService.updateRole(id, request);
    }

    @PatchMapping("/{id}/status")
    public OrgUserDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateActiveRequest request) {
        return orgUserService.updateStatus(id, request);
    }
}
