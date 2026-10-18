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
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


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
    public DirectExchange directExchange(RabbitMQProperties properties) {
        return ExchangeBuilder.directExchange(properties.getExchanges().getDirect())
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange topicExchange(RabbitMQProperties properties) {
        return ExchangeBuilder.topicExchange(properties.getExchanges().getTopic())
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange dlxExchange(RabbitMQProperties properties) {
        return ExchangeBuilder.directExchange(properties.getExchanges().getDlx())
                .durable(true)
                .build();
    }

    // =========================================================================
    // COLAS PRINCIPALES (DURABLES CON DEAD LETTER EXCHANGE Y ROUTING KEY)
    // =========================================================================

    @Bean
    public Queue appointmentCreationQueue(RabbitMQProperties properties) {
        return QueueBuilder.durable(properties.getQueues().getAppointmentCreation())
                .withArgument(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, properties.getExchanges().getDlx())
                .withArgument(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, properties.getRoutingKeys().getAppointmentCreationDlq())
                .build();
    }

    @Bean
    public Queue appointmentConfirmationQueue(RabbitMQProperties properties) {
        return QueueBuilder.durable(properties.getQueues().getAppointmentConfirmation())
                .withArgument(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, properties.getExchanges().getDlx())
                .withArgument(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, properties.getRoutingKeys().getAppointmentConfirmationDlq())
                .build();
    }

    @Bean
    public Queue notificationAuditQueue(RabbitMQProperties properties) {
        return QueueBuilder.durable(properties.getQueues().getNotificationAudit())
                .withArgument(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, properties.getExchanges().getDlx())
                .withArgument(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, properties.getRoutingKeys().getNotificationAuditDlq())
                .build();
    }

    // =========================================================================
    // DEAD LETTER QUEUES (DLQ - DURABLES)
    // =========================================================================

    @Bean
    public Queue appointmentCreationDlq(RabbitMQProperties properties) {
        return QueueBuilder.durable(properties.getQueues().getAppointmentCreationDlq())
                .build();
    }

    @Bean
    public Queue appointmentConfirmationDlq(RabbitMQProperties properties) {
        return QueueBuilder.durable(properties.getQueues().getAppointmentConfirmationDlq())
                .build();
    }

    @Bean
    public Queue notificationAuditDlq(RabbitMQProperties properties) {
        return QueueBuilder.durable(properties.getQueues().getNotificationAuditDlq())
                .build();
    }

    // =========================================================================
    // BINDINGS PRINCIPALES
    // =========================================================================

    @Bean
    public Binding appointmentCreationBinding(Queue appointmentCreationQueue, DirectExchange directExchange, RabbitMQProperties properties) {
        return BindingBuilder.bind(appointmentCreationQueue)
                .to(directExchange)
                .with(properties.getRoutingKeys().getAppointmentCreate());
    }

    @Bean
    public Binding appointmentConfirmationBinding(Queue appointmentConfirmationQueue, DirectExchange directExchange, RabbitMQProperties properties) {
        return BindingBuilder.bind(appointmentConfirmationQueue)
                .to(directExchange)
                .with(properties.getRoutingKeys().getAppointmentConfirm());
    }

    @Bean
    public Binding notificationAuditBinding(Queue notificationAuditQueue, TopicExchange topicExchange, RabbitMQProperties properties) {
        return BindingBuilder.bind(notificationAuditQueue)
                .to(topicExchange)
                .with(properties.getRoutingKeys().getNotificationAudit());
    }

    // =========================================================================
    // BINDINGS DE DEAD LETTER QUEUES (DLQ -> DLX)
    // =========================================================================

    @Bean
    public Binding appointmentCreationDlqBinding(Queue appointmentCreationDlq, DirectExchange dlxExchange, RabbitMQProperties properties) {
        return BindingBuilder.bind(appointmentCreationDlq)
                .to(dlxExchange)
                .with(properties.getRoutingKeys().getAppointmentCreationDlq());
    }

    @Bean
    public Binding appointmentConfirmationDlqBinding(Queue appointmentConfirmationDlq, DirectExchange dlxExchange, RabbitMQProperties properties) {
        return BindingBuilder.bind(appointmentConfirmationDlq)
                .to(dlxExchange)
                .with(properties.getRoutingKeys().getAppointmentConfirmationDlq());
    }

    @Bean
    public Binding notificationAuditDlqBinding(Queue notificationAuditDlq, DirectExchange dlxExchange, RabbitMQProperties properties) {
        return BindingBuilder.bind(notificationAuditDlq)
                .to(dlxExchange)
                .with(properties.getRoutingKeys().getNotificationAuditDlq());
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
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            @Value("${spring.rabbitmq.listener.simple.prefetch:10}") int prefetch) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        // Aplica también la política retry definida en application.yml al factory personalizado.
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(prefetch);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
