package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.NotificationDto;
import cl.duoc.vidasalud.bff.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para consultar y gestionar notificaciones de negocio en VidaSalud.
 * Los eventos son originados por RabbitMQ (topic vidasalud.notificacion.*)
 * y persistidos por el consumidor NotificationAuditConsumer.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Paciente', 'Auditor')")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(Authentication authentication) {
        boolean isStaffOrAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_Admin") || a.equals("ROLE_Recepcionista") || a.equals("ROLE_Auditor"));

        if (isStaffOrAdmin) {
            return ResponseEntity.ok(notificationService.findAll());
        }

        // Si es rol Paciente exclusivamente, filtrar por el nombre o identificador del usuario
        String username = authentication.getName();
        return ResponseEntity.ok(notificationService.findByPatientName(username));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable String id) {
        boolean updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("id", id, "read", updated));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        int count = notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("markedCount", count));
    }
}
