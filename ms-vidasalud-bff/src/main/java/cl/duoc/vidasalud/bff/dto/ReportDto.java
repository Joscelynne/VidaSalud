package cl.duoc.vidasalud.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDto {
    private long totalAppointments;
    private long completedAppointments;
    private long pendingAppointments;
    private long cancelledAppointments;
    private double monthlyRevenue;
    private double patientSatisfactionRate;
}
