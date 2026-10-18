package cl.duoc.vidasalud.bff.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Evento de dominio emitido cuando se agenda una nueva atención médica.
 * Contiene únicamente identificadores y datos de negocio pertinentes, evitando PII sensible.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentCreatedEvent implements Serializable {

    private String eventId;

    @Builder.Default
    private String eventType = "APPOINTMENT_CREATED";

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Builder.Default
    private Instant timestamp = Instant.now();

    private String appointmentId;
    /** Accepts the historical producer field while keeping {@code patientIdentifier} canonical. */
    @JsonAlias("patientId")
    private String patientIdentifier;
    private String patientName;
    private String doctorName;
    private String specialty;
    private String dateTime;
    private String roomNumber;
    private String notes;

    @Builder.Default
    private String status = "PENDING";
}
