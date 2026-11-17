package cl.duoc.vidasalud.bff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Propiedades centralizadas para la infraestructura de Apache Kafka en VidaSalud (EP5 / EP6).
 * Permite configurar tópicos, particiones, réplicas, retención y consumer groups.
 */
@Configuration
@ConfigurationProperties(prefix = "vidasalud.kafka")
@Getter
@Setter
public class KafkaProperties {

    private boolean enabled = true;
    private String bootstrapServers = "localhost:9092,localhost:9093,localhost:9094";
    private Topics topics = new Topics();
    private int partitions = 3;
    private short replicas = 3;
    private long retentionAtencionesMs = 604800000L;  // 7 días
    private long retentionTelemetriaMs = 259200000L;  // 3 días
    private String cleanupPolicy = "delete";
    private ConsumerGroups consumerGroups = new ConsumerGroups();

    @Getter
    @Setter
    public static class Topics {
        private String atencionesStream = "vidasalud.atenciones.stream";
        private String telemetriaStream = "vidasalud.telemetria.stream";
    }

    @Getter
    @Setter
    public static class ConsumerGroups {
        private String analiticaGroup = "vidasalud-analitica-group";
        private String alertasGroup = "vidasalud-alertas-group";
    }

    /**
     * Retorna el conjunto de tópicos esenciales protegidos que no pueden ser eliminados administrativamente.
     */
    public Set<String> getProtectedTopics() {
        return Set.of(
                topics.getAtencionesStream(),
                topics.getTelemetriaStream()
        );
    }

    public boolean isProtectedTopic(String topicName) {
        return topicName != null && getProtectedTopics().contains(topicName.trim());
    }
}
