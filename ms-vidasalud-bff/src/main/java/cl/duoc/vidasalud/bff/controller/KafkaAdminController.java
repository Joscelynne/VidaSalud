package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.kafka.*;
import cl.duoc.vidasalud.bff.service.KafkaAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la administración oficial de Apache Kafka en VidaSalud (EP5 / EP6).
 * Endpoints protegidos exclusivamente para usuarios con rol 'Admin'.
 * Provee gestión de ciclo de vida de tópicos, configuraciones avanzadas,
 * inspección de consumer groups, offsets y generación de eventos de streaming para pruebas.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/kafka")
@RequiredArgsConstructor
@PreAuthorize("hasRole('Admin')")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    @GetMapping("/topics")
    public ResponseEntity<List<KafkaTopicSummaryDto>> listTopics() {
        log.info("[KAFKA-ADMIN-CTRL] Listando tópicos del clúster");
        return ResponseEntity.ok(kafkaAdminService.listTopics());
    }

    @GetMapping("/topics/{topicName}")
    public ResponseEntity<KafkaTopicDetailDto> getTopicDetail(@PathVariable String topicName) {
        log.info("[KAFKA-ADMIN-CTRL] Consultando detalle de tópico [{}]", topicName);
        return ResponseEntity.ok(kafkaAdminService.getTopicDetail(topicName));
    }

    @PostMapping("/topics")
    public ResponseEntity<KafkaOperationResponse> createTopic(@Valid @RequestBody CreateKafkaTopicRequest request) {
        log.info("[KAFKA-ADMIN-CTRL] Solicitud para crear tópico [{}], particiones [{}], réplicas [{}]",
                request.getName(), request.getPartitions(), request.getReplicationFactor());
        KafkaOperationResponse response = kafkaAdminService.createTopic(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/topics/{topicName}/config")
    public ResponseEntity<KafkaOperationResponse> alterTopicConfig(
            @PathVariable String topicName,
            @Valid @RequestBody AlterKafkaTopicConfigRequest request) {
        log.info("[KAFKA-ADMIN-CTRL] Modificando configs para tópico [{}]", topicName);
        return ResponseEntity.ok(kafkaAdminService.alterTopicConfig(topicName, request));
    }

    @DeleteMapping("/topics/{topicName}")
    public ResponseEntity<KafkaOperationResponse> deleteTopic(@PathVariable String topicName) {
        log.info("[KAFKA-ADMIN-CTRL] Solicitud de eliminación para tópico [{}]", topicName);
        return ResponseEntity.ok(kafkaAdminService.deleteTopic(topicName));
    }

    @GetMapping("/consumer-groups")
    public ResponseEntity<List<KafkaConsumerGroupDto>> listConsumerGroups() {
        log.info("[KAFKA-ADMIN-CTRL] Inspeccionando consumer groups, offsets y lag");
        return ResponseEntity.ok(kafkaAdminService.listConsumerGroups());
    }

    @PostMapping("/consumer-groups/{groupId}/offsets/reset")
    public ResponseEntity<KafkaOperationResponse> resetConsumerGroupOffsets(
            @PathVariable String groupId,
            @Valid @RequestBody ResetConsumerGroupOffsetsRequest request) {
        log.warn("[KAFKA-ADMIN-CTRL] Reinicio de offsets solicitado para grupo [{}], estrategia [{}]",
                groupId, request.getStrategy());
        return ResponseEntity.ok(kafkaAdminService.resetConsumerGroupOffsets(groupId, request));
    }

    @PostMapping("/publish/test")
    public ResponseEntity<KafkaOperationResponse> publishTestEvent(
            @Valid @RequestBody PublishTestKafkaEventRequest request) {
        log.info("[KAFKA-ADMIN-CTRL] Publicando evento de prueba tipo [{}]", request.getEventType());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(kafkaAdminService.publishTestEvent(request));
    }
}
