package ma.fstg.security.spring_jwt_api.repositories;

import ma.fstg.security.spring_jwt_api.entities.TicketLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketLogRepository extends JpaRepository<TicketLog, Long> {
    List<TicketLog> findByTicket_IdOrderByCreatedAtDesc(Long ticketId);
}
