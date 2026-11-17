package cl.duoc.vidasalud.bff.service;

import cl.duoc.vidasalud.bff.config.KafkaProperties;
import cl.duoc.vidasalud.bff.dto.kafka.*;
import cl.duoc.vidasalud.bff.event.AtencionMetricaEvent;
import cl.duoc.vidasalud.bff.event.TelemetriaClinicaEvent;
import cl.duoc.vidasalud.bff.producer.AtencionMetricaProducer;
import cl.duoc.vidasalud.bff.producer.TelemetriaClinicaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Servicio de administración para Apache Kafka en VidaSalud (EP5 / EP6).
 * Provee operaciones para listar, crear, modificar e inspeccionar tópicos,
 * consultar consumer groups, offsets y lag, y publicar eventos de prueba.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaAdminService {

    private final AdminClient adminClient;
    private final KafkaProperties properties;
    private final AtencionMetricaProducer atencionMetricaProducer;
    private final TelemetriaClinicaProducer telemetriaClinicaProducer;

    private static final int TIMEOUT_SECONDS = 5;

    /**
     * Lista todos los tópicos no internos con su configuración esencial.
     */
    public List<KafkaTopicSummaryDto> listTopics() {
        try {
            ListTopicsResult listTopicsResult = adminClient.listTopics(new ListTopicsOptions().listInternal(false));
            Set<String> topicNames = listTopicsResult.names().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (topicNames.isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, TopicDescription> descriptions = adminClient.describeTopics(topicNames)
                    .allTopicNames().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<KafkaTopicSummaryDto> summaries = new ArrayList<>();
            for (String topicName : topicNames) {
                TopicDescription desc = descriptions.get(topicName);
                int partitions = desc != null ? desc.partitions().size() : 0;
                int replicationFactor = (desc != null && !desc.partitions().isEmpty())
                        ? desc.partitions().get(0).replicas().size()
                        : 0;

                boolean isProtected = properties.isProtectedTopic(topicName);

                // Obtener configuraciones clave (retention, cleanup)
                String retentionMs = "default";
                String cleanupPolicy = "default";
                Map<String, String> keyConfigs = new HashMap<>();

                try {
                    ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
                    Map<ConfigResource, Config> configs = adminClient.describeConfigs(List.of(resource))
                            .all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    Config config = configs.get(resource);
                    if (config != null) {
                        if (config.get(TopicConfig.RETENTION_MS_CONFIG) != null) {
                            retentionMs = config.get(TopicConfig.RETENTION_MS_CONFIG).value();
                            keyConfigs.put("retention.ms", retentionMs);
                        }
                        if (config.get(TopicConfig.CLEANUP_POLICY_CONFIG) != null) {
                            cleanupPolicy = config.get(TopicConfig.CLEANUP_POLICY_CONFIG).value();
                            keyConfigs.put("cleanup.policy", cleanupPolicy);
                        }
                    }
                } catch (Exception ex) {
                    log.debug("No se pudieron obtener configuraciones detalladas para [{}]: {}", topicName, ex.getMessage());
                }

                summaries.add(KafkaTopicSummaryDto.builder()
                        .name(topicName)
                        .partitions(partitions)
                        .replicationFactor(replicationFactor)
                        .isProtected(isProtected)
                        .retentionMs(retentionMs)
                        .cleanupPolicy(cleanupPolicy)
                        .keyConfigs(keyConfigs)
                        .build());
            }

            summaries.sort(Comparator.comparing(KafkaTopicSummaryDto::getName));
            return summaries;

        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            log.error("[KAFKA-ADMIN] Error al listar tópicos de Kafka: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo conectar con el clúster de Kafka para listar tópicos: " + ex.getMessage(), ex);
        }
    }

    /**
     * Obtiene el detalle técnico completo de un tópico (particiones, líderes, réplicas, ISRs y configuraciones).
     */
    public KafkaTopicDetailDto getTopicDetail(String topicName) {
        if (topicName == null || topicName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del tópico no puede estar vacío");
        }

        try {
            Map<String, TopicDescription> descriptions = adminClient.describeTopics(List.of(topicName.trim()))
                    .allTopicNames().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            TopicDescription desc = descriptions.get(topicName.trim());
            if (desc == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El tópico " + topicName + " no existe");
            }

            List<KafkaTopicDetailDto.PartitionDetail> partitionDetails = desc.partitions().stream()
                    .map(p -> KafkaTopicDetailDto.PartitionDetail.builder()
                            .partitionId(p.partition())
                            .leaderNodeId(p.leader() != null ? p.leader().id() : null)
                            .replicaNodeIds(p.replicas().stream().map(Node::id).collect(Collectors.toList()))
                            .inSyncReplicaNodeIds(p.isr().stream().map(Node::id).collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList());

            int replFactor = !desc.partitions().isEmpty() ? desc.partitions().get(0).replicas().size() : 0;

            // Obtener configs
            Map<String, String> configMap = new HashMap<>();
            try {
                ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName.trim());
                Map<ConfigResource, Config> configs = adminClient.describeConfigs(List.of(resource))
                        .all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Config config = configs.get(resource);
                if (config != null) {
                    config.entries().forEach(entry -> configMap.put(entry.name(), entry.value()));
                }
            } catch (Exception ex) {
                log.debug("No se pudieron leer configs del tópico {}: {}", topicName, ex.getMessage());
            }

            return KafkaTopicDetailDto.builder()
                    .name(desc.name())
                    .partitionsCount(desc.partitions().size())
                    .replicationFactor(replFactor)
                    .isProtected(properties.isProtectedTopic(topicName.trim()))
                    .partitions(partitionDetails)
                    .configuration(configMap)
                    .build();

        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            log.error("[KAFKA-ADMIN] Error al describir tópico [{}]: {}", topicName, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se pudo consultar el tópico " + topicName + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Crea un nuevo tópico con validaciones y configuraciones avanzadas.
     */
    public KafkaOperationResponse createTopic(CreateKafkaTopicRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del tópico es requerido");
        }

        String name = request.getName().trim();

        // Validar si ya existe
        try {
            Set<String> existing = adminClient.listTopics().names().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (existing.contains(name)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El tópico " + name + " ya existe en el clúster");
            }

            NewTopic newTopic = new NewTopic(name, request.getPartitions(), request.getReplicationFactor());

            Map<String, String> configs = new HashMap<>();
            if (request.getRetentionMs() != null) {
                configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(request.getRetentionMs()));
            }
            if (request.getCleanupPolicy() != null && !request.getCleanupPolicy().isBlank()) {
                configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, request.getCleanupPolicy().trim().toLowerCase());
            }
            if (request.getAdditionalConfigs() != null) {
                configs.putAll(request.getAdditionalConfigs());
            }

            newTopic.configs(configs);

            adminClient.createTopics(List.of(newTopic)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("[KAFKA-ADMIN] Tópico creado administrativamente: [{}], particiones [{}], réplicas [{}]",
                    name, request.getPartitions(), request.getReplicationFactor());

            return KafkaOperationResponse.builder()
                    .status("SUCCESS")
                    .message("Tópico '" + name + "' creado exitosamente con " + request.getPartitions() +
                            " particiones y factor de réplica " + request.getReplicationFactor())
                    .timestamp(Instant.now())
                    .details(Map.of(
                            "topic", name,
                            "partitions", request.getPartitions(),
                            "replicationFactor", request.getReplicationFactor(),
                            "configs", configs
                    ))
                    .build();

        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            log.error("[KAFKA-ADMIN] Error al crear tópico [{}]: {}", name, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al crear tópico en Kafka: " + ex.getMessage(), ex);
        }
    }

    /**
     * Modifica parámetros de configuración de un tópico (ej. retention.ms, cleanup.policy).
     */
    public KafkaOperationResponse alterTopicConfig(String topicName, AlterKafkaTopicConfigRequest request) {
        if (topicName == null || topicName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del tópico no puede estar vacío");
        }
        if (request == null || request.getConfigs() == null || request.getConfigs().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar al menos una configuración");
        }

        String name = topicName.trim();
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);

        List<AlterConfigOp> operations = request.getConfigs().entrySet().stream()
                .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .collect(Collectors.toList());

        try {
            adminClient.incrementalAlterConfigs(Map.of(resource, operations)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("[KAFKA-ADMIN] Configuraciones modificadas para tópico [{}]: {}", name, request.getConfigs());

            return KafkaOperationResponse.builder()
                    .status("SUCCESS")
                    .message("Configuración actualizada exitosamente para el tópico '" + name + "'")
                    .timestamp(Instant.now())
                    .details(Map.of("topic", name, "updatedConfigs", request.getConfigs()))
                    .build();

        } catch (Exception ex) {
            log.error("[KAFKA-ADMIN] Error al alterar configs de [{}]: {}", name, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo actualizar la configuración del tópico " + name + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Elimina un tópico asegurando que NO sea un tópico esencial protegido del sistema.
     */
    public KafkaOperationResponse deleteTopic(String topicName) {
        if (topicName == null || topicName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del tópico no puede estar vacío");
        }

        String name = topicName.trim();

        if (properties.isProtectedTopic(name)) {
            log.warn("[KAFKA-ADMIN] Intento denegado de eliminación para el tópico protegido [{}]", name);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El tópico '" + name + "' es un recurso esencial protegido de VidaSalud y no puede ser eliminado");
        }

        try {
            adminClient.deleteTopics(List.of(name)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.warn("[KAFKA-ADMIN] Tópico eliminado administrativamente: [{}]", name);

            return KafkaOperationResponse.builder()
                    .status("SUCCESS")
                    .message("Tópico '" + name + "' eliminado exitosamente del clúster")
                    .timestamp(Instant.now())
                    .details(Map.of("topic", name))
                    .build();

        } catch (Exception ex) {
            log.error("[KAFKA-ADMIN] Error al eliminar tópico [{}]: {}", name, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo eliminar el tópico " + name + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Inspecciona todos los consumer groups activos, estado, asignación de particiones y cálculo de lag.
     */
    public List<KafkaConsumerGroupDto> listConsumerGroups() {
        try {
            Collection<ConsumerGroupListing> groupListings = adminClient.listConsumerGroups().all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (groupListings.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> groupIds = groupListings.stream().map(ConsumerGroupListing::groupId).collect(Collectors.toList());

            Map<String, ConsumerGroupDescription> descriptions = adminClient.describeConsumerGroups(groupIds)
                    .all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<KafkaConsumerGroupDto> result = new ArrayList<>();

            for (String groupId : groupIds) {
                ConsumerGroupDescription desc = descriptions.get(groupId);
                String state = desc != null ? desc.state().toString() : "UNKNOWN";
                int members = desc != null ? desc.members().size() : 0;

                // Obtener offsets del grupo
                Map<TopicPartition, OffsetAndMetadata> groupOffsets = adminClient.listConsumerGroupOffsets(groupId)
                        .partitionsToOffsetAndMetadata().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                long totalLag = 0;
                List<KafkaConsumerGroupDto.GroupPartitionAssignment> partitionAssignments = new ArrayList<>();

                if (desc != null && !groupOffsets.isEmpty()) {
                    // Obtener log end offsets para calcular lag
                    Map<TopicPartition, OffsetSpec> offsetSpecs = groupOffsets.keySet().stream()
                            .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

                    Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                            adminClient.listOffsets(offsetSpecs).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                    for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : groupOffsets.entrySet()) {
                        TopicPartition tp = entry.getKey();
                        long currentOffset = entry.getValue() != null ? entry.getValue().offset() : 0L;
                        long endOffset = endOffsets.containsKey(tp) ? endOffsets.get(tp).offset() : currentOffset;
                        long lag = Math.max(0, endOffset - currentOffset);
                        totalLag += lag;

                        // Buscar miembro asignado
                        String memberId = "-";
                        String clientId = "-";
                        String host = "-";
                        for (MemberDescription member : desc.members()) {
                            if (member.assignment() != null && member.assignment().topicPartitions().contains(tp)) {
                                memberId = member.consumerId();
                                clientId = member.clientId();
                                host = member.host();
                                break;
                            }
                        }

                        partitionAssignments.add(KafkaConsumerGroupDto.GroupPartitionAssignment.builder()
                                .topic(tp.topic())
                                .partition(tp.partition())
                                .currentOffset(currentOffset)
                                .logEndOffset(endOffset)
                                .lag(lag)
                                .memberId(memberId)
                                .clientId(clientId)
                                .host(host)
                                .build());
                    }
                }

                result.add(KafkaConsumerGroupDto.builder()
                        .groupId(groupId)
                        .state(state)
                        .protocolType(desc != null && desc.partitionAssignor() != null ? desc.partitionAssignor() : "consumer")
                        .membersCount(members)
                        .totalLag(totalLag)
                        .partitions(partitionAssignments)
                        .build());
            }

            result.sort(Comparator.comparing(KafkaConsumerGroupDto::getGroupId));
            return result;

        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            log.error("[KAFKA-ADMIN] Error al inspeccionar consumer groups: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudieron inspeccionar los consumer groups de Kafka: " + ex.getMessage(), ex);
        }
    }

    /**
     * Publica un evento de prueba hacia el streaming de Kafka para demostración inmediata en laboratorios/defensa.
     */
    public KafkaOperationResponse publishTestEvent(PublishTestKafkaEventRequest request) {
        if (request == null || request.getEventType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El eventType ('ATENCIONES' o 'TELEMETRIA') es requerido");
        }

        String type = request.getEventType().trim().toUpperCase();
        Instant now = Instant.now();
        String traceId = "trc-" + UUID.randomUUID().toString().substring(0, 8);

        if ("ATENCIONES".equals(type)) {
            String specialty = (request.getSpecialty() != null && !request.getSpecialty().isBlank())
                    ? request.getSpecialty().trim()
                    : "Cardiología";
            String status = (request.getStatus() != null && !request.getStatus().isBlank())
                    ? request.getStatus().trim()
                    : "CONFIRMED";
            int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : 30;

            AtencionMetricaEvent event = AtencionMetricaEvent.builder()
                    .eventId("evt-sim-" + UUID.randomUUID().toString().substring(0, 8))
                    .specialty(specialty)
                    .status(status)
                    .durationMinutes(duration)
                    .timestamp(now)
                    .traceId(traceId)
                    .build();

            atencionMetricaProducer.publishAtencionMetrica(event);

            return KafkaOperationResponse.builder()
                    .status("SUCCESS")
                    .message("Evento AtencionMetricaEvent publicado hacia " + properties.getTopics().getAtencionesStream())
                    .timestamp(now)
                    .details(Map.of(
                            "topic", properties.getTopics().getAtencionesStream(),
                            "event", event
                    ))
                    .build();

        } else if ("TELEMETRIA".equals(type)) {
            String component = (request.getSourceComponent() != null && !request.getSourceComponent().isBlank())
                    ? request.getSourceComponent().trim()
                    : "BFF-AppointmentService";
            String op = (request.getOperationType() != null && !request.getOperationType().isBlank())
                    ? request.getOperationType().trim()
                    : "QUERY_CATALOG";
            long latency = request.getExecutionTimeMs() != null ? request.getExecutionTimeMs() : (long)(50 + Math.random() * 250);
            int code = request.getStatusCode() != null ? request.getStatusCode() : 200;
            String severity = (request.getSeverity() != null && !request.getSeverity().isBlank())
                    ? request.getSeverity().trim().toUpperCase()
                    : "INFO";

            TelemetriaClinicaEvent event = TelemetriaClinicaEvent.builder()
                    .telemetryId("tel-sim-" + UUID.randomUUID().toString().substring(0, 8))
                    .sourceComponent(component)
                    .operationType(op)
                    .executionTimeMs(latency)
                    .statusCode(code)
                    .serverNode("node-bff-" + (1 + (int)(Math.random() * 3)))
                    .severity(severity)
                    .systemLoadPct(25.5 + Math.random() * 20.0)
                    .timestamp(now)
                    .traceId(traceId)
                    .details("Evento de telemetría de prueba generado para verificación de streaming en Kafka")
                    .build();

            telemetriaClinicaProducer.publishTelemetria(event);

            return KafkaOperationResponse.builder()
                    .status("SUCCESS")
                    .message("Evento TelemetriaClinicaEvent publicado hacia " + properties.getTopics().getTelemetriaStream())
                    .timestamp(now)
                    .details(Map.of(
                            "topic", properties.getTopics().getTelemetriaStream(),
                            "event", event
                    ))
                    .build();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de evento desconocido: '" + type + "'. Use 'ATENCIONES' o 'TELEMETRIA'");
        }
    }
}
