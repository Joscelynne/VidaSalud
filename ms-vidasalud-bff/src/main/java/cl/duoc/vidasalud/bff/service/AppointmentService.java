package cl.duoc.vidasalud.bff.service;

import cl.duoc.vidasalud.bff.dto.AppointmentDto;
import cl.duoc.vidasalud.bff.dto.CreateAppointmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final JdbcTemplate jdbcTemplate;

    public List<AppointmentDto> findAll() {
        return jdbcTemplate.query("""
                SELECT id, patient_name, doctor_name, specialty, date_time, status, room_number, notes
                FROM vidasalud_appointments
                ORDER BY id
                """, (rs, rowNum) -> new AppointmentDto(
                rs.getString("id"), rs.getString("patient_name"), rs.getString("doctor_name"),
                rs.getString("specialty"), rs.getString("date_time"), rs.getString("status"),
                rs.getString("room_number"), rs.getString("notes")));
    }

    public Optional<AppointmentDto> findById(String id) {
        return jdbcTemplate.query("""
                        SELECT id, patient_name, doctor_name, specialty, date_time, status, room_number, notes
                        FROM vidasalud_appointments WHERE id = ?
                        """,
                (rs, rowNum) -> new AppointmentDto(
                        rs.getString("id"), rs.getString("patient_name"), rs.getString("doctor_name"),
                        rs.getString("specialty"), rs.getString("date_time"), rs.getString("status"),
                        rs.getString("room_number"), rs.getString("notes")), id)
                .stream().findFirst();
    }

    @Transactional
    public AppointmentDto create(CreateAppointmentRequest request) {
        String id = (request.getId() != null && !request.getId().isBlank())
                ? request.getId().trim()
                : "APT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        if (findById(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una atención médica con ID " + id);
        }

        String status = (request.getStatus() != null && !request.getStatus().isBlank())
                ? request.getStatus().trim()
                : "PENDING";
        String roomNumber = (request.getRoomNumber() != null && !request.getRoomNumber().isBlank())
                ? request.getRoomNumber().trim()
                : "Box Por Asignar";
        String notes = request.getNotes() != null ? request.getNotes().trim() : "";

        jdbcTemplate.update("""
                INSERT INTO vidasalud_appointments
                    (id, patient_name, doctor_name, specialty, date_time, status, room_number, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, request.getPatientName().trim(), request.getDoctorName().trim(),
                request.getSpecialty().trim(), request.getDateTime().trim(), status,
                roomNumber, notes);

        return new AppointmentDto(id, request.getPatientName().trim(), request.getDoctorName().trim(),
                request.getSpecialty().trim(), request.getDateTime().trim(), status, roomNumber, notes);
    }

    @Transactional
    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM vidasalud_appointments WHERE id = ?", id);
    }

    @Transactional
    public void revertToPending(String id) {
        jdbcTemplate.update("""
                UPDATE vidasalud_appointments
                SET status = 'PENDING', updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, id);
    }

    @Transactional
    public ConfirmationResult confirm(String id) {
        int changed = jdbcTemplate.update("""
                UPDATE vidasalud_appointments
                SET status = 'CONFIRMED', updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'PENDING'
                """, id);

        AppointmentDto appointment = findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la atención " + id));

        if (changed == 0 && !"CONFIRMED".equals(appointment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede confirmar una atención con estado " + appointment.getStatus());
        }
        return new ConfirmationResult(appointment, changed == 1);
    }

    public record ConfirmationResult(AppointmentDto appointment, boolean newlyConfirmed) { }
}
