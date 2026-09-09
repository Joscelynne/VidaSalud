package cl.duoc.vidasalud.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
public class JwtAuthenticationConverterConfig {

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return new Converter<Jwt, AbstractAuthenticationToken>() {
            private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

            @Override
            public AbstractAuthenticationToken convert(Jwt jwt) {
                Collection<GrantedAuthority> authorities = new ArrayList<>();

                // 1. Convertir scopes estándar (SCOPE_)
                Collection<GrantedAuthority> defaultAuthorities = defaultGrantedAuthoritiesConverter.convert(jwt);
                if (defaultAuthorities != null) {
                    authorities.addAll(defaultAuthorities);
                }

                // 2. Extraer claim 'roles' de Azure AD y convertir a ROLE_<RoleName>
                List<String> roles = jwt.getClaimAsStringList("roles");
                if (roles != null) {
                    for (String role : roles) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }
                }

                // Si no hay roles explícitos en el JWT, otorgar ROLE_Client por defecto
                if (roles == null || roles.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_Client"));
                }

                // Usar preferiblemente preferred_username, name u sub como principal name
                String principalClaimName = jwt.getClaimAsString("preferred_username");
                if (principalClaimName == null) {
                    principalClaimName = jwt.getClaimAsString("name");
                }
                if (principalClaimName == null) {
                    principalClaimName = jwt.getSubject();
                }

                return new JwtAuthenticationToken(jwt, authorities, principalClaimName);
            }
        };
    }
}
