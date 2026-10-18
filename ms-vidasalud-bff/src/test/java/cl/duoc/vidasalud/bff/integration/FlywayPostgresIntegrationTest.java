package cl.duoc.vidasalud.bff.integration;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración que valida:
 * 1. Flyway ejecuta migraciones V1 y V2 correctamente con baseline-version=0
 * 2. La migración V2 copia datos de forma aditiva e idempotente
 * 3. El flujo de confirmación (UPDATE) persiste el estado en PostgreSQL
 * 4. SELECT posterior devuelve CONFIRMED
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlywayPostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("vidasalud_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void setUp() {
        DataSource dataSource = createDataSource();
        jdbcTemplate = new JdbcTemplate(dataSource);
        
        // Simular la tabla appointments existente en RDS (antes de Flyway)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS appointments (
                id VARCHAR(50) PRIMARY KEY,
                patient_name VARCHAR(200) NOT NULL,
                doctor_name VARCHAR(200) NOT NULL,
                specialty VARCHAR(100) NOT NULL,
                date_time VARCHAR(50) NOT NULL,
                status VARCHAR(50) NOT NULL,
                room_number VARCHAR(50),
                notes VARCHAR(500)
            )
        """);
        
        // Insertar los 3 registros existentes (simulando RDS)
        jdbcTemplate.execute("""
            INSERT INTO appointments (id, patient_name, doctor_name, specialty, date_time, status, room_number, notes)
            VALUES 
                ('APT-101', 'Carlos Mendoza', 'Dra. María Paz', 'Medicina General', '2026-09-09T17:00:00', 'PENDING', 'Box 204', 'Chequeo preventivo anual'),
                ('APT-102', 'Lorena Silva', 'Dr. Roberto Gómez', 'Cardiología', '2026-09-09T17:30:00', 'CONFIRMED', 'Box 108', 'Electrocardiograma de control'),
                ('APT-103', 'Felipe Araya', 'Dra. Camila Morales', 'Pediatría', '2026-09-10T11:00:00', 'COMPLETED', 'Box 301', 'Control de niño sano')
            ON CONFLICT (id) DO NOTHING
        """);

        // Ejecutar Flyway con baseline-version=0
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        
        flyway.migrate();
    }

    private static DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        return dataSource;
    }

    @Test
    @Order(1)
    @DisplayName("Flyway crea flyway_schema_history con baseline 0")
    void flywayCreatesSchemaHistoryWithBaseline() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '0'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("Flyway ejecuta V1: crea tabla vidasalud_appointments")
    void flywayExecutesV1Migration() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1'", Integer.class);
        assertThat(count).isEqualTo(1);
        
        // Verificar que la tabla existe
        Integer tableExists = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM information_schema.tables 
            WHERE table_name = 'vidasalud_appointments'
        """, Integer.class);
        assertThat(tableExists).isEqualTo(1);
    }

    @Test
    @Order(3)
    @DisplayName("Flyway ejecuta V2: copia datos de appointments a vidasalud_appointments")
    void flywayExecutesV2MigrationAndCopiesData() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '2'", Integer.class);
        assertThat(count).isEqualTo(1);
        
        // Verificar que los 3 registros fueron copiados
        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vidasalud_appointments", Integer.class);
        assertThat(rowCount).isEqualTo(3);
    }

    @Test
    @Order(4)
    @DisplayName("La tabla appointments original permanece intacta")
    void originalAppointmentsTableRemainsIntact() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointments", Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    @Order(5)
    @DisplayName("V2 es idempotente: ejecutar dos veces no duplica datos")
    void v2MigrationIsIdempotent() {
        // Ejecutar el INSERT de V2 manualmente otra vez
        jdbcTemplate.execute("""
            INSERT INTO vidasalud_appointments 
                (id, patient_name, doctor_name, specialty, date_time, status, room_number, notes, created_at, updated_at)
            SELECT 
                id, patient_name, doctor_name, specialty, date_time, status, room_number, 
                COALESCE(notes, ''), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM appointments a
            WHERE NOT EXISTS (
                SELECT 1 FROM vidasalud_appointments va WHERE va.id = a.id
            )
        """);
        
        // Verificar que sigue habiendo solo 3 registros
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vidasalud_appointments", Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    @Order(6)
    @DisplayName("SELECT devuelve las 3 citas migradas con datos correctos")
    void selectReturnsAllMigratedAppointments() {
        var appointments = jdbcTemplate.queryForList(
                "SELECT id, patient_name, status FROM vidasalud_appointments ORDER BY id");
        
        assertThat(appointments).hasSize(3);
        assertThat(appointments.get(0).get("id")).isEqualTo("APT-101");
        assertThat(appointments.get(0).get("patient_name")).isEqualTo("Carlos Mendoza");
    }

    @Test
    @Order(7)
    @DisplayName("UPDATE confirma APT-101 y persiste el estado CONFIRMED")
    void updateConfirmsAppointmentAndPersistsStatus() {
        // APT-101 tiene status PENDING (migrado desde appointments)
        int updated = jdbcTemplate.update("""
            UPDATE vidasalud_appointments
            SET status = 'CONFIRMED', updated_at = CURRENT_TIMESTAMP
            WHERE id = 'APT-101' AND status = 'PENDING'
        """);
        
        assertThat(updated).isEqualTo(1);
    }

    @Test
    @Order(8)
    @DisplayName("SELECT posterior a confirmación devuelve CONFIRMED")
    void selectAfterConfirmationReturnsCONFIRMED() {
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM vidasalud_appointments WHERE id = 'APT-101'", String.class);
        
        assertThat(status).isEqualTo("CONFIRMED");
    }

    @Test
    @Order(9)
    @DisplayName("UPDATE repetido no modifica (idempotente)")
    void repeatedUpdateIsIdempotent() {
        int updated = jdbcTemplate.update("""
            UPDATE vidasalud_appointments
            SET status = 'CONFIRMED', updated_at = CURRENT_TIMESTAMP
            WHERE id = 'APT-101' AND status = 'PENDING'
        """);
        
        // Ya estaba CONFIRMED, no hay filas con PENDING
        assertThat(updated).isEqualTo(0);
    }

    @Test
    @Order(10)
    @DisplayName("El status CONFIRMED sobrevive a una nueva lectura")
    void confirmedStatusSurvivesNewRead() {
        // Verificar directamente en la BD que el status persiste
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM vidasalud_appointments WHERE id = 'APT-101'", String.class);
        
        assertThat(status).isEqualTo("CONFIRMED");
    }
}
