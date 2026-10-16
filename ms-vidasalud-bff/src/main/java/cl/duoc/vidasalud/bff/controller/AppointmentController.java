package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.AppointmentDto;
import cl.duoc.vidasalud.bff.event.AppointmentConfirmedEvent;
import cl.duoc.vidasalud.bff.producer.AppointmentEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

        @Autowired(required = false)
        private AppointmentEventProducer appointmentEventProducer;

        @GetMapping
        @PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Paciente')")
        public ResponseEntity<List<AppointmentDto>> getAllAppointments() {
                List<AppointmentDto> mockAppointments = List.of(
                                AppointmentDto.builder()
                                                .id("APT-101")
                                                .patientName("Carlos Mendoza")
                                                .doctorName("Dra. María Paz")
                                                .specialty("Medicina General")
                                                .dateTime("2026-09-09T17:00:00")
                                                .status("PENDING")
                                                .roomNumber("Box 204")
                                                .notes("Chequeo preventivo anual")
                                                .build(),
                                AppointmentDto.builder()
                                                .id("APT-102")
                                                .patientName("Lorena Silva")
                                                .doctorName("Dr. Roberto Gómez")
                                                .specialty("Cardiología")
                                                .dateTime("2026-09-09T17:30:00")
                                                .status("CONFIRMED")
                                                .roomNumber("Box 108")
                                                .notes("Electrocardiograma de control")
                                                .build(),
                                AppointmentDto.builder()
                                                .id("APT-103")
                                                .patientName("Felipe Araya")
                                                .doctorName("Dra. Camila Morales")
                                                .specialty("Pediatría")
                                                .dateTime("2026-09-10T11:00:00")
                                                .status("COMPLETED")
                                                .roomNumber("Box 301")
                                                .notes("Control de niño sano")
                                                .build());
                return ResponseEntity.ok(mockAppointments);
        }

        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Paciente')")
        public ResponseEntity<AppointmentDto> getAppointmentById(@PathVariable String id) {
                AppointmentDto dto = AppointmentDto.builder()
                                .id(id)
                                .patientName("Paciente Registrado")
                                .doctorName("Dra. María Paz")
                                .specialty("Medicina General")
                                .dateTime("2026-09-09T17:00:00")
                                .status("CONFIRMED")
                                .roomNumber("Box 204")
                                .build();
                return ResponseEntity.ok(dto);
        }

        @PostMapping("/{id}/confirm")
        @PreAuthorize("hasAnyRole('Admin', 'Recepcionista')")
        public ResponseEntity<AppointmentDto> confirmAppointment(@PathVariable String id) {
                if (appointmentEventProducer != null) {
                        try {
                                appointmentEventProducer.publishAppointmentConfirmed(
                                                AppointmentConfirmedEvent.builder()
                                                                .appointmentId(id)
                                                                .confirmedBy("BFF-Controller")
                                                                .dateTime("2026-09-09T17:00:00")
                                                                .status("CONFIRMED")
                                                                .notes("Confirmación emitida desde endpoint REST /api/appointments/" + id + "/confirm")
                                                                .build()
                                );
                        } catch (Exception ex) {
                                log.warn("Aviso: no se pudo publicar AppointmentConfirmedEvent en RabbitMQ: {}", ex.getMessage());
                        }
                }

                AppointmentDto dto = AppointmentDto.builder()
                                .id(id)
                                .patientName("Paciente Confirmado")
                                .doctorName("Dra. María Paz")
                                .specialty("Medicina General")
                                .dateTime("2026-09-09T17:00:00")
                                .status("CONFIRMED")
                                .roomNumber("Box 204")
                                .build();
                return ResponseEntity.ok(dto);
        }
}
