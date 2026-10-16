package cl.duoc.vidasalud.bff.config;

/**
 * Constantes de topología RabbitMQ para VidaSalud.
 * Define exchanges, colas principales, colas DLQ y routing keys del sistema.
 */
public final class RabbitMQConstants {

    private RabbitMQConstants() {
        // Clase de constantes, no instanciable
    }

    // =========================================================================
    // EXCHANGES
    // =========================================================================
    public static final String EXCHANGE_DIRECT = "vidasalud.direct.exchange";
    public static final String EXCHANGE_TOPIC = "vidasalud.topic.exchange";
    public static final String EXCHANGE_DLX = "vidasalud.dlx.exchange";

    // =========================================================================
    // COLAS PRINCIPALES (DURABLES)
    // =========================================================================
    public static final String QUEUE_APPOINTMENT_CREATION = "citas.agendamiento.queue";
    public static final String QUEUE_APPOINTMENT_CONFIRMATION = "citas.confirmacion.queue";
    public static final String QUEUE_NOTIFICATION_AUDIT = "notificaciones.auditoria.queue";

    // =========================================================================
    // DEAD LETTER QUEUES (DLQ - DURABLES)
    // =========================================================================
    public static final String QUEUE_APPOINTMENT_CREATION_DLQ = "citas.agendamiento.dlq";
    public static final String QUEUE_APPOINTMENT_CONFIRMATION_DLQ = "citas.confirmacion.dlq";
    public static final String QUEUE_NOTIFICATION_AUDIT_DLQ = "notificaciones.auditoria.dlq";

    // =========================================================================
    // ROUTING KEYS PRINCIPALES
    // =========================================================================
    public static final String RK_APPOINTMENT_CREATE = "citas.crear";
    public static final String RK_APPOINTMENT_CONFIRM = "citas.confirmar";
    public static final String RK_NOTIFICATION_TOPIC_PATTERN = "vidasalud.notificacion.*";
    public static final String RK_NOTIFICATION_AUDIT_DEFAULT = "vidasalud.notificacion.audit";

    // =========================================================================
    // ROUTING KEYS DE DEAD LETTER QUEUES
    // =========================================================================
    public static final String RK_APPOINTMENT_CREATION_DLQ = "citas.agendamiento.dlq.rk";
    public static final String RK_APPOINTMENT_CONFIRMATION_DLQ = "citas.confirmacion.dlq.rk";
    public static final String RK_NOTIFICATION_AUDIT_DLQ = "notificaciones.auditoria.dlq.rk";

    // =========================================================================
    // ARGUMENTOS AMQP PARA DEAD LETTER
    // =========================================================================
    public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
}
