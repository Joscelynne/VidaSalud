package cl.duoc.vidasalud.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMeResponse {
    private String oid;
    private String name;
    private String email;
    private String preferredUsername;
    private List<String> roles;
    private List<String> scopes;
    private Instant expiresAt;
    private Instant issuedAt;
    private String issuer;
}
