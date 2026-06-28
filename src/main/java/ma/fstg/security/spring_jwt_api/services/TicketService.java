package ma.fstg.security.spring_jwt_api.services;

import ma.fstg.security.spring_jwt_api.dto.*;
import ma.fstg.security.spring_jwt_api.entities.Equipment;
import ma.fstg.security.spring_jwt_api.entities.Ticket;
import ma.fstg.security.spring_jwt_api.entities.TicketLog;
import ma.fstg.security.spring_jwt_api.entities.User;
import ma.fstg.security.spring_jwt_api.entities.enums.Priority;
import ma.fstg.security.spring_jwt_api.entities.enums.TicketStatus;
import ma.fstg.security.spring_jwt_api.exceptions.ResourceNotFoundException;
import ma.fstg.security.spring_jwt_api.repositories.EquipmentRepository;
import ma.fstg.security.spring_jwt_api.repositories.TicketLogRepository;
import ma.fstg.security.spring_jwt_api.repositories.TicketRepository;
import ma.fstg.security.spring_jwt_api.repositories.UserRepository;
import ma.fstg.security.spring_jwt_api.realtime.RealtimeEventPublisher;
import ma.fstg.security.spring_jwt_api.security.CurrentUserProvider;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final TicketLogRepository ticketLogRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public TicketService(TicketRepository ticketRepository,
                         EquipmentRepository equipmentRepository,
                         UserRepository userRepository,
                         TicketLogRepository ticketLogRepository,
                         CurrentUserProvider currentUserProvider,
                         AuditService auditService,
                         RealtimeEventPublisher realtimeEventPublisher) {
        this.ticketRepository = ticketRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.ticketLogRepository = ticketLogRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<TicketDto> list(TicketStatus status, Priority priority, String search, Pageable pageable) {
        User current = currentUserProvider.getCurrentUser();
        Long organizationId = currentUserProvider.getCurrentOrganizationId();
        boolean staff = currentUserProvider.hasRole(current, CurrentUserProvider.ROLE_ADMIN)
                || currentUserProvider.hasRole(current, CurrentUserProvider.ROLE_TECH);

        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            if (!staff) {
                predicates.add(cb.equal(root.get("user").get("id"), current.getId()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("equipment").get("name")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return ticketRepository.findAll(spec, pageable).map(TicketDto::from);
    }

    @Transactional(readOnly = true)
    public TicketDto get(Long id) {
        return TicketDto.from(findInOrganization(id));
    }

    @Transactional(readOnly = true)
    public List<TicketLogDto> logs(Long id) {
        findInOrganization(id);
        return ticketLogRepository.findByTicket_IdOrderByCreatedAtDesc(id)
                .stream().map(TicketLogDto::from).toList();
    }

    @Transactional
    public TicketDto create(CreateTicketRequest request) {
        User current = currentUserProvider.getCurrentUser();
        Long organizationId = currentUserProvider.getCurrentOrganizationId();
        Equipment equipment = equipmentRepository.findByIdAndOrganization_Id(request.getEquipmentId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable : " + request.getEquipmentId()));

        Ticket ticket = new Ticket();
        ticket.setEquipment(equipment);
        ticket.setUser(current);
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setDueDate(computeDueDate(request.getPriority()));
        ticket.setOrganization(current.getOrganization());
        ticket = ticketRepository.save(ticket);

        addLog(ticket, current, "CREATE", null, TicketStatus.OPEN.name());
        auditService.record(current, "CREATE", "TICKET", ticket.getId(), ticket.getDescription());
        TicketDto dto = TicketDto.from(ticket);
        realtimeEventPublisher.publishTicketEvent(ticket.getOrganization().getId(), "TICKET_CREATED", dto);
        return dto;
    }

    @Transactional
    public TicketDto updateStatus(Long id, UpdateStatusRequest request) {
        User current = currentUserProvider.getCurrentUser();
        Ticket ticket = findInOrganization(id);
        String oldStatus = ticket.getStatus().name();
        ticket.setStatus(request.getStatus());
        if (request.getComment() != null && !request.getComment().isBlank()) {
            ticket.setResolutionComment(request.getComment());
        }
        ticket = ticketRepository.save(ticket);

        addLog(ticket, current, "STATUS_CHANGE", oldStatus, request.getStatus().name());
        auditService.record(current, "STATUS_CHANGE", "TICKET", ticket.getId(),
                oldStatus + " -> " + request.getStatus().name());
        TicketDto dto = TicketDto.from(ticket);
        realtimeEventPublisher.publishTicketEvent(ticket.getOrganization().getId(), "TICKET_STATUS_CHANGED", dto);
        return dto;
    }

    @Transactional
    public TicketDto assign(Long id, AssignTicketRequest request) {
        User current = currentUserProvider.getCurrentUser();
        Long organizationId = currentUserProvider.getCurrentOrganizationId();
        Ticket ticket = findInOrganization(id);
        User technician = userRepository.findByIdAndOrganization_Id(request.getTechnicianId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Technicien introuvable : " + request.getTechnicianId()));

        String oldTech = ticket.getTechnician() == null ? null : ticket.getTechnician().getUsername();
        ticket.setTechnician(technician);
        ticket = ticketRepository.save(ticket);

        addLog(ticket, current, "ASSIGN", oldTech, technician.getUsername());
        auditService.record(current, "ASSIGN", "TICKET", ticket.getId(), technician.getUsername());
        TicketDto dto = TicketDto.from(ticket);
        realtimeEventPublisher.publishTicketEvent(ticket.getOrganization().getId(), "TICKET_ASSIGNED", dto);
        return dto;
    }

    @Transactional
    public TicketDto attachPhoto(Long id, String photoUrl) {
        User current = currentUserProvider.getCurrentUser();
        Ticket ticket = findInOrganization(id);
        ticket.setPhotoUrl(photoUrl);
        ticket = ticketRepository.save(ticket);

        addLog(ticket, current, "PHOTO", null, photoUrl);
        auditService.record(current, "PHOTO", "TICKET", ticket.getId(), photoUrl);
        TicketDto dto = TicketDto.from(ticket);
        realtimeEventPublisher.publishTicketEvent(ticket.getOrganization().getId(), "TICKET_PHOTO", dto);
        return dto;
    }

    private Ticket findInOrganization(Long id) {
        Long organizationId = currentUserProvider.getCurrentOrganizationId();
        return ticketRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + id));
    }

    private void addLog(Ticket ticket, User user, String action, String oldValue, String newValue) {
        TicketLog log = new TicketLog();
        log.setTicket(ticket);
        log.setUser(user);
        log.setAction(action);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        ticketLogRepository.save(log);
    }

    private LocalDateTime computeDueDate(Priority priority) {
        LocalDateTime now = LocalDateTime.now();
        return switch (priority) {
            case HIGH -> now.plusHours(24);
            case MEDIUM -> now.plusHours(72);
            case LOW -> now.plusDays(7);
        };
    }
}
