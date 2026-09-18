package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.AppointmentDto;
import cl.duoc.vidasalud.bff.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestión de appointments (citas médicas).
 * Ahora utiliza AppointmentService para obtener datos desde PostgreSQL RDS.
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Obtiene todos los appointments desde la base de datos PostgreSQL.
     * Roles permitidos: Admin, Recepcionista, Paciente
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Paciente')")
    public ResponseEntity<List<AppointmentDto>> getAllAppointments() {
        log.info("GET /api/appointments - Consultando appointments desde PostgreSQL");
        List<AppointmentDto> appointments = appointmentService.getAllAppointments();
        return ResponseEntity.ok(appointments);
    }

    /**
     * Obtiene un appointment por su ID desde la base de datos.
     * Roles permitidos: Admin, Recepcionista, Paciente
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Paciente')")
    public ResponseEntity<AppointmentDto> getAppointmentById(@PathVariable String id) {
        log.info("GET /api/appointments/{} - Buscando appointment", id);
        return appointmentService.getAppointmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Confirma un appointment actualizando su estado en la base de datos.
     * Roles permitidos: Admin, Recepcionista
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('Admin', 'Recepcionista')")
    public ResponseEntity<AppointmentDto> confirmAppointment(@PathVariable String id) {
        log.info("POST /api/appointments/{}/confirm - Confirmando appointment", id);
        return appointmentService.confirmAppointment(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
