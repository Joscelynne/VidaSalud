package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.AppointmentDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

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
