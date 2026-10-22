package cl.duoc.vidasalud.bff.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud de creación / agendamiento de una nueva atención médica en VidaSalud.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {

    private String id;

    @NotBlank(message = "El nombre del paciente es obligatorio")
    private String patientName;

    @NotBlank(message = "El nombre del médico es obligatorio")
    private String doctorName;

    @NotBlank(message = "La especialidad es obligatoria")
    private String specialty;

    @NotBlank(message = "La fecha y hora son obligatorias")
    private String dateTime;

    private String roomNumber;
    private String notes;

    @Builder.Default
    private String status = "PENDING";
}
