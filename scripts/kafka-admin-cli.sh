#!/bin/bash
# ==============================================================================
# SCRIPT DE ADMINISTRACIÓN CLI PARA APACHE KAFKA (EP5 / EP6) — VIDASALUD
# ==============================================================================
# Permite interactuar con los brokers Kafka y ZooKeeper del clúster Docker Compose:
# - Listar, describir, crear, alterar y eliminar tópicos
# - Inspeccionar consumer groups, offsets y lag
# - Consumir y producir eventos de streaming en tiempo real
# ==============================================================================

set -e

CONTAINER_NAME="vidasalud-kafka-1"
BOOTSTRAP_SERVERS="kafka1:9092,kafka2:9093,kafka3:9094"

# Color formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

function check_container() {
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo -e "${RED}[ERROR] El contenedor ${CONTAINER_NAME} no está en ejecución.${NC}"
        echo -e "Asegúrate de haber iniciado el clúster con: ${CYAN}docker compose -f docker-compose.kafka.yml up -d${NC}"
        exit 1
    fi
}

function show_menu() {
    echo -e "${CYAN}==================================================================${NC}"
    echo -e "${CYAN}     VIDASALUD — GESTIÓN DE STREAMING APACHE KAFKA (EP5 / EP6)     ${NC}"
    echo -e "${CYAN}==================================================================${NC}"
    echo -e "1) Listar todos los tópicos"
    echo -e "2) Describir tópico ('vidasalud.atenciones.stream' o 'vidasalud.telemetria.stream')"
    echo -e "3) Crear un nuevo tópico (3 particiones, factor 3)"
    echo -e "4) Modificar retención de un tópico (retention.ms)"
    echo -e "5) Eliminar un tópico (solo no protegidos)"
    echo -e "6) Listar todos los Consumer Groups"
    echo -e "7) Describir Consumer Group (Offsets y Lag)"
    echo -e "8) Monitorear mensajes en tiempo real (Console Consumer)"
    echo -e "9) Publicar evento de prueba interactivo (Console Producer)"
    echo -e "10) Reiniciar offsets de un grupo detenido (inicio/final)"
    echo -e "0) Salir"
    echo -e "${CYAN}------------------------------------------------------------------${NC}"
    echo -n "Selecciona una opción [0-10]: "
}

function list_topics() {
    check_container
    echo -e "\n${BLUE}--> Listando tópicos en ${BOOTSTRAP_SERVERS}:${NC}"
    docker exec -t "${CONTAINER_NAME}" kafka-topics --bootstrap-server "${BOOTSTRAP_SERVERS}" --list
}

function describe_topic() {
    check_container
    echo -e "\n${YELLOW}Tópicos esenciales de VidaSalud:${NC}"
    echo -e "1. vidasalud.atenciones.stream"
    echo -e "2. vidasalud.telemetria.stream"
    echo -n "Ingresa el nombre del tópico [default: vidasalud.atenciones.stream]: "
    read -r TOPIC_NAME
    TOPIC_NAME=${TOPIC_NAME:-vidasalud.atenciones.stream}

    echo -e "\n${BLUE}--> Detalle técnico del tópico '${TOPIC_NAME}':${NC}"
    docker exec -t "${CONTAINER_NAME}" kafka-topics --bootstrap-server "${BOOTSTRAP_SERVERS}" --describe --topic "${TOPIC_NAME}"
}

function create_topic() {
    check_container
    echo -n "Nombre del nuevo tópico: "
    read -r TOPIC_NAME
    if [ -z "$TOPIC_NAME" ]; then
        echo -e "${RED}[ERROR] El nombre no puede estar vacío.${NC}"
        return
    fi
    echo -n "Número de particiones [default: 3]: "
    read -r PARTS
    PARTS=${PARTS:-3}
    echo -n "Factor de replicación [default: 3]: "
    read -r REPLS
    REPLS=${REPLS:-3}

    echo -e "\n${BLUE}--> Creando tópico '${TOPIC_NAME}' con ${PARTS} particiones y factor ${REPLS}:${NC}"
    docker exec -t "${CONTAINER_NAME}" kafka-topics --bootstrap-server "${BOOTSTRAP_SERVERS}" \
        --create --topic "${TOPIC_NAME}" --partitions "${PARTS}" --replication-factor "${REPLS}"
    echo -e "${GREEN}[OK] Tópico creado exitosamente.${NC}"
}

function alter_topic() {
    check_container
    echo -n "Nombre del tópico a modificar: "
    read -r TOPIC_NAME
    echo -n "Nueva retención en milisegundos (ej: 86400000 para 24h, 604800000 para 7d): "
    read -r RETENTION_MS

    if [ -z "$TOPIC_NAME" ] || [ -z "$RETENTION_MS" ]; then
        echo -e "${RED}[ERROR] Parámetros incompletos.${NC}"
        return
    fi

    echo -e "\n${BLUE}--> Modificando config 'retention.ms' a ${RETENTION_MS} para '${TOPIC_NAME}':${NC}"
    docker exec -t "${CONTAINER_NAME}" kafka-configs --bootstrap-server "${BOOTSTRAP_SERVERS}" \
        --entity-type topics --entity-name "${TOPIC_NAME}" --alter --add-config "retention.ms=${RETENTION_MS}"
    echo -e "${GREEN}[OK] Configuración alterada exitosamente.${NC}"
}

function delete_topic() {
    check_container
    echo -n "Nombre del tópico a eliminar: "
    read -r TOPIC_NAME

    if [ "$TOPIC_NAME" == "vidasalud.atenciones.stream" ] || [ "$TOPIC_NAME" == "vidasalud.telemetria.stream" ]; then
        echo -e "${RED}[PROTEGIDO] No se permite eliminar los tópicos esenciales del sistema VidaSalud.${NC}"
        return
    fi

    echo -e "\n${YELLOW}--> Eliminando tópico '${TOPIC_NAME}':${NC}"
    docker exec -t "${CONTAINER_NAME}" kafka-topics --bootstrap-server "${BOOTSTRAP_SERVERS}" --delete --topic "${TOPIC_NAME}"
    echo -e "${GREEN}[OK] Tópico eliminado.${NC}"
}

function list_groups() {
    check_container
    echo -e "\n${BLUE}--> Listando Consumer Groups activos:${NC}"
    docker exec -t "${CONTAINER_NAME}" kafka-consumer-groups --bootstrap-server "${BOOTSTRAP_SERVERS}" --list
}

function describe_group() {
    check_container
    echo -e "\n${YELLOW}Grupos de VidaSalud:${NC}"
    echo -e "1. vidasalud-analitica-group"
    echo -e "2. vidasalud-alertas-group"
    echo -n "Ingresa el nombre del Consumer Group [default: vidasalud-analitica-group]: "
    read -r GROUP_NAME
    GROUP_NAME=${GROUP_NAME:-vidasalud-analitica-group}

    echo -e "\n${BLUE}--> Offsets, asignación y Lag del grupo '${GROUP_NAME}':${NC}"
    docker exec -t "${CONTAINER_NAME}" kafka-consumer-groups --bootstrap-server "${BOOTSTRAP_SERVERS}" \
        --describe --group "${GROUP_NAME}"
}

function reset_group_offsets() {
    check_container
    echo -n "Consumer Group detenido: "
    read -r GROUP_NAME
    if [ -z "$GROUP_NAME" ]; then
        echo -e "${RED}[ERROR] El nombre del grupo es requerido.${NC}"
        return
    fi

    echo -n "Destino [earliest/latest]: "
    read -r RESET_TARGET
    case "$RESET_TARGET" in
        earliest|latest) ;;
        *) echo -e "${RED}[ERROR] El destino debe ser earliest o latest.${NC}"; return ;;
    esac

    echo -e "\n${YELLOW}Vista previa del cambio (no modifica offsets):${NC}"
    docker exec -t "${CONTAINER_NAME}" kafka-consumer-groups --bootstrap-server "${BOOTSTRAP_SERVERS}" \
        --group "${GROUP_NAME}" --reset-offsets "--to-${RESET_TARGET}" --dry-run
    echo -e "${RED}Esta acción puede reprocesar mensajes (earliest) u omitir pendientes (latest).${NC}"
    echo -n "Para confirmar, escribe exactamente RESET ${GROUP_NAME}: "
    read -r CONFIRMATION
    if [ "$CONFIRMATION" != "RESET ${GROUP_NAME}" ]; then
        echo -e "${YELLOW}Cancelado; no se modificaron offsets.${NC}"
        return
    fi

    docker exec -t "${CONTAINER_NAME}" kafka-consumer-groups --bootstrap-server "${BOOTSTRAP_SERVERS}" \
        --group "${GROUP_NAME}" --reset-offsets "--to-${RESET_TARGET}" --execute
    echo -e "${GREEN}[OK] Offsets reiniciados. Kafka rechazará la operación si el grupo sigue activo.${NC}"
}

function consume_stream() {
    check_container
    echo -n "Tópico a monitorear [default: vidasalud.atenciones.stream]: "
    read -r TOPIC_NAME
    TOPIC_NAME=${TOPIC_NAME:-vidasalud.atenciones.stream}

    echo -e "\n${BLUE}--> Conectando Console Consumer para '${TOPIC_NAME}' (Presiona Ctrl+C para salir):${NC}"
    docker exec -it "${CONTAINER_NAME}" kafka-console-consumer --bootstrap-server "${BOOTSTRAP_SERVERS}" \
        --topic "${TOPIC_NAME}" --from-beginning --property print.key=true --property print.partition=true
}

function produce_stream() {
    check_container
    echo -n "Tópico hacia el cual publicar [default: vidasalud.atenciones.stream]: "
    read -r TOPIC_NAME
    TOPIC_NAME=${TOPIC_NAME:-vidasalud.atenciones.stream}

    echo -e "\n${BLUE}--> Escribe mensajes en formato 'clave:valor' o texto plano (Ctrl+C para salir):${NC}"
    docker exec -it "${CONTAINER_NAME}" kafka-console-producer --bootstrap-server "${BOOTSTRAP_SERVERS}" \
        --topic "${TOPIC_NAME}" --property "parse.key=true" --property "key.separator=:"
}

# Bucle interactivo si no se pasan argumentos
if [ $# -eq 0 ]; then
    while true; do
        show_menu
        read -r OPTION
        case $OPTION in
            1) list_topics ;;
            2) describe_topic ;;
            3) create_topic ;;
            4) alter_topic ;;
            5) delete_topic ;;
            6) list_groups ;;
            7) describe_group ;;
            8) consume_stream ;;
            9) produce_stream ;;
            10) reset_group_offsets ;;
            0) echo -e "\nSaliendo."; exit 0 ;;
            *) echo -e "${RED}Opción no válida.${NC}" ;;
        esac
        echo ""
    done
else
    # Ejecución directa por argumento
    case "$1" in
        list) list_topics ;;
        describe) describe_topic ;;
        groups) list_groups ;;
        *) echo "Uso: $0 [list|describe|groups]" ; exit 1 ;;
    esac
fi
