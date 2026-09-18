package cl.duoc.vidasalud.bff.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA para la tabla appointments en PostgreSQL.
 * Representa una cita médica en el sistema VidaSalud.
 */
@Entity
@Table(name = "appointments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @Column(name = "patient_name", nullable = false, length = 200)
    private String patientName;

    @Column(name = "doctor_name", nullable = false, length = 200)
    private String doctorName;

    @Column(name = "specialty", nullable = false, length = 100)
    private String specialty;

    @Column(name = "date_time", nullable = false, length = 50)
    private String dateTime;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "room_number", length = 50)
    private String roomNumber;

    @Column(name = "notes", length = 500)
    private String notes;
}
