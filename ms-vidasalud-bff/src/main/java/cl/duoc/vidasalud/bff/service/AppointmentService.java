package cl.duoc.vidasalud.bff.service;

import cl.duoc.vidasalud.bff.dto.AppointmentDto;
import cl.duoc.vidasalud.bff.entity.Appointment;
import cl.duoc.vidasalud.bff.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de appointments.
 * Encapsula la lógica de negocio y conversión Entity ↔ DTO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    /**
     * Obtiene todos los appointments de la base de datos.
     * @return lista de AppointmentDto
     */
    @Transactional(readOnly = true)
    public List<AppointmentDto> getAllAppointments() {
        log.debug("Consultando todos los appointments desde PostgreSQL");
        List<Appointment> appointments = appointmentRepository.findAll();
        log.info("Se encontraron {} appointments en la base de datos", appointments.size());
        return appointments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un appointment por su ID.
     * @param id identificador del appointment
     * @return Optional con AppointmentDto si existe
     */
    @Transactional(readOnly = true)
    public Optional<AppointmentDto> getAppointmentById(String id) {
        log.debug("Buscando appointment con id: {}", id);
        return appointmentRepository.findById(id)
                .map(this::toDto);
    }

    /**
     * Confirma un appointment actualizando su estado a CONFIRMED.
     * @param id identificador del appointment
     * @return Optional con AppointmentDto actualizado si existe
     */
    @Transactional
    public Optional<AppointmentDto> confirmAppointment(String id) {
        log.debug("Confirmando appointment con id: {}", id);
        return appointmentRepository.findById(id)
                .map(appointment -> {
                    appointment.setStatus("CONFIRMED");
                    Appointment saved = appointmentRepository.save(appointment);
                    log.info("Appointment {} confirmado exitosamente", id);
                    return toDto(saved);
                });
    }

    /**
     * Convierte una entidad Appointment a AppointmentDto.
     * Mantiene el contrato JSON esperado por el frontend.
     */
    private AppointmentDto toDto(Appointment entity) {
        return AppointmentDto.builder()
                .id(entity.getId())
                .patientName(entity.getPatientName())
                .doctorName(entity.getDoctorName())
                .specialty(entity.getSpecialty())
                .dateTime(entity.getDateTime())
                .status(entity.getStatus())
                .roomNumber(entity.getRoomNumber())
                .notes(entity.getNotes())
                .build();
    }

    /**
     * Convierte un AppointmentDto a entidad Appointment.
     */
    private Appointment toEntity(AppointmentDto dto) {
        return Appointment.builder()
                .id(dto.getId())
                .patientName(dto.getPatientName())
                .doctorName(dto.getDoctorName())
                .specialty(dto.getSpecialty())
                .dateTime(dto.getDateTime())
                .status(dto.getStatus())
                .roomNumber(dto.getRoomNumber())
                .notes(dto.getNotes())
                .build();
    }
}
