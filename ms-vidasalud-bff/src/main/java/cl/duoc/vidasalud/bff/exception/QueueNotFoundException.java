package cl.duoc.vidasalud.bff.exception;

/**
 * Excepción lanzada cuando una cola solicitada no existe en el broker RabbitMQ.
 */
public class QueueNotFoundException extends RuntimeException {

    public QueueNotFoundException(String queueName) {
        super(String.format("La cola '%s' no existe en el broker RabbitMQ", queueName));
    }
}
