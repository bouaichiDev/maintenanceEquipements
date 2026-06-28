package ma.fstg.security.spring_jwt_api.services;

import ma.fstg.security.spring_jwt_api.dto.DashboardStatsDto;
import ma.fstg.security.spring_jwt_api.dto.PersonalDashboardDto;
import ma.fstg.security.spring_jwt_api.dto.TopEquipmentDto;
import ma.fstg.security.spring_jwt_api.entities.Ticket;
import ma.fstg.security.spring_jwt_api.entities.User;
import ma.fstg.security.spring_jwt_api.entities.enums.Priority;
import ma.fstg.security.spring_jwt_api.entities.enums.TicketStatus;
import ma.fstg.security.spring_jwt_api.repositories.EquipmentRepository;
import ma.fstg.security.spring_jwt_api.repositories.TicketRepository;
import ma.fstg.security.spring_jwt_api.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final EquipmentRepository equipmentRepository;
    private final CurrentUserProvider currentUserProvider;

    public DashboardService(TicketRepository ticketRepository,
                            EquipmentRepository equipmentRepository,
                            CurrentUserProvider currentUserProvider) {
        this.ticketRepository = ticketRepository;
        this.equipmentRepository = equipmentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto stats() {
        Long organizationId = currentUserProvider.getCurrentOrganizationId();

        long total = ticketRepository.countByOrganization_Id(organizationId);
        long open = ticketRepository.countByOrganization_IdAndStatus(organizationId, TicketStatus.OPEN);
        long inProgress = ticketRepository.countByOrganization_IdAndStatus(organizationId, TicketStatus.IN_PROGRESS);
        long resolved = ticketRepository.countByOrganization_IdAndStatus(organizationId, TicketStatus.RESOLVED);
        long closed = ticketRepository.countByOrganization_IdAndStatus(organizationId, TicketStatus.CLOSED);
        long high = ticketRepository.countByOrganization_IdAndPriority(organizationId, Priority.HIGH);
        long medium = ticketRepository.countByOrganization_IdAndPriority(organizationId, Priority.MEDIUM);
        long low = ticketRepository.countByOrganization_IdAndPriority(organizationId, Priority.LOW);
        long equipments = equipmentRepository.countByOrganization_Id(organizationId);

        List<Ticket> all = ticketRepository.findByOrganization_Id(organizationId);
        LocalDateTime now = LocalDateTime.now();
        long overdue = all.stream().filter(t -> isOverdue(t, now)).count();

        List<TopEquipmentDto> topEquipments = topEquipments(all);

        return new DashboardStatsDto(total, open, inProgress, resolved, closed,
                high, medium, low, equipments, overdue, topEquipments);
    }

    @Transactional(readOnly = true)
    public PersonalDashboardDto myDashboard() {
        User current = currentUserProvider.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        List<Ticket> created = ticketRepository.findByUser_Id(current.getId());
        long createdTotal = created.size();
        long createdOpen = countStatus(created, TicketStatus.OPEN);
        long createdInProgress = countStatus(created, TicketStatus.IN_PROGRESS);
        long createdResolved = countStatus(created, TicketStatus.RESOLVED);
        long createdClosed = countStatus(created, TicketStatus.CLOSED);
        long createdOverdue = created.stream().filter(t -> isOverdue(t, now)).count();

        String role = primaryRole(current);
        boolean staff = currentUserProvider.hasRole(current, CurrentUserProvider.ROLE_ADMIN)
                || currentUserProvider.hasRole(current, CurrentUserProvider.ROLE_TECH);

        Long assignedTotal = null;
        Long assignedOpen = null;
        Long assignedInProgress = null;
        Long assignedResolved = null;
        Long assignedOverdue = null;
        Long unassignedOpen = null;

        if (staff && current.getOrganization() != null) {
            List<Ticket> assigned = ticketRepository.findByTechnician_Id(current.getId());
            assignedTotal = (long) assigned.size();
            assignedOpen = countStatus(assigned, TicketStatus.OPEN);
            assignedInProgress = countStatus(assigned, TicketStatus.IN_PROGRESS);
            assignedResolved = countStatus(assigned, TicketStatus.RESOLVED);
            assignedOverdue = assigned.stream().filter(t -> isOverdue(t, now)).count();
            unassignedOpen = ticketRepository.countByOrganization_IdAndTechnicianIsNullAndStatus(
                    current.getOrganization().getId(), TicketStatus.OPEN);
        }

        return new PersonalDashboardDto(role, createdTotal, createdOpen, createdInProgress,
                createdResolved, createdClosed, createdOverdue,
                assignedTotal, assignedOpen, assignedInProgress, assignedResolved,
                assignedOverdue, unassignedOpen);
    }

    private long countStatus(List<Ticket> tickets, TicketStatus status) {
        return tickets.stream().filter(t -> t.getStatus() == status).count();
    }

    private String primaryRole(User user) {
        List<String> priority = List.of(
                CurrentUserProvider.ROLE_SUPER_ADMIN,
                CurrentUserProvider.ROLE_ADMIN,
                CurrentUserProvider.ROLE_TECH,
                CurrentUserProvider.ROLE_USER);
        for (String role : priority) {
            if (currentUserProvider.hasRole(user, role)) {
                return role;
            }
        }
        return CurrentUserProvider.ROLE_USER;
    }

    private boolean isOverdue(Ticket ticket, LocalDateTime now) {
        return ticket.getDueDate() != null
                && ticket.getStatus() != TicketStatus.RESOLVED
                && ticket.getStatus() != TicketStatus.CLOSED
                && ticket.getDueDate().isBefore(now);
    }

    private List<TopEquipmentDto> topEquipments(List<Ticket> tickets) {
        Map<Long, TopEquipmentDto> counts = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            if (ticket.getEquipment() == null) {
                continue;
            }
            Long id = ticket.getEquipment().getId();
            String name = ticket.getEquipment().getName();
            TopEquipmentDto current = counts.get(id);
            if (current == null) {
                counts.put(id, new TopEquipmentDto(id, name, 1));
            } else {
                current.setTicketCount(current.getTicketCount() + 1);
            }
        }
        return counts.values().stream()
                .sorted(Comparator.comparingLong(TopEquipmentDto::getTicketCount).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }
}
