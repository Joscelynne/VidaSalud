package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.ReportDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @GetMapping("/kpis")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ReportDto> getKPIs() {
        ReportDto report = ReportDto.builder()
                .totalAppointments(1420)
                .completedAppointments(1180)
                .pendingAppointments(190)
                .cancelledAppointments(50)
                .monthlyRevenue(48500000.0)
                .patientSatisfactionRate(96.4)
                .build();
        return ResponseEntity.ok(report);
    }
}
