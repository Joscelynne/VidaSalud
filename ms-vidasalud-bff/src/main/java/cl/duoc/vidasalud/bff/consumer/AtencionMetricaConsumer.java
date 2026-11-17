package cl.duoc.vidasalud.bff.consumer;

import cl.duoc.vidasalud.bff.event.AtencionMetricaEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumidor Kafka para analítica de atenciones médicas en tiempo real (EP5 / EP6).
 * Consumer Group: 'vidasalud-analitica-group'.
 * Procesa el stream de atenciones, computa métricas operacionales agregadas por especialidad
 * y demuestra balanceo y avance de offsets en Kafka UI.
 */
@Slf4j
@Component
public class AtencionMetricaConsumer {

    @Getter
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);

    @Getter
    private final Map<String, AtomicLong> specialtyCounts = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = "${vidasalud.kafka.topics.atenciones-stream:vidasalud.atenciones.stream}",
            groupId = "${vidasalud.kafka.consumer-groups.analitica-group:vidasalud-analitica-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAtencionMetrica(
            @Payload AtencionMetricaEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {

        log.debug("[KAFKA-CONSUMER-ANALITICA] Mensaje recibido desde [{}], partición [{}], offset [{}], key [{}]",
                topic, partition, offset, key);

        validateEvent(event);
        processAnalytics(event, topic, partition, offset);
    }

    private void validateEvent(AtencionMetricaEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento AtencionMetricaEvent no puede ser nulo");
        }
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("El eventId no puede estar vacío");
        }
        if (event.getSpecialty() == null || event.getSpecialty().isBlank()) {
            throw new IllegalArgumentException("La especialidad no puede estar vacía");
        }
        if (event.getStatus() == null || event.getStatus().isBlank()) {
            throw new IllegalArgumentException("El estado no puede estar vacío");
        }
        // Soporte para simular fallos en pruebas de reintentos
        if (event.getEventId().startsWith("FAIL-KAFKA")) {
            throw new IllegalArgumentException("Simulación controlada de fallo para validación de reintentos Kafka");
        }
    }

    private void processAnalytics(AtencionMetricaEvent event, String topic, int partition, long offset) {
        long total = totalEventsProcessed.incrementAndGet();
        String specialty = (event.getSpecialty() != null && !event.getSpecialty().isBlank())
                ? event.getSpecialty()
                : "Sin Especialidad";

        specialtyCounts.computeIfAbsent(specialty, k -> new AtomicLong(0)).incrementAndGet();

        log.info("[KAFKA-CONSUMER-ANALITICA] Evento procesado exitosamente: eventId [{}], especialidad [{}], estado [{}], duración [{} min], partición [{}], offset [{}]. Total procesados: [{}]",
                event.getEventId(),
                specialty,
                event.getStatus(),
                event.getDurationMinutes(),
                partition,
                offset,
                total);
    }
}
