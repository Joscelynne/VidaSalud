package cl.duoc.vidasalud.bff.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Evento de dominio emitido cuando una cita médica es confirmada por recepcionista o administrador.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentConfirmedEvent implements Serializable {

    private String eventId;

    @Builder.Default
    private String eventType = "APPOINTMENT_CONFIRMED";

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Builder.Default
    private Instant timestamp = Instant.now();

    private String appointmentId;
    private String confirmedBy;
    private String dateTime;
    private String notes;

    @Builder.Default
    private String status = "CONFIRMED";
}
