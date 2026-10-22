package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.AppointmentDto;
import cl.duoc.vidasalud.bff.dto.CreateAppointmentRequest;
import cl.duoc.vidasalud.bff.event.AppointmentConfirmedEvent;
import cl.duoc.vidasalud.bff.event.AppointmentCreatedEvent;
import cl.duoc.vidasalud.bff.event.NotificationAuditEvent;
import cl.duoc.vidasalud.bff.producer.AppointmentEventProducer;
import cl.duoc.vidasalud.bff.producer.AuditEventProducer;
import cl.duoc.vidasalud.bff.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

        private final AppointmentEventProducer appointmentEventProducer;
        private final AuditEventProducer auditEventProducer;
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

        @PostMapping
        @PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Paciente')")
        public ResponseEntity<AppointmentDto> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
                AppointmentDto dto = appointmentService.create(request);

                // 1. Publicar evento de agendamiento en RabbitMQ (Direct Exchange -> citas.crear)
                try {
                        appointmentEventProducer.publishAppointmentCreated(
                                        AppointmentCreatedEvent.builder()
                                                        .appointmentId(dto.getId())
                                                        .patientIdentifier(dto.getPatientName())
                                                        .patientName(dto.getPatientName())
                                                        .doctorName(dto.getDoctorName())
                                                        .specialty(dto.getSpecialty())
                                                        .dateTime(dto.getDateTime())
                                                        .roomNumber(dto.getRoomNumber())
                                                        .notes(dto.getNotes())
                                                        .status(dto.getStatus())
                                                        .build()
                        );
                } catch (Exception ex) {
                        log.error("Fallo al publicar AppointmentCreatedEvent para la cita {}. Revirtiendo persistencia.", dto.getId(), ex);
                        appointmentService.delete(dto.getId());
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                        "No se pudo agendar la cita debido a un fallo en el broker de mensajería RabbitMQ", ex);
                }

                // 2. Publicar evento de notificación en RabbitMQ (Topic Exchange -> vidasalud.notificacion.*)
                try {
                        auditEventProducer.publishNotificationAudit(
                                        "vidasalud.notificacion.cita.creada",
                                        NotificationAuditEvent.builder()
                                                        .eventId("evt-notif-" + UUID.randomUUID().toString().substring(0, 8))
                                                        .action("APPOINTMENT_CREATED")
                                                        .resource("AppointmentController")
                                                        .user(dto.getPatientName())
                                                        .status("SUCCESS")
                                                        .traceId(UUID.randomUUID().toString().substring(0, 8))
                                                        .details(String.format("Nueva cita %s agendada para %s en %s con %s para %s",
                                                                        dto.getId(), dto.getPatientName(), dto.getSpecialty(), dto.getDoctorName(), dto.getDateTime()))
                                                        .build()
                        );
                } catch (Exception ex) {
                        log.warn("No se pudo emitir evento de notificación para cita {}: {}", dto.getId(), ex.getMessage());
                }

                return ResponseEntity.status(HttpStatus.CREATED).body(dto);
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
                                log.error("Fallo al publicar AppointmentConfirmedEvent para {}. Revirtiendo a PENDING.", dto.getId(), ex);
                                appointmentService.revertToPending(dto.getId());
                                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                                "La atención no pudo ser confirmada debido a una falla en el broker RabbitMQ", ex);
                        }

                        // Publicar evento de notificación en RabbitMQ
                        try {
                                auditEventProducer.publishNotificationAudit(
                                                "vidasalud.notificacion.cita.confirmada",
                                                NotificationAuditEvent.builder()
                                                                .eventId("evt-notif-" + UUID.randomUUID().toString().substring(0, 8))
                                                                .action("APPOINTMENT_CONFIRMED")
                                                                .resource("AppointmentController")
                                                                .user(dto.getPatientName())
                                                                .status("SUCCESS")
                                                                .traceId(UUID.randomUUID().toString().substring(0, 8))
                                                                .details(String.format("Cita médica %s confirmada para paciente %s con %s",
                                                                                dto.getId(), dto.getPatientName(), dto.getDoctorName()))
                                                                .build()
                                );
                        } catch (Exception ex) {
                                log.warn("No se pudo emitir notificación de confirmación para cita {}: {}", dto.getId(), ex.getMessage());
                        }
                }
                return ResponseEntity.ok(dto);
        }
}

