package cl.duoc.vidasalud.bff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuración de la topología esencial de RabbitMQ para VidaSalud.
 * Define los exchanges, colas y DLQs que NO pueden ser eliminados ni modificados
 * a través de la API administrativa, ya que son parte crítica del sistema.
 */
@Configuration
@ConfigurationProperties(prefix = "vidasalud.rabbitmq")
@Getter
@Setter
public class VidaSaludTopologyConfig {

    private Exchanges exchanges = new Exchanges();
    private Queues queues = new Queues();
    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class Exchanges {
        private String direct = "vidasalud.direct.exchange";
        private String topic = "vidasalud.topic.exchange";
        private String dlx = "vidasalud.dlx.exchange";
    }

    @Getter
    @Setter
    public static class Queues {
        private String appointmentCreation = "citas.agendamiento.queue";
        private String appointmentConfirmation = "citas.confirmacion.queue";
        private String notificationAudit = "notificaciones.auditoria.queue";
        private String appointmentCreationDlq = "citas.agendamiento.dlq";
        private String appointmentConfirmationDlq = "citas.confirmacion.dlq";
        private String notificationAuditDlq = "notificaciones.auditoria.dlq";
    }

    @Getter
    @Setter
    public static class RoutingKeys {
        private String appointmentCreate = "citas.crear";
        private String appointmentConfirm = "citas.confirmar";
        private String notificationAudit = "vidasalud.notificacion.*";
        private String notificationAuditDefault = "vidasalud.notificacion.audit";
        private String appointmentCreationDlq = "citas.agendamiento.dlq.rk";
        private String appointmentConfirmationDlq = "citas.confirmacion.dlq.rk";
        private String notificationAuditDlq = "notificaciones.auditoria.dlq.rk";
    }

    /**
     * Retorna el conjunto de todos los exchanges protegidos del sistema.
     */
    public Set<String> getProtectedExchanges() {
        return Set.of(
                exchanges.getDirect(),
                exchanges.getTopic(),
                exchanges.getDlx()
        );
    }

    /**
     * Retorna el conjunto de todas las colas protegidas del sistema (incluye DLQs).
     */
    public Set<String> getProtectedQueues() {
        return Set.of(
                queues.getAppointmentCreation(),
                queues.getAppointmentConfirmation(),
                queues.getNotificationAudit(),
                queues.getAppointmentCreationDlq(),
                queues.getAppointmentConfirmationDlq(),
                queues.getNotificationAuditDlq()
        );
    }

    /**
     * Retorna el conjunto combinado de todos los recursos protegidos.
     */
    public Set<String> getAllProtectedResources() {
        return Stream.concat(
                getProtectedExchanges().stream(),
                getProtectedQueues().stream()
        ).collect(Collectors.toSet());
    }

    /**
     * Verifica si un nombre de exchange está protegido.
     */
    public boolean isProtectedExchange(String name) {
        return name != null && getProtectedExchanges().contains(name.trim());
    }

    /**
     * Verifica si un nombre de cola está protegido.
     */
    public boolean isProtectedQueue(String name) {
        return name != null && getProtectedQueues().contains(name.trim());
    }

    /**
     * Verifica si un recurso (exchange o cola) está protegido.
     */
    public boolean isProtectedResource(String name) {
        return name != null && getAllProtectedResources().contains(name.trim());
    }
}
