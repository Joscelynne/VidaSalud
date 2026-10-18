package cl.duoc.vidasalud.bff.config;

/** AMQP protocol header names; application topology is configured in application.yml. */
public final class RabbitMQConstants {
    private RabbitMQConstants() { }

    public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
}
