package cl.duoc.vidasalud.bff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static cl.duoc.vidasalud.bff.config.RabbitMQConstants.*;

/**
 * Configuración integral de RabbitMQ para VidaSalud BFF.
 * Declara exchanges (direct, topic, dlx), 6 colas durables (3 principales con DLQ y 3 DLQ),
 * bindings, conversor JSON con Jackson, RabbitAdmin, RabbitTemplate y contenedor con ACK manual.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // =========================================================================
    // EXCHANGES
    // =========================================================================

    @Bean
    public DirectExchange directExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_DIRECT)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange topicExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_TOPIC)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_DLX)
                .durable(true)
                .build();
    }

    // =========================================================================
    // COLAS PRINCIPALES (DURABLES CON DEAD LETTER EXCHANGE Y ROUTING KEY)
    // =========================================================================

    @Bean
    public Queue appointmentCreationQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CREATION)
                .withArgument(X_DEAD_LETTER_EXCHANGE, EXCHANGE_DLX)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, RK_APPOINTMENT_CREATION_DLQ)
                .build();
    }

    @Bean
    public Queue appointmentConfirmationQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CONFIRMATION)
                .withArgument(X_DEAD_LETTER_EXCHANGE, EXCHANGE_DLX)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, RK_APPOINTMENT_CONFIRMATION_DLQ)
                .build();
    }

    @Bean
    public Queue notificationAuditQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_AUDIT)
                .withArgument(X_DEAD_LETTER_EXCHANGE, EXCHANGE_DLX)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, RK_NOTIFICATION_AUDIT_DLQ)
                .build();
    }

    // =========================================================================
    // DEAD LETTER QUEUES (DLQ - DURABLES)
    // =========================================================================

    @Bean
    public Queue appointmentCreationDlq() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CREATION_DLQ)
                .build();
    }

    @Bean
    public Queue appointmentConfirmationDlq() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CONFIRMATION_DLQ)
                .build();
    }

    @Bean
    public Queue notificationAuditDlq() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_AUDIT_DLQ)
                .build();
    }

    // =========================================================================
    // BINDINGS PRINCIPALES
    // =========================================================================

    @Bean
    public Binding appointmentCreationBinding(Queue appointmentCreationQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(appointmentCreationQueue)
                .to(directExchange)
                .with(RK_APPOINTMENT_CREATE);
    }

    @Bean
    public Binding appointmentConfirmationBinding(Queue appointmentConfirmationQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(appointmentConfirmationQueue)
                .to(directExchange)
                .with(RK_APPOINTMENT_CONFIRM);
    }

    @Bean
    public Binding notificationAuditBinding(Queue notificationAuditQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(notificationAuditQueue)
                .to(topicExchange)
                .with(RK_NOTIFICATION_TOPIC_PATTERN);
    }

    // =========================================================================
    // BINDINGS DE DEAD LETTER QUEUES (DLQ -> DLX)
    // =========================================================================

    @Bean
    public Binding appointmentCreationDlqBinding(Queue appointmentCreationDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(appointmentCreationDlq)
                .to(dlxExchange)
                .with(RK_APPOINTMENT_CREATION_DLQ);
    }

    @Bean
    public Binding appointmentConfirmationDlqBinding(Queue appointmentConfirmationDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(appointmentConfirmationDlq)
                .to(dlxExchange)
                .with(RK_APPOINTMENT_CONFIRMATION_DLQ);
    }

    @Bean
    public Binding notificationAuditDlqBinding(Queue notificationAuditDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(notificationAuditDlq)
                .to(dlxExchange)
                .with(RK_NOTIFICATION_AUDIT_DLQ);
    }

    // =========================================================================
    // INFRAESTRUCTURA AMQP (SERIALIZADOR, ADMIN, TEMPLATE Y LISTENER CONTAINER)
    // =========================================================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            @Value("${spring.rabbitmq.listener.simple.prefetch:10}") int prefetch) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(prefetch);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
