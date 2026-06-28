package ma.fstg.security.spring_jwt_api.web;

import jakarta.validation.Valid;
import ma.fstg.security.spring_jwt_api.dto.*;
import ma.fstg.security.spring_jwt_api.entities.enums.Priority;
import ma.fstg.security.spring_jwt_api.entities.enums.TicketStatus;
import ma.fstg.security.spring_jwt_api.services.FileStorageService;
import ma.fstg.security.spring_jwt_api.services.TicketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final FileStorageService fileStorageService;

    public TicketController(TicketService ticketService, FileStorageService fileStorageService) {
        this.ticketService = ticketService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public Page<TicketDto> list(@RequestParam(required = false) TicketStatus status,
                                @RequestParam(required = false) Priority priority,
                                @RequestParam(required = false) String search,
                                @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ticketService.list(status, priority, search, pageable);
    }

    @GetMapping("/{id}")
    public TicketDto get(@PathVariable Long id) {
        return ticketService.get(id);
    }

    @GetMapping("/{id}/logs")
    public List<TicketLogDto> logs(@PathVariable Long id) {
        return ticketService.logs(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto create(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.create(request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TECH', 'ADMIN')")
    public TicketDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return ticketService.updateStatus(id, request);
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketDto assign(@PathVariable Long id, @Valid @RequestBody AssignTicketRequest request) {
        return ticketService.assign(id, request);
    }

    @PostMapping("/{id}/photo")
    public TicketDto uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file);
        return ticketService.attachPhoto(id, url);
    }
}
