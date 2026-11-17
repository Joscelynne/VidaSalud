package cl.duoc.vidasalud.bff.producer;

import cl.duoc.vidasalud.bff.config.KafkaProperties;
import cl.duoc.vidasalud.bff.event.AtencionMetricaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Productor Kafka para eventos de ciclo de vida y métricas de atenciones médicas.
 * Publica hacia el tópico 'vidasalud.atenciones.stream' utilizando la especialidad médica
 * como clave de particionado para garantizar orden relativo y balanceo entre las 3 particiones.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AtencionMetricaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProperties properties;

    public CompletableFuture<SendResult<String, Object>> publishAtencionMetrica(AtencionMetricaEvent event) {
        String topic = properties.getTopics().getAtencionesStream();
        String partitionKey = (event.getSpecialty() != null && !event.getSpecialty().isBlank())
                ? event.getSpecialty()
                : event.getEventId();

        log.info("[KAFKA-PRODUCER] Publicando AtencionMetricaEvent hacia tópico [{}], key [{}], eventId [{}], estado [{}]",
                topic, partitionKey, event.getEventId(), event.getStatus());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, partitionKey, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[KAFKA-PRODUCER] AtencionMetricaEvent entregado con éxito a [{}], partición [{}], offset [{}]",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("[KAFKA-PRODUCER] Fallo al publicar AtencionMetricaEvent en [{}]: {}",
                        topic, ex.getMessage(), ex);
            }
        });

        return future;
    }
}
