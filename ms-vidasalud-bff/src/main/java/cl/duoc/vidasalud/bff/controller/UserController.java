package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.UserMeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getAuthenticatedUserInfo(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            String oid = jwt.getClaimAsString("oid");
            if (oid == null) oid = jwt.getSubject();

            String name = jwt.getClaimAsString("name");
            if (name == null) name = jwtAuth.getName();

            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername == null) preferredUsername = jwt.getClaimAsString("email");
            if (preferredUsername == null) preferredUsername = name;

            // Extraer roles otorgados (sin el prefijo ROLE_)
            List<String> roles = jwtAuth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .map(auth -> auth.substring(5))
                    .collect(Collectors.toList());

            // Extraer scopes
            String scp = jwt.getClaimAsString("scp");
            List<String> scopes = (scp != null && !scp.isBlank()) ? Arrays.asList(scp.split(" ")) : List.of();

            Instant expiresAt = jwt.getExpiresAt();
            Instant issuedAt = jwt.getIssuedAt();
            String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "https://login.microsoftonline.com/common/v2.0";

            UserMeResponse response = UserMeResponse.builder()
                    .oid(oid)
                    .name(name)
                    .email(preferredUsername)
                    .preferredUsername(preferredUsername)
                    .roles(roles)
                    .scopes(scopes)
                    .expiresAt(expiresAt)
                    .issuedAt(issuedAt)
                    .issuer(issuer)
                    .build();

            return ResponseEntity.ok(response);
        }

        // Fallback si la autenticación es mock o test
        UserMeResponse fallback = UserMeResponse.builder()
                .oid("00000000-0000-0000-0000-000000000000")
                .name(authentication.getName())
                .email(authentication.getName())
                .preferredUsername(authentication.getName())
                .roles(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .scopes(List.of("access_as_user"))
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuedAt(Instant.now())
                .issuer("https://login.microsoftonline.com/common/v2.0")
                .build();

        return ResponseEntity.ok(fallback);
    }
}
