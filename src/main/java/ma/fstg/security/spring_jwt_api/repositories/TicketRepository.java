package ma.fstg.security.spring_jwt_api.repositories;

import ma.fstg.security.spring_jwt_api.entities.Ticket;
import ma.fstg.security.spring_jwt_api.entities.enums.Priority;
import ma.fstg.security.spring_jwt_api.entities.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Page<Ticket> findByOrganization_Id(Long organizationId, Pageable pageable);

    Page<Ticket> findByOrganization_IdAndStatus(Long organizationId, TicketStatus status, Pageable pageable);

    Page<Ticket> findByOrganization_IdAndPriority(Long organizationId, Priority priority, Pageable pageable);

    Page<Ticket> findByOrganization_IdAndUser_Id(Long organizationId, Long userId, Pageable pageable);

    Optional<Ticket> findByIdAndOrganization_Id(Long id, Long organizationId);

    List<Ticket> findByOrganization_Id(Long organizationId);

    long countByOrganization_Id(Long organizationId);

    long countByOrganization_IdAndStatus(Long organizationId, TicketStatus status);

    long countByOrganization_IdAndPriority(Long organizationId, Priority priority);

    List<Ticket> findByUser_Id(Long userId);

    List<Ticket> findByTechnician_Id(Long technicianId);

    long countByOrganization_IdAndTechnicianIsNullAndStatus(Long organizationId, TicketStatus status);
}
