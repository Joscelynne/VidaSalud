package cl.duoc.vidasalud.bff.consumer;

import cl.duoc.vidasalud.bff.event.TelemetriaClinicaEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumidor Kafka para telemetría y detección de alertas operacionales en tiempo real (EP5 / EP6).
 * Consumer Group: 'vidasalud-alertas-group'.
 * Monitorea latencia, fallos de infraestructura y clasifica eventos por severidad (INFO, WARN, CRITICAL).
 */
@Slf4j
@Component
public class TelemetriaClinicaConsumer {

    @Getter
    private final AtomicLong totalTelemetryProcessed = new AtomicLong(0);

    @Getter
    private final AtomicLong alertsTriggered = new AtomicLong(0);

    @KafkaListener(
            topics = "${vidasalud.kafka.topics.telemetria-stream:vidasalud.telemetria.stream}",
            groupId = "${vidasalud.kafka.consumer-groups.alertas-group:vidasalud-alertas-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTelemetria(
            @Payload TelemetriaClinicaEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {

        log.debug("[KAFKA-CONSUMER-ALERTAS] Telemetría recibida desde [{}], partición [{}], offset [{}], key [{}]",
                topic, partition, offset, key);

        validateEvent(event);
        processTelemetry(event, topic, partition, offset);
    }

    private void validateEvent(TelemetriaClinicaEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento TelemetriaClinicaEvent no puede ser nulo");
        }
        if (event.getTelemetryId() == null || event.getTelemetryId().isBlank()) {
            throw new IllegalArgumentException("El telemetryId no puede estar vacío");
        }
        if (event.getExecutionTimeMs() == null || event.getExecutionTimeMs() < 0) {
            throw new IllegalArgumentException("El executionTimeMs debe ser un valor positivo");
        }
    }

    private void processTelemetry(TelemetriaClinicaEvent event, String topic, int partition, long offset) {
        long total = totalTelemetryProcessed.incrementAndGet();

        boolean isHighLatency = event.getExecutionTimeMs() != null && event.getExecutionTimeMs() > 2000L;
        boolean isServerError = event.getStatusCode() != null && event.getStatusCode() >= 500;
        boolean isCriticalSeverity = "CRITICAL".equalsIgnoreCase(event.getSeverity());

        if (isHighLatency || isServerError || isCriticalSeverity) {
            long alertNum = alertsTriggered.incrementAndGet();
            log.warn("[KAFKA-ALERTA-OPERACIONAL #{}] Anomalía detectada en componente [{}], operación [{}], latencia [{} ms], status [{}], severidad [{}], partición [{}], offset [{}]",
                    alertNum,
                    event.getSourceComponent(),
                    event.getOperationType(),
                    event.getExecutionTimeMs(),
                    event.getStatusCode(),
                    event.getSeverity(),
                    partition,
                    offset);
        } else {
            log.info("[KAFKA-TELEMETRIA-OK] Métrica procesada en [{}]: componente [{}], operación [{}], latencia [{} ms], partición [{}], offset [{}]. Total: [{}]",
                    topic,
                    event.getSourceComponent(),
                    event.getOperationType(),
                    event.getExecutionTimeMs(),
                    partition,
                    offset,
                    total);
        }
    }
}
