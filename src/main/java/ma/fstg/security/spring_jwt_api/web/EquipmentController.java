package ma.fstg.security.spring_jwt_api.web;

import jakarta.validation.Valid;
import ma.fstg.security.spring_jwt_api.dto.EquipmentDto;
import ma.fstg.security.spring_jwt_api.dto.EquipmentRequest;
import ma.fstg.security.spring_jwt_api.entities.enums.EquipmentStatus;
import ma.fstg.security.spring_jwt_api.services.EquipmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public Page<EquipmentDto> list(@RequestParam(required = false) EquipmentStatus status,
                                   @RequestParam(required = false) String room,
                                   @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return equipmentService.list(status, room, pageable);
    }

    @GetMapping("/{id}")
    public EquipmentDto get(@PathVariable Long id) {
        return equipmentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EquipmentDto create(@Valid @RequestBody EquipmentRequest request) {
        return equipmentService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EquipmentDto update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return equipmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        equipmentService.delete(id);
    }
}
