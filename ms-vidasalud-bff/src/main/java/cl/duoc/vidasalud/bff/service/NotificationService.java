package cl.duoc.vidasalud.bff.service;

import cl.duoc.vidasalud.bff.dto.NotificationDto;
import cl.duoc.vidasalud.bff.event.NotificationAuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de persistencia y consulta para notificaciones de negocio en VidaSalud.
 * Almacena los eventos recibidos desde RabbitMQ en la tabla 'vidasalud_notifications'
 * asegurando una fuente de datos persistente e independiente del ciclo de vida del broker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void saveFromEvent(NotificationAuditEvent event) {
        if (event == null) return;

        String id = "NOTIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String action = event.getAction() != null ? event.getAction() : "NOTIFICATION";
        String title = switch (action) {
            case "APPOINTMENT_CREATED" -> "Nueva Cita Agendada";
            case "APPOINTMENT_CONFIRMED" -> "Cita Médica Confirmada";
            default -> "Notificación del Sistema";
        };

        String message = event.getDetails() != null ? event.getDetails() : "Evento registrado en el broker";
        String patient = event.getUser() != null ? event.getUser() : "Paciente";
        String status = event.getStatus() != null ? event.getStatus() : "PROCESSED";

        try {
            jdbcTemplate.update("""
                    INSERT INTO vidasalud_notifications
                        (id, event_id, type, title, message, appointment_id, patient_name, status, is_read)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, false)
                    """,
                    id, event.getEventId(), action, title, message,
                    extractAppointmentId(message), patient, status);

            log.info("Notificación persistida [{}] para evento [{}] acción [{}]", id, event.getEventId(), action);
        } catch (Exception ex) {
            log.error("Error al persistir notificación para evento [{}]: {}", event.getEventId(), ex.getMessage());
        }
    }

    public List<NotificationDto> findAll() {
        return jdbcTemplate.query("""
                SELECT id, event_id, type, title, message, appointment_id, patient_name, status, is_read, created_at
                FROM vidasalud_notifications
                ORDER BY created_at DESC
                LIMIT 50
                """, (rs, rowNum) -> NotificationDto.builder()
                .id(rs.getString("id"))
                .eventId(rs.getString("event_id"))
                .type(rs.getString("type"))
                .title(rs.getString("title"))
                .message(rs.getString("message"))
                .appointmentId(rs.getString("appointment_id"))
                .patientName(rs.getString("patient_name"))
                .status(rs.getString("status"))
                .read(rs.getBoolean("is_read"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant().toString() : null)
                .build());
    }

    public List<NotificationDto> findByPatientName(String patientName) {
        if (patientName == null || patientName.isBlank()) {
            return findAll();
        }
        return jdbcTemplate.query("""
                SELECT id, event_id, type, title, message, appointment_id, patient_name, status, is_read, created_at
                FROM vidasalud_notifications
                WHERE LOWER(patient_name) LIKE LOWER(?)
                ORDER BY created_at DESC
                LIMIT 50
                """, (rs, rowNum) -> NotificationDto.builder()
                .id(rs.getString("id"))
                .eventId(rs.getString("event_id"))
                .type(rs.getString("type"))
                .title(rs.getString("title"))
                .message(rs.getString("message"))
                .appointmentId(rs.getString("appointment_id"))
                .patientName(rs.getString("patient_name"))
                .status(rs.getString("status"))
                .read(rs.getBoolean("is_read"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant().toString() : null)
                .build(), "%" + patientName.trim() + "%");
    }

    @Transactional
    public boolean markAsRead(String id) {
        int rows = jdbcTemplate.update("UPDATE vidasalud_notifications SET is_read = true WHERE id = ?", id);
        return rows > 0;
    }

    @Transactional
    public int markAllAsRead() {
        return jdbcTemplate.update("UPDATE vidasalud_notifications SET is_read = true WHERE is_read = false");
    }

    private String extractAppointmentId(String details) {
        if (details == null) return null;
        int aptIdx = details.indexOf("APT-");
        if (aptIdx >= 0) {
            int endIdx = details.indexOf(" ", aptIdx);
            if (endIdx > aptIdx) {
                return details.substring(aptIdx, endIdx).replaceAll("[^a-zA-Z0-9-]", "");
            }
            return details.substring(aptIdx).replaceAll("[^a-zA-Z0-9-]", "");
        }
        return null;
    }
}
