package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.AuditDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('Admin', 'Auditor')")
    public ResponseEntity<List<AuditDto>> getAuditLogs() {
        List<AuditDto> logs = List.of(
                AuditDto.builder()
                        .id("LOG-001")
                        .timestamp(Instant.now().toString())
                        .user("admin@vidasalud.cl")
                        .action("GET /api/me")
                        .resource("UserController")
                        .status("SUCCESS")
                        .ipAddress("192.168.1.50")
                        .traceId("tr-8a9f-4312")
                        .details("JWT Token validado contra Azure AD JWKS endpoint. Firma y Expiración OK.")
                        .build(),
                AuditDto.builder()
                        .id("LOG-002")
                        .timestamp(Instant.now().minusSeconds(3600).toString())
                        .user("recepcionista@vidasalud.cl")
                        .action("POST /api/appointments/APT-101/confirm")
                        .resource("AppointmentController")
                        .status("SUCCESS")
                        .ipAddress("192.168.1.52")
                        .traceId("tr-11bc-9902")
                        .details("Estado de atención médica actualizado a CONFIRMED.")
                        .build(),
                AuditDto.builder()
                        .id("LOG-003")
                        .timestamp(Instant.now().minusSeconds(7200).toString())
                        .user("paciente@vidasalud.cl")
                        .action("GET /api/report/kpis")
                        .resource("ReportController")
                        .status("UNAUTHORIZED")
                        .ipAddress("190.160.10.4")
                        .traceId("tr-99dd-0012")
                        .details("CustomAccessDeniedHandler disparado (HTTP 403). El token JWT carece de rol Admin.")
                        .build()
        );
        return ResponseEntity.ok(logs);
    }
}
