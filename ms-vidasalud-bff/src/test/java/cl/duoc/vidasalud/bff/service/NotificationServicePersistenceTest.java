package cl.duoc.vidasalud.bff.service;

import cl.duoc.vidasalud.bff.dto.NotificationDto;
import cl.duoc.vidasalud.bff.event.NotificationAuditEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServicePersistenceTest {

    private NotificationService notificationService;
    private EmbeddedDatabase database;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
        new ResourceDatabasePopulator(new ClassPathResource("test-schema.sql")).execute(database);
        notificationService = new NotificationService(new JdbcTemplate(database));
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void shouldFindInitialSeedNotifications() {
        List<NotificationDto> notifications = notificationService.findAll();
        assertThat(notifications).isNotEmpty();
        assertThat(notifications).anyMatch(n -> n.getId().equals("NOTIF-001"));
    }

    @Test
    void shouldSaveFromNotificationAuditEvent() {
        NotificationAuditEvent event = NotificationAuditEvent.builder()
                .eventId("EVT-TEST-1")
                .action("APPOINTMENT_SCHEDULED")
                .resource("AppointmentController")
                .user("Juan Pérez")
                .status("SUCCESS")
                .details("Nueva cita agendada APT-999 para Juan Pérez")
                .build();

        notificationService.saveFromEvent(event);

        List<NotificationDto> forPatient = notificationService.findByPatientName("Juan Pérez");
        assertThat(forPatient).isNotEmpty();
        NotificationDto saved = forPatient.get(0);
        assertThat(saved.getTitle()).isEqualTo("Notificación del Sistema");
        assertThat(saved.getAppointmentId()).isEqualTo("APT-999");
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void shouldMarkAsReadAndMarkAllAsRead() {
        notificationService.markAsRead("NOTIF-001");
        List<NotificationDto> all = notificationService.findAll();
        NotificationDto notif1 = all.stream().filter(n -> "NOTIF-001".equals(n.getId())).findFirst().orElseThrow();
        assertThat(notif1.isRead()).isTrue();

        notificationService.markAllAsRead();
        List<NotificationDto> updatedAll = notificationService.findAll();
        assertThat(updatedAll).allMatch(NotificationDto::isRead);
    }
}
