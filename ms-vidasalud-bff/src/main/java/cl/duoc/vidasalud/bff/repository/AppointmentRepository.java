package cl.duoc.vidasalud.bff.repository;

import cl.duoc.vidasalud.bff.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Appointment.
 * Proporciona operaciones CRUD sobre la tabla appointments en PostgreSQL.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    /**
     * Busca appointments por estado.
     * @param status estado de la cita (PENDING, CONFIRMED, COMPLETED, CANCELLED)
     * @return lista de appointments con el estado especificado
     */
    List<Appointment> findByStatus(String status);

    /**
     * Busca appointments por nombre de paciente (búsqueda parcial, case-insensitive).
     * @param patientName nombre del paciente
     * @return lista de appointments del paciente
     */
    List<Appointment> findByPatientNameContainingIgnoreCase(String patientName);
}
