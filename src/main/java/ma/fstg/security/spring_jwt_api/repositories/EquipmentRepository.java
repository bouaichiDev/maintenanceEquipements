package ma.fstg.security.spring_jwt_api.repositories;

import ma.fstg.security.spring_jwt_api.entities.Equipment;
import ma.fstg.security.spring_jwt_api.entities.enums.EquipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Page<Equipment> findByOrganization_Id(Long organizationId, Pageable pageable);

    Page<Equipment> findByOrganization_IdAndStatus(Long organizationId, EquipmentStatus status, Pageable pageable);

    Page<Equipment> findByOrganization_IdAndRoomContainingIgnoreCase(Long organizationId, String room, Pageable pageable);

    List<Equipment> findByOrganization_Id(Long organizationId);

    Optional<Equipment> findByIdAndOrganization_Id(Long id, Long organizationId);

    long countByOrganization_Id(Long organizationId);
}
