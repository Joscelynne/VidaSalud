package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.kafka.*;
import cl.duoc.vidasalud.bff.service.KafkaAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Seguridad y Control de Acceso REST para /api/admin/kafka/** (EP5)")
class KafkaAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private KafkaAdminService kafkaAdminService;

    // =========================================================================
    // CASO 1: PETICIONES SIN TOKEN BEARER (HTTP 401 UNAUTHORIZED)
    // =========================================================================
    @Test
    @DisplayName("1. Rechaza peticiones sin token Bearer con HTTP 401 Unauthorized")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/kafka/topics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token inválido, expirado o ausente"))
                .andExpect(jsonPath("$.path").value("/api/admin/kafka/topics"));
    }

    // =========================================================================
    // CASO 2: PETICIONES CON ROL INSUFICIENTE (HTTP 403 FORBIDDEN)
    // =========================================================================
    @Test
    @DisplayName("2a. Rechaza usuario con rol 'Paciente' con HTTP 403 Forbidden")
    void shouldReturn403WhenRoleIsPaciente() throws Exception {
        mockMvc.perform(get("/api/admin/kafka/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Paciente")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Acceso denegado: rol o privilegio insuficiente para esta operación"));
    }

    @Test
    @DisplayName("2b. Rechaza usuario con rol 'Recepcionista' con HTTP 403 Forbidden")
    void shouldReturn403WhenRoleIsRecepcionista() throws Exception {
        mockMvc.perform(get("/api/admin/kafka/consumer-groups")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Recepcionista")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // =========================================================================
    // CASO 3: PETICIONES CON ROL ADMIN AUTORIZADO (HTTP 200 / 201 / 202)
    // =========================================================================
    @Test
    @DisplayName("3a. Permite listar tópicos a usuario con rol 'Admin' (HTTP 200 OK)")
    void shouldAllowListTopicsForAdmin() throws Exception {
        KafkaTopicSummaryDto summary = KafkaTopicSummaryDto.builder()
                .name("vidasalud.atenciones.stream")
                .partitions(3)
                .replicationFactor(3)
                .isProtected(true)
                .retentionMs("604800000")
                .cleanupPolicy("delete")
                .build();

        when(kafkaAdminService.listTopics()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/admin/kafka/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("vidasalud.atenciones.stream"))
                .andExpect(jsonPath("$[0].partitions").value(3))
                .andExpect(jsonPath("$[0].replicationFactor").value(3))
                .andExpect(jsonPath("$[0].isProtected").value(true));
    }

    @Test
    @DisplayName("3b. Permite crear tópicos a usuario con rol 'Admin' (HTTP 201 Created)")
    void shouldAllowCreateTopicForAdmin() throws Exception {
        CreateKafkaTopicRequest request = CreateKafkaTopicRequest.builder()
                .name("test.nuevo.topic")
                .partitions(3)
                .replicationFactor((short) 3)
                .retentionMs(86400000L)
                .cleanupPolicy("delete")
                .build();

        KafkaOperationResponse response = KafkaOperationResponse.builder()
                .status("SUCCESS")
                .message("Tópico creado")
                .timestamp(Instant.now())
                .details(Map.of("topic", "test.nuevo.topic"))
                .build();

        when(kafkaAdminService.createTopic(any(CreateKafkaTopicRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/kafka/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.details.topic").value("test.nuevo.topic"));
    }

    @Test
    @DisplayName("3c. Permite publicar evento de prueba a usuario con rol 'Admin' (HTTP 202 Accepted)")
    void shouldAllowPublishTestEventForAdmin() throws Exception {
        PublishTestKafkaEventRequest request = PublishTestKafkaEventRequest.builder()
                .eventType("ATENCIONES")
                .specialty("Traumatología")
                .status("SCHEDULED")
                .build();

        KafkaOperationResponse response = KafkaOperationResponse.builder()
                .status("SUCCESS")
                .message("Evento publicado")
                .timestamp(Instant.now())
                .build();

        when(kafkaAdminService.publishTestEvent(any(PublishTestKafkaEventRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/kafka/publish/test")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
