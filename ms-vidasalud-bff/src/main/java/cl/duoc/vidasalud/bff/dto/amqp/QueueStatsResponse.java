package cl.duoc.vidasalud.bff.dto.amqp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representación de estadísticas obtenidas mediante RabbitAdmin (protocolo AMQP).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatsResponse {

    private String name;
    private boolean exists;
    private int messageCount;
    private int consumerCount;
    private String apiSource;
    private String details;
}
