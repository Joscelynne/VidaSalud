package cl.duoc.vidasalud.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representación de una notificación de negocio derivada de eventos RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String id;
    private String eventId;
    private String type;
    private String title;
    private String message;
    private String appointmentId;
    private String patientName;
    private String status;
    private boolean read;
    private String createdAt;
}
