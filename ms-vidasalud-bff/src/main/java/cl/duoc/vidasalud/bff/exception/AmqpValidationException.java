package cl.duoc.vidasalud.bff.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción para errores de validación en la API administrativa de RabbitMQ.
 * Proporciona código HTTP y mensaje descriptivo para respuestas claras al cliente.
 */
@Getter
public class AmqpValidationException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public AmqpValidationException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    /**
     * Error para nombres vacíos o en blanco.
     */
    public static AmqpValidationException emptyName(String resourceType) {
        return new AmqpValidationException(
                String.format("El nombre de %s no puede estar vacío ni contener solo espacios en blanco", resourceType),
                HttpStatus.BAD_REQUEST,
                "AMQP_EMPTY_NAME"
        );
    }

    /**
     * Error para prefijo reservado 'amq.' de RabbitMQ.
     */
    public static AmqpValidationException reservedPrefix(String name) {
        return new AmqpValidationException(
                String.format("El nombre '%s' usa el prefijo reservado 'amq.' que está reservado por RabbitMQ para uso interno", name),
                HttpStatus.BAD_REQUEST,
                "AMQP_RESERVED_PREFIX"
        );
    }

    /**
     * Error para recursos protegidos de la topología esencial de VidaSalud.
     */
    public static AmqpValidationException protectedResource(String name, String operation) {
        return new AmqpValidationException(
                String.format("No se permite %s el recurso '%s' porque pertenece a la topología esencial de VidaSalud. " +
                        "Los exchanges, colas y DLQs del sistema están protegidos contra modificaciones.", operation, name),
                HttpStatus.FORBIDDEN,
                "AMQP_PROTECTED_RESOURCE"
        );
    }

    /**
     * Error para caracteres inválidos en nombres.
     */
    public static AmqpValidationException invalidCharacters(String name, String resourceType) {
        return new AmqpValidationException(
                String.format("El nombre de %s '%s' contiene caracteres no permitidos. " +
                        "Solo se permiten letras, números, puntos, guiones y guiones bajos.", resourceType, name),
                HttpStatus.BAD_REQUEST,
                "AMQP_INVALID_CHARACTERS"
        );
    }

    /**
     * Error para tipo de exchange no soportado.
     */
    public static AmqpValidationException unsupportedExchangeType(String type) {
        return new AmqpValidationException(
                String.format("El tipo de exchange '%s' no está soportado. Tipos válidos: direct, topic, fanout", type),
                HttpStatus.BAD_REQUEST,
                "AMQP_UNSUPPORTED_EXCHANGE_TYPE"
        );
    }

    /**
     * Error para tipo de destino inválido en bindings.
     */
    public static AmqpValidationException invalidDestinationType(String type) {
        return new AmqpValidationException(
                String.format("El destinationType '%s' no es válido. Debe ser 'QUEUE' o 'EXCHANGE'", type),
                HttpStatus.BAD_REQUEST,
                "AMQP_INVALID_DESTINATION_TYPE"
        );
    }

    /**
     * Error cuando RabbitMQ rechaza silenciosamente la creación de un recurso.
     */
    public static AmqpValidationException brokerRejected(String name, String resourceType) {
        return new AmqpValidationException(
                String.format("El broker RabbitMQ rechazó la creación de %s '%s'. " +
                        "Esto puede ocurrir si el nombre viola políticas internas del broker.", resourceType, name),
                HttpStatus.BAD_REQUEST,
                "AMQP_BROKER_REJECTED"
        );
    }
}
