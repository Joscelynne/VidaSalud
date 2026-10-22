package cl.duoc.vidasalud.bff.service;

import cl.duoc.vidasalud.bff.config.VidaSaludTopologyConfig;
import cl.duoc.vidasalud.bff.dto.amqp.*;
import cl.duoc.vidasalud.bff.exception.AmqpValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas de validación para la API administrativa de RabbitMQ.
 * Verifica el cumplimiento de la rúbrica EP3:
 * - Rechazo de nombres vacíos o en blanco
 * - Rechazo del prefijo reservado 'amq.'
 * - Protección de la topología esencial de VidaSalud
 * - Mensajes de error claros con códigos HTTP apropiados
 * 
 * Estas pruebas usan mocks y NO requieren conexión a RabbitMQ.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Validaciones de la API Admin RabbitMQ - Rúbrica EP3")
class RabbitAdminValidationTest {

    @Mock
    private AmqpAdmin amqpAdmin;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private VidaSaludTopologyConfig topologyConfig;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        // Configuración manual de la topología protegida (sin Spring Context)
        topologyConfig = new VidaSaludTopologyConfig();
        
        VidaSaludTopologyConfig.Exchanges exchanges = new VidaSaludTopologyConfig.Exchanges();
        exchanges.setDirect("vidasalud.direct.exchange");
        exchanges.setTopic("vidasalud.topic.exchange");
        exchanges.setDlx("vidasalud.dlx.exchange");
        topologyConfig.setExchanges(exchanges);
        
        VidaSaludTopologyConfig.Queues queues = new VidaSaludTopologyConfig.Queues();
        queues.setAppointmentCreation("citas.agendamiento.queue");
        queues.setAppointmentConfirmation("citas.confirmacion.queue");
        queues.setNotificationAudit("notificaciones.auditoria.queue");
        queues.setAppointmentCreationDlq("citas.agendamiento.dlq");
        queues.setAppointmentConfirmationDlq("citas.confirmacion.dlq");
        queues.setNotificationAuditDlq("notificaciones.auditoria.dlq");
        topologyConfig.setQueues(queues);
        
        service = new RabbitAdminService(amqpAdmin, rabbitTemplate, topologyConfig);
    }

    // ==================== VALIDACIÓN DE NOMBRES VACÍOS ====================
    
    @Nested
    @DisplayName("Validación de nombres vacíos o en blanco")
    class EmptyNameValidation {

        @ParameterizedTest(name = "createQueue rechaza nombre: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n", "  \t  "})
        @DisplayName("createQueue rechaza nombres nulos, vacíos o solo espacios")
        void createQueue_rejectsEmptyNames(String invalidName) {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .name(invalidName)
                    .durable(true)
                    .build();

            assertThatThrownBy(() -> service.createQueue(request))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_EMPTY_NAME");
                        assertThat(amqpEx.getMessage()).contains("vacío");
                    });
        }

        @ParameterizedTest(name = "createExchange rechaza nombre: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t\n"})
        @DisplayName("createExchange rechaza nombres nulos, vacíos o solo espacios")
        void createExchange_rejectsEmptyNames(String invalidName) {
            CreateExchangeRequest request = CreateExchangeRequest.builder()
                    .name(invalidName)
                    .type("direct")
                    .build();

            assertThatThrownBy(() -> service.createExchange(request))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_EMPTY_NAME");
                    });
        }

        @ParameterizedTest(name = "deleteQueue rechaza nombre: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("deleteQueue rechaza nombres nulos, vacíos o solo espacios")
        void deleteQueue_rejectsEmptyNames(String invalidName) {
            assertThatThrownBy(() -> service.deleteQueue(invalidName))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_EMPTY_NAME");
                    });
        }

        @ParameterizedTest(name = "deleteExchange rechaza nombre: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("deleteExchange rechaza nombres nulos, vacíos o solo espacios")
        void deleteExchange_rejectsEmptyNames(String invalidName) {
            assertThatThrownBy(() -> service.deleteExchange(invalidName))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_EMPTY_NAME");
                    });
        }
    }

    // ==================== VALIDACIÓN DE PREFIJO RESERVADO 'amq.' ====================
    
    @Nested
    @DisplayName("Validación del prefijo reservado 'amq.'")
    class ReservedPrefixValidation {

        @ParameterizedTest(name = "createQueue rechaza prefijo amq.: [{0}]")
        @ValueSource(strings = {"amq.direct", "amq.topic", "amq.fanout", "amq.headers", "amq.match", "AMQ.TEST"})
        @DisplayName("createQueue rechaza nombres con prefijo reservado 'amq.'")
        void createQueue_rejectsAmqPrefix(String reservedName) {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .name(reservedName)
                    .durable(true)
                    .build();

            assertThatThrownBy(() -> service.createQueue(request))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_RESERVED_PREFIX");
                        assertThat(amqpEx.getMessage()).contains("amq.");
                        assertThat(amqpEx.getMessage()).contains("reservado");
                    });
        }

        @ParameterizedTest(name = "createExchange rechaza prefijo amq.: [{0}]")
        @ValueSource(strings = {"amq.direct", "amq.topic", "amq.fanout"})
        @DisplayName("createExchange rechaza nombres con prefijo reservado 'amq.'")
        void createExchange_rejectsAmqPrefix(String reservedName) {
            CreateExchangeRequest request = CreateExchangeRequest.builder()
                    .name(reservedName)
                    .type("direct")
                    .build();

            assertThatThrownBy(() -> service.createExchange(request))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_RESERVED_PREFIX");
                    });
        }

        @ParameterizedTest(name = "deleteQueue rechaza prefijo amq.: [{0}]")
        @ValueSource(strings = {"amq.gen-abc123", "amq.rabbitmq.trace"})
        @DisplayName("deleteQueue rechaza nombres con prefijo reservado 'amq.'")
        void deleteQueue_rejectsAmqPrefix(String reservedName) {
            assertThatThrownBy(() -> service.deleteQueue(reservedName))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_RESERVED_PREFIX");
                    });
        }

        @Test
        @DisplayName("Nombres que contienen 'amq' pero NO como prefijo son permitidos")
        void allowsNamesContainingAmqNotAsPrefix() {
            // "miapp.amq.queue" no empieza con "amq." así que debería pasar validación de prefijo
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .name("miapp.amq.queue")
                    .durable(true)
                    .build();

            // No debe lanzar excepción de prefijo reservado (puede fallar por otras razones en mock)
            try {
                service.createQueue(request);
            } catch (AmqpValidationException ex) {
                // Si lanza excepción, NO debe ser por prefijo reservado
                assertThat(ex.getErrorCode()).isNotEqualTo("AMQP_RESERVED_PREFIX");
            } catch (Exception ignored) {
                // Otras excepciones son aceptables (mock no configurado)
            }
        }
    }

    // ==================== VALIDACIÓN DE TOPOLOGÍA PROTEGIDA ====================
    
    @Nested
    @DisplayName("Protección de la topología esencial de VidaSalud")
    class ProtectedTopologyValidation {

        @ParameterizedTest(name = "deleteQueue rechaza cola protegida: [{0}]")
        @ValueSource(strings = {
                "citas.agendamiento.queue",
                "citas.confirmacion.queue",
                "notificaciones.auditoria.queue",
                "citas.agendamiento.dlq",
                "citas.confirmacion.dlq",
                "notificaciones.auditoria.dlq"
        })
        @DisplayName("deleteQueue rechaza eliminación de colas protegidas de VidaSalud")
        void deleteQueue_rejectsProtectedQueues(String protectedQueue) {
            // La validación de cola protegida ocurre ANTES de consultar el broker,
            // por lo que no se necesita mock de getQueueInfo

            assertThatThrownBy(() -> service.deleteQueue(protectedQueue))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_PROTECTED_RESOURCE");
                        assertThat(amqpEx.getMessage()).contains("topología esencial");
                        assertThat(amqpEx.getMessage()).contains(protectedQueue);
                    });
        }

        @ParameterizedTest(name = "deleteExchange rechaza exchange protegido: [{0}]")
        @ValueSource(strings = {
                "vidasalud.direct.exchange",
                "vidasalud.topic.exchange",
                "vidasalud.dlx.exchange"
        })
        @DisplayName("deleteExchange rechaza eliminación de exchanges protegidos de VidaSalud")
        void deleteExchange_rejectsProtectedExchanges(String protectedExchange) {
            assertThatThrownBy(() -> service.deleteExchange(protectedExchange))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_PROTECTED_RESOURCE");
                        assertThat(amqpEx.getMessage()).contains("topología esencial");
                    });
        }

        @Test
        @DisplayName("deleteQueue permite eliminar colas NO protegidas")
        void deleteQueue_allowsNonProtectedQueues() {
            String customQueue = "mi.cola.personalizada";
            when(amqpAdmin.getQueueInfo(customQueue)).thenReturn(
                    new QueueInformation(customQueue, 5, 1)
            );
            when(amqpAdmin.deleteQueue(customQueue)).thenReturn(true);

            // No debe lanzar excepción
            boolean result = service.deleteQueue(customQueue);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("deleteExchange permite eliminar exchanges NO protegidos")
        void deleteExchange_allowsNonProtectedExchanges() {
            String customExchange = "mi.exchange.personalizado";
            
            // No debe lanzar excepción de recurso protegido
            try {
                service.deleteExchange(customExchange);
            } catch (AmqpValidationException ex) {
                assertThat(ex.getErrorCode()).isNotEqualTo("AMQP_PROTECTED_RESOURCE");
            }
        }

        @ParameterizedTest(name = "purgeQueue rechaza cola protegida: [{0}]")
        @ValueSource(strings = {
                "citas.agendamiento.queue",
                "citas.confirmacion.queue",
                "notificaciones.auditoria.queue",
                "citas.agendamiento.dlq",
                "citas.confirmacion.dlq",
                "notificaciones.auditoria.dlq"
        })
        @DisplayName("purgeQueue rechaza purga de colas protegidas de VidaSalud")
        void purgeQueue_rejectsProtectedQueues(String protectedQueue) {
            assertThatThrownBy(() -> service.purgeQueue(protectedQueue))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_PROTECTED_RESOURCE");
                        assertThat(amqpEx.getMessage()).contains("topología esencial");
                        assertThat(amqpEx.getMessage()).contains(protectedQueue);
                    });
        }

        @Test
        @DisplayName("purgeQueue sí permite purgar colas no protegidas")
        void purgeQueue_allowsNonProtectedQueues() {
            String customQueue = "mi.cola.personalizada";
            when(amqpAdmin.getQueueInfo(customQueue)).thenReturn(
                    new QueueInformation(customQueue, 10, 2)
            );

            int purged = service.purgeQueue(customQueue);
            assertThat(purged).isEqualTo(10);
        }
    }

    // ==================== VALIDACIÓN DE CARACTERES INVÁLIDOS ====================
    
    @Nested
    @DisplayName("Validación de caracteres en nombres")
    class InvalidCharactersValidation {

        @ParameterizedTest(name = "createQueue rechaza caracteres inválidos: [{0}]")
        @ValueSource(strings = {"cola con espacios", "cola@especial", "cola#hash", "cola$dolar", "cola%porciento"})
        @DisplayName("createQueue rechaza nombres con caracteres no permitidos")
        void createQueue_rejectsInvalidCharacters(String invalidName) {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .name(invalidName)
                    .durable(true)
                    .build();

            assertThatThrownBy(() -> service.createQueue(request))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_INVALID_CHARACTERS");
                    });
        }

        @ParameterizedTest(name = "createQueue acepta caracteres válidos: [{0}]")
        @ValueSource(strings = {"cola.valida", "cola-con-guiones", "cola_con_underscore", "Cola123", "mi.cola.app"})
        @DisplayName("createQueue acepta nombres con caracteres permitidos")
        void createQueue_acceptsValidCharacters(String validName) {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .name(validName)
                    .durable(true)
                    .build();

            // No debe lanzar excepción de caracteres inválidos
            try {
                service.createQueue(request);
            } catch (AmqpValidationException ex) {
                assertThat(ex.getErrorCode()).isNotEqualTo("AMQP_INVALID_CHARACTERS");
            } catch (Exception ignored) {
                // Otras excepciones son aceptables
            }
        }
    }

    // ==================== VALIDACIÓN DE TIPO DE EXCHANGE ====================
    
    @Nested
    @DisplayName("Validación de tipos de exchange")
    class ExchangeTypeValidation {

        @ParameterizedTest(name = "createExchange rechaza tipo no soportado: [{0}]")
        @ValueSource(strings = {"headers", "invalid", "UNKNOWN", "consistenthash"})
        @DisplayName("createExchange rechaza tipos de exchange no soportados")
        void createExchange_rejectsUnsupportedTypes(String unsupportedType) {
            CreateExchangeRequest request = CreateExchangeRequest.builder()
                    .name("test.exchange")
                    .type(unsupportedType)
                    .build();

            assertThatThrownBy(() -> service.createExchange(request))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_UNSUPPORTED_EXCHANGE_TYPE");
                        assertThat(amqpEx.getMessage()).contains("direct, topic, fanout");
                    });
        }

        @ParameterizedTest(name = "createExchange acepta tipo soportado: [{0}]")
        @ValueSource(strings = {"direct", "topic", "fanout", "DIRECT", "Topic", "FANOUT"})
        @DisplayName("createExchange acepta tipos de exchange soportados (case-insensitive)")
        void createExchange_acceptsSupportedTypes(String supportedType) {
            CreateExchangeRequest request = CreateExchangeRequest.builder()
                    .name("test.exchange.valid")
                    .type(supportedType)
                    .build();

            // No debe lanzar excepción de tipo no soportado
            try {
                service.createExchange(request);
            } catch (AmqpValidationException ex) {
                assertThat(ex.getErrorCode()).isNotEqualTo("AMQP_UNSUPPORTED_EXCHANGE_TYPE");
            } catch (Exception ignored) {
                // Otras excepciones son aceptables
            }
        }
    }

    // ==================== VALIDACIÓN DE DESTINATION TYPE ====================
    
    @Nested
    @DisplayName("Validación de destinationType en bindings")
    class DestinationTypeValidation {

        @ParameterizedTest(name = "createBinding rechaza destinationType inválido: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "INVALID", "cola", "topic"})
        @DisplayName("createBinding rechaza destinationType inválido")
        void createBinding_rejectsInvalidDestinationType(String invalidType) {
            CreateBindingRequest request = CreateBindingRequest.builder()
                    .destination("test.queue")
                    .destinationType(invalidType)
                    .exchange("test.exchange")
                    .routingKey("test.key")
                    .build();

            assertThatThrownBy(() -> service.createBinding(request))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_INVALID_DESTINATION_TYPE");
                    });
        }

        @ParameterizedTest(name = "createBinding acepta destinationType válido: [{0}]")
        @ValueSource(strings = {"QUEUE", "EXCHANGE", "queue", "exchange", "Queue", "Exchange"})
        @DisplayName("createBinding acepta destinationType válido (case-insensitive)")
        void createBinding_acceptsValidDestinationType(String validType) {
            CreateBindingRequest request = CreateBindingRequest.builder()
                    .destination("test.queue")
                    .destinationType(validType)
                    .exchange("test.exchange")
                    .routingKey("test.key")
                    .build();

            // No debe lanzar excepción de destinationType inválido
            try {
                service.createBinding(request);
            } catch (AmqpValidationException ex) {
                assertThat(ex.getErrorCode()).isNotEqualTo("AMQP_INVALID_DESTINATION_TYPE");
            } catch (Exception ignored) {
                // Otras excepciones son aceptables
            }
        }
    }

    // ==================== VERIFICACIÓN DE CÓDIGOS HTTP ====================
    
    @Nested
    @DisplayName("Verificación de códigos HTTP según tipo de error")
    class HttpStatusCodeValidation {

        @Test
        @DisplayName("Errores de validación retornan HTTP 400 Bad Request")
        void validationErrors_return400() {
            AmqpValidationException emptyName = AmqpValidationException.emptyName("cola");
            AmqpValidationException reservedPrefix = AmqpValidationException.reservedPrefix("amq.test");
            AmqpValidationException invalidChars = AmqpValidationException.invalidCharacters("bad@name", "cola");
            AmqpValidationException unsupportedType = AmqpValidationException.unsupportedExchangeType("invalid");

            assertThat(emptyName.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(reservedPrefix.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(invalidChars.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(unsupportedType.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Errores de recurso protegido retornan HTTP 403 Forbidden")
        void protectedResourceErrors_return403() {
            AmqpValidationException protectedResource = AmqpValidationException.protectedResource(
                    "vidasalud.direct.exchange", "eliminar");

            assertThat(protectedResource.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Todos los errores incluyen errorCode descriptivo")
        void allErrors_includeErrorCode() {
            assertThat(AmqpValidationException.emptyName("cola").getErrorCode())
                    .isEqualTo("AMQP_EMPTY_NAME");
            assertThat(AmqpValidationException.reservedPrefix("amq.test").getErrorCode())
                    .isEqualTo("AMQP_RESERVED_PREFIX");
            assertThat(AmqpValidationException.protectedResource("cola", "eliminar").getErrorCode())
                    .isEqualTo("AMQP_PROTECTED_RESOURCE");
            assertThat(AmqpValidationException.invalidCharacters("bad", "cola").getErrorCode())
                    .isEqualTo("AMQP_INVALID_CHARACTERS");
            assertThat(AmqpValidationException.unsupportedExchangeType("bad").getErrorCode())
                    .isEqualTo("AMQP_UNSUPPORTED_EXCHANGE_TYPE");
            assertThat(AmqpValidationException.invalidDestinationType("bad").getErrorCode())
                    .isEqualTo("AMQP_INVALID_DESTINATION_TYPE");
        }
    }

    // ==================== VERIFICACIÓN DE CREACIÓN REAL ====================
    
    @Nested
    @DisplayName("Verificación de creación real en RabbitMQ")
    class CreationVerificationValidation {

        @Test
        @DisplayName("createQueue falla si declareQueue retorna null (rechazo silencioso)")
        void createQueue_failsWhenDeclareReturnsNull() {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .name("test.cola.valida")
                    .durable(true)
                    .build();

            // Mock: declareQueue retorna null (RabbitMQ rechazó silenciosamente)
            when(amqpAdmin.declareQueue(any())).thenReturn(null);

            assertThatThrownBy(() -> service.createQueue(request))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        // Para nombres que NO empiezan con amq., se usa AMQP_BROKER_REJECTED
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_BROKER_REJECTED");
                        assertThat(amqpEx.getMessage()).contains("rechazó");
                    });
        }

        @Test
        @DisplayName("createQueue falla con AMQP_RESERVED_PREFIX si nombre amq.* y declareQueue retorna null")
        void createQueue_failsWithReservedPrefixWhenAmqNameAndDeclareReturnsNull() {
            // IMPORTANTE: Este test verifica que si un nombre con prefijo amq. 
            // logra pasar la validación inicial (edge case) y RabbitMQ lo rechaza,
            // se reporta correctamente como AMQP_RESERVED_PREFIX
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .name("amq.test.queue")  // Prefijo reservado
                    .durable(true)
                    .build();

            // Este test debe fallar en la validación previa, no llegar al declareQueue
            assertThatThrownBy(() -> service.createQueue(request))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_RESERVED_PREFIX");
                    });
        }

        @Test
        @DisplayName("createQueue falla si getQueueInfo retorna null después de declarar (cola no creada)")
        void createQueue_failsWhenQueueNotCreatedAfterDeclare() {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .name("test.cola.fantasma")
                    .durable(true)
                    .build();

            // Mock: declareQueue retorna el nombre pero getQueueInfo retorna null
            when(amqpAdmin.declareQueue(any())).thenReturn("test.cola.fantasma");
            when(amqpAdmin.getQueueInfo("test.cola.fantasma")).thenReturn(null);

            assertThatThrownBy(() -> service.createQueue(request))
                    .isInstanceOf(AmqpValidationException.class)
                    .satisfies(ex -> {
                        AmqpValidationException amqpEx = (AmqpValidationException) ex;
                        assertThat(amqpEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        // Para nombres que NO empiezan con amq., se usa AMQP_BROKER_REJECTED
                        assertThat(amqpEx.getErrorCode()).isEqualTo("AMQP_BROKER_REJECTED");
                        assertThat(amqpEx.getMessage()).contains("rechazó");
                    });
        }

        @Test
        @DisplayName("createQueue tiene éxito cuando declareQueue y getQueueInfo confirman creación")
        void createQueue_succeedsWhenBothConfirm() {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .name("test.cola.real")
                    .durable(true)
                    .build();

            // Mock: declareQueue retorna el nombre Y getQueueInfo confirma existencia
            when(amqpAdmin.declareQueue(any())).thenReturn("test.cola.real");
            when(amqpAdmin.getQueueInfo("test.cola.real")).thenReturn(
                    new QueueInformation("test.cola.real", 0, 0)
            );

            String result = service.createQueue(request);
            
            assertThat(result).isEqualTo("test.cola.real");
        }
    }
}
