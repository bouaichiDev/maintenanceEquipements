package ma.fstg.security.spring_jwt_api.services;

import ma.fstg.security.spring_jwt_api.dto.ActivityLogDto;
import ma.fstg.security.spring_jwt_api.dto.OrgUserDto;
import ma.fstg.security.spring_jwt_api.dto.OrganizationSummaryDto;
import ma.fstg.security.spring_jwt_api.dto.PlatformStatsDto;
import ma.fstg.security.spring_jwt_api.dto.TicketDto;
import ma.fstg.security.spring_jwt_api.dto.UpdateActiveRequest;
import ma.fstg.security.spring_jwt_api.entities.Organization;
import ma.fstg.security.spring_jwt_api.exceptions.ResourceNotFoundException;
import ma.fstg.security.spring_jwt_api.repositories.ActivityLogRepository;
import ma.fstg.security.spring_jwt_api.repositories.EquipmentRepository;
import ma.fstg.security.spring_jwt_api.repositories.OrganizationRepository;
import ma.fstg.security.spring_jwt_api.repositories.TicketRepository;
import ma.fstg.security.spring_jwt_api.repositories.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final EquipmentRepository equipmentRepository;
    private final ActivityLogRepository activityLogRepository;

    public AdminService(OrganizationRepository organizationRepository,
                        UserRepository userRepository,
                        TicketRepository ticketRepository,
                        EquipmentRepository equipmentRepository,
                        ActivityLogRepository activityLogRepository) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.equipmentRepository = equipmentRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityLogDto> recentActivity() {
        return activityLogRepository
                .findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ActivityLogDto::from)
                .getContent();
    }

    @Transactional(readOnly = true)
    public Page<OrganizationSummaryDto> listOrganizations(Boolean active, String search, Pageable pageable) {
        Specification<Organization> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return organizationRepository.findAll(spec, pageable).map(org -> new OrganizationSummaryDto(
                org.getId(),
                org.getName(),
                org.isActive(),
                org.getCreatedAt(),
                userRepository.countByOrganization_Id(org.getId()),
                ticketRepository.countByOrganization_Id(org.getId()),
                equipmentRepository.countByOrganization_Id(org.getId())));
    }

    @Transactional
    public OrganizationSummaryDto setOrganizationStatus(Long id, UpdateActiveRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + id));
        org.setActive(request.getActive());
        organizationRepository.save(org);
        return new OrganizationSummaryDto(
                org.getId(),
                org.getName(),
                org.isActive(),
                org.getCreatedAt(),
                userRepository.countByOrganization_Id(org.getId()),
                ticketRepository.countByOrganization_Id(org.getId()),
                equipmentRepository.countByOrganization_Id(org.getId()));
    }

    @Transactional(readOnly = true)
    public OrganizationSummaryDto getOrganization(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + id));
        return new OrganizationSummaryDto(
                org.getId(),
                org.getName(),
                org.isActive(),
                org.getCreatedAt(),
                userRepository.countByOrganization_Id(id),
                ticketRepository.countByOrganization_Id(id),
                equipmentRepository.countByOrganization_Id(id));
    }

    @Transactional(readOnly = true)
    public List<OrgUserDto> organizationUsers(Long id) {
        return userRepository.findByOrganization_Id(id).stream().map(OrgUserDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<TicketDto> organizationTickets(Long id, Pageable pageable) {
        return ticketRepository.findByOrganization_Id(id, pageable).map(TicketDto::from);
    }

    @Transactional(readOnly = true)
    public PlatformStatsDto platformStats() {
        long totalOrganizations = organizationRepository.count();
        long activeOrganizations = organizationRepository.findAll().stream()
                .filter(Organization::isActive).count();
        long totalUsers = userRepository.count();
        long totalTickets = ticketRepository.count();
        return new PlatformStatsDto(totalOrganizations, activeOrganizations, totalUsers, totalTickets);
    }
}
