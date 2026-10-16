package cl.duoc.vidasalud.bff.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Evento de dominio emitido para propósitos de notificación y auditoría transversal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAuditEvent implements Serializable {

    private String eventId;

    @Builder.Default
    private String eventType = "NOTIFICATION_AUDIT";

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Builder.Default
    private Instant timestamp = Instant.now();

    private String action;
    private String resource;
    private String user;
    private String status;
    private String traceId;
    private String details;
}
