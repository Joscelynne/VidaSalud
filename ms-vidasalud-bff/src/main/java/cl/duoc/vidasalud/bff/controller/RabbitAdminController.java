package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.amqp.*;
import cl.duoc.vidasalud.bff.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Controlador REST para administración de RabbitMQ en VidaSalud BFF.
 * Toda la ruta /api/admin/amqp/** está restringida exclusivamente al rol 'Admin'.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/amqp")
@PreAuthorize("hasRole('Admin')")
@RequiredArgsConstructor
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    /**
     * Declara una nueva cola en RabbitMQ con soporte de DLQ y TTL.
     */
    @PostMapping("/queues")
    public ResponseEntity<AmqpOperationResponse> createQueue(@Valid @RequestBody CreateQueueRequest request) {
        String queueName = rabbitAdminService.createQueue(request);
        AmqpOperationResponse response = AmqpOperationResponse.builder()
                .timestamp(Instant.now().toString())
                .status("CREATED")
                .message("Cola creada exitosamente en RabbitMQ")
                .data(Map.of("queueName", queueName, "durable", request.isDurable()))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Consulta estadísticas básicas de una cola (mensajes y consumidores activos).
     */
    @GetMapping("/queues/{name}")
    public ResponseEntity<QueueStatsResponse> getQueueStats(@PathVariable String name) {
        QueueStatsResponse stats = rabbitAdminService.getQueueStats(name);
        return ResponseEntity.ok(stats);
    }

    /**
     * Elimina una cola del broker.
     */
    @DeleteMapping("/queues/{name}")
    public ResponseEntity<AmqpOperationResponse> deleteQueue(@PathVariable String name) {
        rabbitAdminService.deleteQueue(name);
        AmqpOperationResponse response = AmqpOperationResponse.builder()
                .timestamp(Instant.now().toString())
                .status("DELETED")
                .message("Cola eliminada exitosamente")
                .data(Map.of("queueName", name))
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Purga todos los mensajes contenidos en una cola.
     */
    @PostMapping("/queues/{name}/purge")
    public ResponseEntity<AmqpOperationResponse> purgeQueue(@PathVariable String name) {
        int purgedCount = rabbitAdminService.purgeQueue(name);
        AmqpOperationResponse response = AmqpOperationResponse.builder()
                .timestamp(Instant.now().toString())
                .status("PURGED")
                .message("Cola purgada exitosamente")
                .data(Map.of("queueName", name, "purgedMessageCount", purgedCount))
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Declara un exchange de tipo direct, topic o fanout.
     */
    @PostMapping("/exchanges")
    public ResponseEntity<AmqpOperationResponse> createExchange(@Valid @RequestBody CreateExchangeRequest request) {
        rabbitAdminService.createExchange(request);
        AmqpOperationResponse response = AmqpOperationResponse.builder()
                .timestamp(Instant.now().toString())
                .status("CREATED")
                .message("Exchange creado exitosamente")
                .data(Map.of("exchangeName", request.getName(), "type", request.getType()))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Declara un binding entre un exchange y una cola o exchange destino.
     */
    @PostMapping("/bindings")
    public ResponseEntity<AmqpOperationResponse> createBinding(@Valid @RequestBody CreateBindingRequest request) {
        rabbitAdminService.createBinding(request);
        AmqpOperationResponse response = AmqpOperationResponse.builder()
                .timestamp(Instant.now().toString())
                .status("CREATED")
                .message("Binding creado exitosamente")
                .data(Map.of(
                        "destination", request.getDestination(),
                        "exchange", request.getExchange(),
                        "routingKey", request.getRoutingKey()
                ))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Publica un mensaje de prueba en un exchange con una routing key específica.
     */
    @PostMapping("/test-publish")
    public ResponseEntity<AmqpOperationResponse> testPublish(@Valid @RequestBody TestPublishRequest request) {
        rabbitAdminService.testPublish(request);
        AmqpOperationResponse response = AmqpOperationResponse.builder()
                .timestamp(Instant.now().toString())
                .status("PUBLISHED")
                .message("Mensaje de prueba publicado correctamente")
                .data(Map.of(
                        "exchange", request.getExchange(),
                        "routingKey", request.getRoutingKey()
                ))
                .build();
        return ResponseEntity.ok(response);
    }
}
