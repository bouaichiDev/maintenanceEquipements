package ma.fstg.security.spring_jwt_api.services;

import ma.fstg.security.spring_jwt_api.dto.CreateUserRequest;
import ma.fstg.security.spring_jwt_api.dto.OrgUserDto;
import ma.fstg.security.spring_jwt_api.dto.UpdateActiveRequest;
import ma.fstg.security.spring_jwt_api.dto.UpdateUserRoleRequest;
import ma.fstg.security.spring_jwt_api.entities.Role;
import ma.fstg.security.spring_jwt_api.entities.User;
import ma.fstg.security.spring_jwt_api.exceptions.BadRequestException;
import ma.fstg.security.spring_jwt_api.exceptions.ResourceNotFoundException;
import ma.fstg.security.spring_jwt_api.repositories.RoleRepository;
import ma.fstg.security.spring_jwt_api.repositories.UserRepository;
import ma.fstg.security.spring_jwt_api.security.CurrentUserProvider;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrgUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    public OrgUserService(UserRepository userRepository,
                          RoleRepository roleRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          CurrentUserProvider currentUserProvider,
                          AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<OrgUserDto> list(String role, Boolean active, String search, Pageable pageable) {
        Long organizationId = currentUserProvider.getCurrentOrganizationId();

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + search.toLowerCase() + "%"));
            }
            if (role != null && !role.isBlank()) {
                query.distinct(true);
                Join<Object, Object> roles = root.join("roles");
                predicates.add(cb.equal(roles.get("name"), role));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(OrgUserDto::from);
    }

    @Transactional
    public OrgUserDto create(CreateUserRequest request) {
        User admin = currentUserProvider.getCurrentUser();
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Ce nom d'utilisateur est déjà utilisé");
        }
        Role role = resolveRole(request.getRole().authority());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        user.setOrganization(admin.getOrganization());
        user.setRoles(List.of(role));
        user = userRepository.save(user);

        auditService.record(admin, "CREATE", "USER", user.getId(), user.getUsername());
        return OrgUserDto.from(user);
    }

    @Transactional
    public OrgUserDto updateRole(Long id, UpdateUserRoleRequest request) {
        User admin = currentUserProvider.getCurrentUser();
        User user = findInOrganization(id);
        Role role = resolveRole(request.getRole().authority());
        user.setRoles(List.of(role));
        user = userRepository.save(user);

        auditService.record(admin, "UPDATE_ROLE", "USER", user.getId(), role.getName());
        return OrgUserDto.from(user);
    }

    @Transactional
    public OrgUserDto updateStatus(Long id, UpdateActiveRequest request) {
        User admin = currentUserProvider.getCurrentUser();
        User user = findInOrganization(id);
        user.setActive(request.getActive());
        user = userRepository.save(user);

        auditService.record(admin, "UPDATE_STATUS", "USER", user.getId(), String.valueOf(request.getActive()));
        return OrgUserDto.from(user);
    }

    private User findInOrganization(Long id) {
        Long organizationId = currentUserProvider.getCurrentOrganizationId();
        return userRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }

    private Role resolveRole(String authority) {
        Role role = roleRepository.findByName(authority);
        if (role == null) {
            throw new BadRequestException("Rôle introuvable : " + authority);
        }
        return role;
    }
}
