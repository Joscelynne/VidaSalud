package cl.duoc.vidasalud.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private String id;
    private String patientName;
    private String doctorName;
    private String specialty;
    private String dateTime;
    private String status;
    private String roomNumber;
    private String notes;
}
