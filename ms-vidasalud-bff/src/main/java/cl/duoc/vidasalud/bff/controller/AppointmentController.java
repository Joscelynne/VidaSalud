package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.AppointmentDto;
import cl.duoc.vidasalud.bff.event.AppointmentConfirmedEvent;
import cl.duoc.vidasalud.bff.producer.AppointmentEventProducer;
import cl.duoc.vidasalud.bff.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

        private final AppointmentEventProducer appointmentEventProducer;
        private final AppointmentService appointmentService;

        @GetMapping
        @PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Paciente')")
        public ResponseEntity<List<AppointmentDto>> getAllAppointments() {
                return ResponseEntity.ok(appointmentService.findAll());
        }

        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Paciente')")
        public ResponseEntity<AppointmentDto> getAppointmentById(@PathVariable String id) {
                AppointmentDto dto = appointmentService.findById(id).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la atención " + id));
                return ResponseEntity.ok(dto);
        }

        @PostMapping("/{id}/confirm")
        @PreAuthorize("hasAnyRole('Admin', 'Recepcionista')")
        public ResponseEntity<AppointmentDto> confirmAppointment(@PathVariable String id) {
                AppointmentService.ConfirmationResult result = appointmentService.confirm(id);
                AppointmentDto dto = result.appointment();
                if (result.newlyConfirmed()) {
                        try {
                                appointmentEventProducer.publishAppointmentConfirmed(
                                                AppointmentConfirmedEvent.builder()
                                                                .appointmentId(dto.getId())
                                                                .confirmedBy("BFF-Controller")
                                                                .dateTime(dto.getDateTime())
                                                                .status(dto.getStatus())
                                                                .notes("Confirmación emitida desde endpoint REST /api/appointments/" + dto.getId() + "/confirm")
                                                                .build()
                                );
                        } catch (Exception ex) {
                                log.error("La atención {} quedó confirmada en PostgreSQL, pero no se pudo publicar el evento RabbitMQ",
                                        dto.getId(), ex);
                        }
                }
                return ResponseEntity.ok(dto);
        }
}
