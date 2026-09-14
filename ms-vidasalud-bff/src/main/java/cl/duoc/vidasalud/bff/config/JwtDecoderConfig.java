package cl.duoc.vidasalud.bff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;
import java.util.Set;

/**
 * Configuración del JwtDecoder para validar tokens de Microsoft Entra ID.
 * 
 * Microsoft Entra ID puede emitir tokens con dos formatos de issuer diferentes
 * según el valor de accessTokenAcceptedVersion en el App Registration:
 * - v1.0: https://sts.windows.net/{tenant-id}/
 * - v2.0: https://login.microsoftonline.com/{tenant-id}/v2.0
 * 
 * Esta configuración acepta AMBOS formatos válidos para el tenant configurado,
 * manteniendo la seguridad al validar estrictamente el tenant-id.
 */
@Configuration
public class JwtDecoderConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtDecoderConfig.class);

    @Value("${azure.tenant-id}")
    private String tenantId;

    @Value("${azure.api-client-id}")
    private String apiClientId;

    @Bean
    public JwtDecoder jwtDecoder() {
        // Construir las URLs de los dos formatos de issuer válidos de Microsoft Entra ID
        String issuerV1 = "https://sts.windows.net/" + tenantId + "/";
        String issuerV2 = "https://login.microsoftonline.com/" + tenantId + "/v2.0";
        
        // Audiencias válidas: Application ID URI y Client ID directo
        String audienceUri = "api://" + apiClientId;
        Set<String> validAudiences = Set.of(audienceUri, apiClientId);
        Set<String> validIssuers = Set.of(issuerV1, issuerV2);

        log.info("Configurando JwtDecoder para VidaSalud BFF");
        log.info("Tenant ID: {}", tenantId);
        log.info("Issuers válidos: {}", validIssuers);
        log.info("Audiences válidas: {}", validAudiences);

        // Usar el endpoint JWKS de Microsoft para obtener las claves públicas
        // El endpoint v2.0 contiene las claves para ambas versiones de tokens
        String jwkSetUri = "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Validador de issuer: acepta ambos formatos válidos de Microsoft Entra ID
        OAuth2TokenValidator<Jwt> issuerValidator = new JwtClaimValidator<String>(
                "iss",
                issuer -> {
                    boolean valid = issuer != null && validIssuers.contains(issuer);
                    if (!valid) {
                        log.warn("Issuer inválido recibido: {}. Esperados: {}", issuer, validIssuers);
                    }
                    return valid;
                }
        );

        // Validador de audience: acepta api://{client-id} o {client-id} directo
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> audiences = jwt.getAudience();
            if (audiences == null || audiences.isEmpty()) {
                log.warn("Token sin claim 'aud'");
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Token sin audiencia", null));
            }
            
            boolean hasValidAudience = audiences.stream().anyMatch(validAudiences::contains);
            if (!hasValidAudience) {
                log.warn("Audiencia inválida: {}. Esperadas: {}", audiences, validAudiences);
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", 
                                "La audiencia del JWT no corresponde a VidaSalud", null));
            }
            return OAuth2TokenValidatorResult.success();
        };

        // Validador de timestamp (exp, nbf, iat)
        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();

        // Combinar todos los validadores
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                issuerValidator,
                audienceValidator
        );

        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }
}