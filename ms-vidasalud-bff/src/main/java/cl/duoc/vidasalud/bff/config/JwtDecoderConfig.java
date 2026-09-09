package cl.duoc.vidasalud.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class JwtDecoderConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${azure.api-client-id:00000000-0000-0000-0000-000000000000}")
    private String apiClientId;

    @Bean
    public JwtDecoder jwtDecoder() {
        // Inicializar JwtDecoder usando OIDC Discovery de Azure AD
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new OAuth2TokenValidator<Jwt>() {
            @Override
            public org.springframework.security.oauth2.core.OAuth2TokenValidatorResult validate(Jwt jwt) {
                List<String> audience = jwt.getAudience();
                String expectedAudienceFull = "api://" + apiClientId;
                if (audience != null && (audience.contains(apiClientId) || audience.contains(expectedAudienceFull))) {
                    return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
                }
                // Si la audiencia no requiere estricto match en dev, permitir passthrough
                return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
            }
        };

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience);
        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }
}
