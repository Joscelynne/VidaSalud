package cl.duoc.vidasalud.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDto {
    private String id;
    private String timestamp;
    private String user;
    private String action;
    private String resource;
    private String status;
    private String ipAddress;
    private String traceId;
    private String details;
}
