package cl.duoc.vidasalud.bff.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentServicePersistenceTest {

    private AppointmentService appointmentService;
    private EmbeddedDatabase database;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
        new ResourceDatabasePopulator(new ClassPathResource("test-schema.sql")).execute(database);
        appointmentService = new AppointmentService(new JdbcTemplate(database));
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void confirmationSurvivesASeparateReadAndRepeatedConfirmationIsIdempotent() {
        AppointmentService.ConfirmationResult result = appointmentService.confirm("APT-101");

        assertThat(result.newlyConfirmed()).isTrue();
        assertThat(result.appointment().getStatus()).isEqualTo("CONFIRMED");
        assertThat(appointmentService.findById("APT-101")).hasValueSatisfying(appointment ->
                assertThat(appointment.getStatus()).isEqualTo("CONFIRMED"));

        AppointmentService.ConfirmationResult repeated = appointmentService.confirm("APT-101");
        assertThat(repeated.newlyConfirmed()).isFalse();
        assertThat(repeated.appointment().getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void createAppointmentSavesRecordCorrectly() {
        cl.duoc.vidasalud.bff.dto.CreateAppointmentRequest request = new cl.duoc.vidasalud.bff.dto.CreateAppointmentRequest();
        request.setPatientName("Carlos Mora");
        request.setDoctorName("Dra. Beatriz Morales");
        request.setSpecialty("Cardiología");
        request.setDateTime("2026-11-15T10:00:00");
        request.setRoomNumber("Box 204");
        request.setNotes("Control de rutina");

        cl.duoc.vidasalud.bff.dto.AppointmentDto created = appointmentService.create(request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotBlank();
        assertThat(created.getStatus()).isEqualTo("PENDING");
        assertThat(created.getPatientName()).isEqualTo("Carlos Mora");

        assertThat(appointmentService.findById(created.getId())).isPresent();
    }

    @Test
    void revertToPendingRestoresStatus() {
        appointmentService.confirm("APT-101");
        assertThat(appointmentService.findById("APT-101").get().getStatus()).isEqualTo("CONFIRMED");

        appointmentService.revertToPending("APT-101");
        assertThat(appointmentService.findById("APT-101").get().getStatus()).isEqualTo("PENDING");
    }
}
