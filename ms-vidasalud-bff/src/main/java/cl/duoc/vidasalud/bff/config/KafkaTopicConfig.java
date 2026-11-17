package cl.duoc.vidasalud.bff.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Declaración de tópicos para Apache Kafka en VidaSalud (EP5 / EP6).
 * Declara programáticamente los dos tópicos requeridos con 3 particiones y factor de réplica 3,
 * configurando políticas de retención y limpieza.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(KafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        KafkaAdmin admin = new KafkaAdmin(configs);
        // Evita que la aplicación falle al iniciar si el cluster Kafka aún no está listo o en tests
        admin.setFatalIfBrokerNotAvailable(false);
        admin.setAutoCreate(true);
        return admin;
    }

    @Bean
    public AdminClient kafkaNativeAdminClient(KafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 10000);
        return AdminClient.create(configs);
    }

    /**
     * Tópico 1: vidasalud.atenciones.stream
     * - Particiones: 3 (paralelismo y distribución)
     * - Factor de réplica: 3 (alta disponibilidad y tolerancia a fallos)
     * - Retención: 7 días (604800000 ms)
     * - Cleanup Policy: delete
     */
    @Bean
    @ConditionalOnProperty(name = "vidasalud.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic atencionesStreamTopic(KafkaProperties properties) {
        return TopicBuilder.name(properties.getTopics().getAtencionesStream())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(properties.getRetentionAtencionesMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, properties.getCleanupPolicy())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }

    /**
     * Tópico 2: vidasalud.telemetria.stream
     * - Particiones: 3 (distribución de carga de telemetría de alta frecuencia)
     * - Factor de réplica: 3 (tolerancia ante caída de hasta 2 brokers con min.insync=2)
     * - Retención: 3 días (259200000 ms)
     * - Cleanup Policy: delete
     */
    @Bean
    @ConditionalOnProperty(name = "vidasalud.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic telemetriaStreamTopic(KafkaProperties properties) {
        return TopicBuilder.name(properties.getTopics().getTelemetriaStream())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(properties.getRetentionTelemetriaMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, properties.getCleanupPolicy())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }
}
