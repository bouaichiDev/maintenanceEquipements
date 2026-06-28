package ma.fstg.security.spring_jwt_api.services;

import ma.fstg.security.spring_jwt_api.dto.EquipmentDto;
import ma.fstg.security.spring_jwt_api.dto.EquipmentRequest;
import ma.fstg.security.spring_jwt_api.entities.Equipment;
import ma.fstg.security.spring_jwt_api.entities.User;
import ma.fstg.security.spring_jwt_api.entities.enums.EquipmentStatus;
import ma.fstg.security.spring_jwt_api.exceptions.ResourceNotFoundException;
import ma.fstg.security.spring_jwt_api.repositories.EquipmentRepository;
import ma.fstg.security.spring_jwt_api.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    public EquipmentService(EquipmentRepository equipmentRepository,
                            CurrentUserProvider currentUserProvider,
                            AuditService auditService) {
        this.equipmentRepository = equipmentRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<EquipmentDto> list(EquipmentStatus status, String room, Pageable pageable) {
        Long organizationId = currentUserProvider.getCurrentOrganizationId();
        Page<Equipment> page;
        if (status != null) {
            page = equipmentRepository.findByOrganization_IdAndStatus(organizationId, status, pageable);
        } else if (room != null && !room.isBlank()) {
            page = equipmentRepository.findByOrganization_IdAndRoomContainingIgnoreCase(organizationId, room, pageable);
        } else {
            page = equipmentRepository.findByOrganization_Id(organizationId, pageable);
        }
        return page.map(EquipmentDto::from);
    }

    @Transactional(readOnly = true)
    public EquipmentDto get(Long id) {
        return EquipmentDto.from(findInOrganization(id));
    }

    @Transactional
    public EquipmentDto create(EquipmentRequest request) {
        User current = currentUserProvider.getCurrentUser();
        Equipment equipment = new Equipment();
        equipment.setName(request.getName());
        equipment.setRoom(request.getRoom());
        equipment.setType(request.getType());
        equipment.setStatus(request.getStatus() == null ? EquipmentStatus.OPERATIONAL : request.getStatus());
        equipment.setOrganization(current.getOrganization());
        Equipment saved = equipmentRepository.save(equipment);
        auditService.record(current, "CREATE", "EQUIPMENT", saved.getId(), saved.getName());
        return EquipmentDto.from(saved);
    }

    @Transactional
    public EquipmentDto update(Long id, EquipmentRequest request) {
        Equipment equipment = findInOrganization(id);
        equipment.setName(request.getName());
        equipment.setRoom(request.getRoom());
        equipment.setType(request.getType());
        if (request.getStatus() != null) {
            equipment.setStatus(request.getStatus());
        }
        Equipment saved = equipmentRepository.save(equipment);
        auditService.record(currentUserProvider.getCurrentUser(), "UPDATE", "EQUIPMENT", saved.getId(), saved.getName());
        return EquipmentDto.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Equipment equipment = findInOrganization(id);
        String name = equipment.getName();
        equipmentRepository.delete(equipment);
        auditService.record(currentUserProvider.getCurrentUser(), "DELETE", "EQUIPMENT", id, name);
    }

    private Equipment findInOrganization(Long id) {
        Long organizationId = currentUserProvider.getCurrentOrganizationId();
        return equipmentRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable : " + id));
    }
}
