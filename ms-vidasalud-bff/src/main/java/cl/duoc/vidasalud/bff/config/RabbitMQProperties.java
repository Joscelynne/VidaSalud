package cl.duoc.vidasalud.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** RabbitMQ topology names and routing keys bound from application.yml. */
@Component
@ConfigurationProperties(prefix = "vidasalud.rabbitmq")
public class RabbitMQProperties {
    private Exchanges exchanges = new Exchanges();
    private Queues queues = new Queues();
    private RoutingKeys routingKeys = new RoutingKeys();

    public Exchanges getExchanges() { return exchanges; }
    public void setExchanges(Exchanges exchanges) { this.exchanges = exchanges; }
    public Queues getQueues() { return queues; }
    public void setQueues(Queues queues) { this.queues = queues; }
    public RoutingKeys getRoutingKeys() { return routingKeys; }
    public void setRoutingKeys(RoutingKeys routingKeys) { this.routingKeys = routingKeys; }

    public static class Exchanges {
        private String direct;
        private String topic;
        private String dlx;
        public String getDirect() { return direct; }
        public void setDirect(String value) { direct = value; }
        public String getTopic() { return topic; }
        public void setTopic(String value) { topic = value; }
        public String getDlx() { return dlx; }
        public void setDlx(String value) { dlx = value; }
    }

    public static class Queues {
        private String appointmentCreation;
        private String appointmentConfirmation;
        private String notificationAudit;
        private String appointmentCreationDlq;
        private String appointmentConfirmationDlq;
        private String notificationAuditDlq;
        public String getAppointmentCreation() { return appointmentCreation; }
        public void setAppointmentCreation(String value) { appointmentCreation = value; }
        public String getAppointmentConfirmation() { return appointmentConfirmation; }
        public void setAppointmentConfirmation(String value) { appointmentConfirmation = value; }
        public String getNotificationAudit() { return notificationAudit; }
        public void setNotificationAudit(String value) { notificationAudit = value; }
        public String getAppointmentCreationDlq() { return appointmentCreationDlq; }
        public void setAppointmentCreationDlq(String value) { appointmentCreationDlq = value; }
        public String getAppointmentConfirmationDlq() { return appointmentConfirmationDlq; }
        public void setAppointmentConfirmationDlq(String value) { appointmentConfirmationDlq = value; }
        public String getNotificationAuditDlq() { return notificationAuditDlq; }
        public void setNotificationAuditDlq(String value) { notificationAuditDlq = value; }
    }

    public static class RoutingKeys {
        private String appointmentCreate;
        private String appointmentConfirm;
        private String notificationAudit;
        private String notificationAuditDefault;
        private String appointmentCreationDlq;
        private String appointmentConfirmationDlq;
        private String notificationAuditDlq;
        public String getAppointmentCreate() { return appointmentCreate; }
        public void setAppointmentCreate(String value) { appointmentCreate = value; }
        public String getAppointmentConfirm() { return appointmentConfirm; }
        public void setAppointmentConfirm(String value) { appointmentConfirm = value; }
        public String getNotificationAudit() { return notificationAudit; }
        public void setNotificationAudit(String value) { notificationAudit = value; }
        public String getNotificationAuditDefault() { return notificationAuditDefault; }
        public void setNotificationAuditDefault(String value) { notificationAuditDefault = value; }
        public String getAppointmentCreationDlq() { return appointmentCreationDlq; }
        public void setAppointmentCreationDlq(String value) { appointmentCreationDlq = value; }
        public String getAppointmentConfirmationDlq() { return appointmentConfirmationDlq; }
        public void setAppointmentConfirmationDlq(String value) { appointmentConfirmationDlq = value; }
        public String getNotificationAuditDlq() { return notificationAuditDlq; }
        public void setNotificationAuditDlq(String value) { notificationAuditDlq = value; }
    }
}
