package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.amqp.CreateQueueRequest;
import cl.duoc.vidasalud.bff.dto.amqp.QueueStatsResponse;
import cl.duoc.vidasalud.bff.exception.QueueNotFoundException;
import cl.duoc.vidasalud.bff.service.RabbitAdminService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("Pruebas de Seguridad y Validación REST para /api/admin/amqp/**")
class RabbitAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RabbitAdminService rabbitAdminService;

    // =========================================================================
    // CASO 1: PETICIÓN SIN TOKEN (401 UNAUTHORIZED)
    // =========================================================================
    @Test
    @DisplayName("1. Rechaza peticiones sin token Bearer con HTTP 401 Unauthorized y cuerpo ErrorResponse")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/amqp/queues/citas.agendamiento.queue")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token inválido, expirado o ausente"))
                .andExpect(jsonPath("$.path").value("/api/admin/amqp/queues/citas.agendamiento.queue"));
    }

    // =========================================================================
    // CASO 2: PETICIONES CON ROLES NO AUTORIZADOS (403 FORBIDDEN)
    // =========================================================================
    @Test
    @DisplayName("2a. Rechaza usuario con rol 'Paciente' con HTTP 403 Forbidden")
    void shouldReturn403WhenRoleIsPaciente() throws Exception {
        mockMvc.perform(get("/api/admin/amqp/queues/citas.agendamiento.queue")
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
        mockMvc.perform(post("/api/admin/amqp/queues/citas.agendamiento.queue/purge")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Recepcionista")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("2c. Rechaza usuario con rol 'Auditor' con HTTP 403 Forbidden")
    void shouldReturn403WhenRoleIsAuditor() throws Exception {
        mockMvc.perform(delete("/api/admin/amqp/queues/citas.agendamiento.queue")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Auditor")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // =========================================================================
    // CASO 3: PETICIÓN CON ROL ADMIN AUTORIZADO (200 / 201)
    // =========================================================================
    @Test
    @DisplayName("3a. Permite consulta de estadísticas con rol 'Admin' retornando HTTP 200 OK")
    void shouldAllowAdminToGetQueueStats() throws Exception {
        QueueStatsResponse stats = QueueStatsResponse.builder()
                .name("citas.agendamiento.queue")
                .exists(true)
                .messageCount(5)
                .consumerCount(1)
                .apiSource("Spring AMQP RabbitAdmin")
                .details("Métricas AMQP")
                .build();

        when(rabbitAdminService.getQueueStats(eq("citas.agendamiento.queue"))).thenReturn(stats);

        mockMvc.perform(get("/api/admin/amqp/queues/citas.agendamiento.queue")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("citas.agendamiento.queue"))
                .andExpect(jsonPath("$.messageCount").value(5))
                .andExpect(jsonPath("$.consumerCount").value(1));
    }

    @Test
    @DisplayName("3b. Permite crear colas con rol 'Admin' retornando HTTP 201 Created")
    void shouldAllowAdminToCreateQueue() throws Exception {
        CreateQueueRequest request = CreateQueueRequest.builder()
                .name("citas.urgencia.queue")
                .durable(true)
                .deadLetterExchange("vidasalud.dlx.exchange")
                .deadLetterRoutingKey("citas.urgencia.dlq.rk")
                .build();

        when(rabbitAdminService.createQueue(any(CreateQueueRequest.class))).thenReturn("citas.urgencia.queue");

        mockMvc.perform(post("/api/admin/amqp/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.data.queueName").value("citas.urgencia.queue"));
    }

    // =========================================================================
    // CASO 4: VALIDACIÓN DE SOLICITUDES INVÁLIDAS (400 BAD REQUEST)
    // =========================================================================
    @Test
    @DisplayName("4. Rechaza solicitud de creación con nombre de cola vacío retornando HTTP 400 Bad Request")
    void shouldReturn400WhenQueueNameIsInvalid() throws Exception {
        CreateQueueRequest invalidRequest = CreateQueueRequest.builder()
                .name("") // Inválido: NotBlank
                .durable(true)
                .build();

        mockMvc.perform(post("/api/admin/amqp/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Validación fallida")));
    }

    // =========================================================================
    // CASO 5: RECURSO NO ENCONTRADO (404 NOT FOUND)
    // =========================================================================
    @Test
    @DisplayName("5. Retorna HTTP 404 Not Found cuando la cola solicitada no existe")
    void shouldReturn404WhenQueueNotFound() throws Exception {
        when(rabbitAdminService.getQueueStats(eq("cola.fantasma")))
                .thenThrow(new QueueNotFoundException("cola.fantasma"));

        mockMvc.perform(get("/api/admin/amqp/queues/cola.fantasma")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("La cola 'cola.fantasma' no existe en el broker RabbitMQ"));
    }
}
