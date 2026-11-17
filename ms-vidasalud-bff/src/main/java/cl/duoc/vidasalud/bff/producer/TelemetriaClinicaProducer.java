package cl.duoc.vidasalud.bff.producer;

import cl.duoc.vidasalud.bff.config.KafkaProperties;
import cl.duoc.vidasalud.bff.event.TelemetriaClinicaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Productor Kafka para eventos de telemetría y salud operacional de VidaSalud.
 * Publica hacia el tópico 'vidasalud.telemetria.stream' utilizando el componente de origen
 * como clave de particionado para ordenar la telemetría por microservicio/módulo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetriaClinicaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProperties properties;

    public CompletableFuture<SendResult<String, Object>> publishTelemetria(TelemetriaClinicaEvent event) {
        String topic = properties.getTopics().getTelemetriaStream();
        String partitionKey = (event.getSourceComponent() != null && !event.getSourceComponent().isBlank())
                ? event.getSourceComponent()
                : "vidasalud-bff";

        log.info("[KAFKA-PRODUCER] Publicando TelemetriaClinicaEvent hacia tópico [{}], key [{}], telemetryId [{}], operación [{}], latencia [{} ms]",
                topic, partitionKey, event.getTelemetryId(), event.getOperationType(), event.getExecutionTimeMs());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, partitionKey, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[KAFKA-PRODUCER] TelemetriaClinicaEvent entregado con éxito a [{}], partición [{}], offset [{}]",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("[KAFKA-PRODUCER] Fallo al publicar TelemetriaClinicaEvent en [{}]: {}",
                        topic, ex.getMessage(), ex);
            }
        });

        return future;
    }
}
