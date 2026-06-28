package ma.fstg.security.spring_jwt_api.services;

import ma.fstg.security.spring_jwt_api.dto.AuthResponse;
import ma.fstg.security.spring_jwt_api.dto.LoginRequest;
import ma.fstg.security.spring_jwt_api.dto.RegisterRequest;
import ma.fstg.security.spring_jwt_api.entities.Organization;
import ma.fstg.security.spring_jwt_api.entities.Role;
import ma.fstg.security.spring_jwt_api.entities.User;
import ma.fstg.security.spring_jwt_api.exceptions.AccessDeniedException;
import ma.fstg.security.spring_jwt_api.exceptions.BadRequestException;
import ma.fstg.security.spring_jwt_api.jwt.JwtUtil;
import ma.fstg.security.spring_jwt_api.repositories.OrganizationRepository;
import ma.fstg.security.spring_jwt_api.repositories.RoleRepository;
import ma.fstg.security.spring_jwt_api.repositories.UserRepository;
import ma.fstg.security.spring_jwt_api.security.CurrentUserProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Ce nom d'utilisateur est déjà utilisé");
        }

        Organization organization = new Organization();
        organization.setName(request.getOrganizationName());
        organization.setActive(true);
        organization = organizationRepository.save(organization);

        Role adminRole = roleRepository.findByName(CurrentUserProvider.ROLE_ADMIN);
        if (adminRole == null) {
            throw new BadRequestException("Rôle ADMIN introuvable");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        user.setOrganization(organization);
        user.setRoles(List.of(adminRole));
        userRepository.save(user);

        auditService.record(user, "REGISTER", "AUTH", user.getId(),
                "Création de l'organisation " + organization.getName());

        String token = jwtUtil.generateToken(user.getUsername(), organization.getId(), CurrentUserProvider.ROLE_ADMIN);
        return new AuthResponse(token, user.getUsername(), CurrentUserProvider.ROLE_ADMIN,
                organization.getId(), organization.getName());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadRequestException("Nom d'utilisateur ou mot de passe incorrect");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Nom d'utilisateur ou mot de passe incorrect"));

        Organization organization = user.getOrganization();
        if (organization != null && !organization.isActive()) {
            throw new AccessDeniedException("L'organisation de ce compte est suspendue");
        }

        String role = primaryRole(user);
        Long organizationId = organization == null ? null : organization.getId();
        String organizationName = organization == null ? null : organization.getName();

        auditService.record(user, "LOGIN", "AUTH", user.getId(), null);

        String token = jwtUtil.generateToken(user.getUsername(), organizationId, role);
        return new AuthResponse(token, user.getUsername(), role, organizationId, organizationName);
    }

    private String primaryRole(User user) {
        List<String> priority = List.of(
                CurrentUserProvider.ROLE_SUPER_ADMIN,
                CurrentUserProvider.ROLE_ADMIN,
                CurrentUserProvider.ROLE_TECH,
                CurrentUserProvider.ROLE_USER);
        for (String role : priority) {
            boolean has = user.getRoles().stream().anyMatch(r -> role.equals(r.getName()));
            if (has) {
                return role;
            }
        }
        return CurrentUserProvider.ROLE_USER;
    }
}
