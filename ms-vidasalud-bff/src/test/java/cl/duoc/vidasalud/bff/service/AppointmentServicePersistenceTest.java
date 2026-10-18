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
}
