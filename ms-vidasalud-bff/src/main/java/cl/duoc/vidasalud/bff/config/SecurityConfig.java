package cl.duoc.vidasalud.bff.config;

import cl.duoc.vidasalud.bff.security.CustomAccessDeniedHandler;
import cl.duoc.vidasalud.bff.security.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Desactivar CSRF y configurar gestión de sesiones STATELESS
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. Manejo de excepciones de seguridad 401 y 403 con JSON personalizado
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))

                // 3. Reglas de autorización por endpoint solicitadas en la pauta EP1
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Atenciones Médicas: Admin, Recepcionista, Paciente
                        .requestMatchers("/api/appointments/**")
                        .hasAnyRole("Admin", "Recepcionista", "Paciente")

                        // Catálogo: Admin, Recepcionista
                        .requestMatchers("/api/catalog/**")
                        .hasAnyRole("Admin", "Recepcionista")

                        // Reportería: Admin
                        .requestMatchers("/api/report/**")
                        .hasRole("Admin")

                        // Auditoría: Admin, Auditor
                        .requestMatchers("/api/audit/**")
                        .hasAnyRole("Admin", "Auditor")

                        .requestMatchers("/api/me").authenticated()
                        .anyRequest().authenticated())

                // 4. Configurar OAuth2 Resource Server con JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler));

        return http.build();
    }
}
