# Administración de consumer groups en VidaSalud

## Inspeccionar grupos, offsets y lag

1. Inicia sesión en VidaSalud con un usuario de rol **Admin**.
2. Abre **Streaming Kafka** y pulsa **Refrescar**.
3. En **Consumer Groups, Offsets y Lag**, revisa el estado, cantidad de miembros, offsets por partición y lag.
4. Para una demostración de flujo, publica un evento de prueba o genera una atención desde VidaSalud y vuelve a refrescar. Comprueba que el offset avance y que el lag disminuya cuando el consumidor procese el evento.

## Reiniciar offsets desde la interfaz

1. Detén todas las instancias consumidoras que usen el grupo elegido. El botón solo se habilita en estado `EMPTY` o `DEAD`.
2. Elige **Inicio (reprocesar)** para volver a leer los mensajes retenidos, o **Final (omitir pendientes)** para marcar como leídos los mensajes existentes.
3. Pulsa **Reiniciar offsets** y confirma el cuadro de advertencia.
4. Actualiza los datos y verifica los offsets/lag. Al reanudar los consumidores, confirma el reprocesamiento o avance esperado.

El reinicio altera el punto de lectura del grupo. `EARLIEST` puede provocar reprocesamiento; `LATEST` puede saltarse mensajes pendientes. Kafka crea un grupo cuando arranca un consumidor configurado con ese `group.id`; no se crea como recurso independiente. Si el grupo pertenece a un consumidor que corre dentro del BFF, detener ese consumidor también interrumpe temporalmente el BFF; para esos grupos, realiza el reinicio con el script de consola y vuelve a iniciar el BFF.

## Reiniciar offsets desde consola

Ejecuta `scripts/kafka-admin-cli.sh` en el host que tenga Docker y el clúster Kafka. El menú incluye **10) Reiniciar offsets**. El script muestra primero una vista previa, pide escribir `RESET <nombre-del-grupo>` y solo entonces aplica el cambio. Kafka rechazará el reinicio si hay miembros activos en el grupo.

## API protegida

`POST /api/admin/kafka/consumer-groups/{groupId}/offsets/reset` (rol `Admin`):

```json
{
  "strategy": "EARLIEST",
  "confirmed": true
}
```

`strategy` acepta `EARLIEST` o `LATEST`. El backend valida que el grupo exista, esté detenido, tenga offsets comprometidos y que `confirmed` sea `true`.
