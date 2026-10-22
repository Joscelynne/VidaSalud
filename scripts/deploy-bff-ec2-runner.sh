#!/usr/bin/env bash
set -Eeuo pipefail

: "${GITHUB_SHA:?GITHUB_SHA is required}"
CONTAINER_NAME="${BFF_CONTAINER_NAME:-vidasalud-bff}"
HEALTH_URL="${BFF_HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"
IMAGE_TAG="vidasalud-bff:${GITHUB_SHA}"
BACKUP_NAME="${CONTAINER_NAME}-rollback-${GITHUB_SHA:0:12}"
WORK_DIR="$(mktemp -d /tmp/vidasalud-bff-deploy.XXXXXX)"
chmod 700 "$WORK_DIR"
ENV_FILE="$WORK_DIR/container.env"
PORTS_FILE="$WORK_DIR/ports.txt"
NETWORKS_FILE="$WORK_DIR/networks.txt"
DEPLOYMENT_STARTED=false

cleanup() { rm -rf "$WORK_DIR"; }
trap cleanup EXIT

command -v docker >/dev/null || { echo "Docker is required on the EC2 runner." >&2; exit 1; }
command -v curl >/dev/null || { echo "curl is required on the EC2 runner." >&2; exit 1; }
docker info >/dev/null

if ! docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
  echo "Container '$CONTAINER_NAME' was not found; refusing to guess its configuration." >&2
  exit 1
fi
if [[ "$(docker inspect --format '{{.State.Running}}' "$CONTAINER_NAME")" != "true" ]]; then
  echo "Container '$CONTAINER_NAME' is not running; refusing deployment." >&2
  exit 1
fi
if docker inspect "$BACKUP_NAME" >/dev/null 2>&1; then
  echo "Rollback container '$BACKUP_NAME' already exists; refusing to overwrite it." >&2
  exit 1
fi

# Build before touching the running container, so a build failure has no impact.
docker build --pull --platform linux/amd64 -t "$IMAGE_TAG" -f ms-vidasalud-bff/Dockerfile ms-vidasalud-bff

# Preserve the current runtime configuration in private temporary files.
docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$CONTAINER_NAME" > "$ENV_FILE"
chmod 600 "$ENV_FILE"
docker inspect --format '{{range $port, $bindings := .HostConfig.PortBindings}}{{range $bindings}}{{printf "%s|%s|%s\n" .HostIp .HostPort $port}}{{end}}{{end}}' "$CONTAINER_NAME" > "$PORTS_FILE"
docker inspect --format '{{range $name, $_ := .NetworkSettings.Networks}}{{println $name}}{{end}}' "$CONTAINER_NAME" > "$NETWORKS_FILE"
NETWORK_MODE="$(docker inspect --format '{{.HostConfig.NetworkMode}}' "$CONTAINER_NAME")"
RESTART_POLICY="$(docker inspect --format '{{.HostConfig.RestartPolicy.Name}}' "$CONTAINER_NAME")"
if [[ -z "$RESTART_POLICY" || "$RESTART_POLICY" == "no" ]]; then
  RESTART_POLICY="unless-stopped"
fi

PRIMARY_NETWORK="$NETWORK_MODE"
if ! grep -Fxq "$PRIMARY_NETWORK" "$NETWORKS_FILE"; then
  PRIMARY_NETWORK="$(head -n 1 "$NETWORKS_FILE")"
fi
if [[ -z "$PRIMARY_NETWORK" ]]; then
  echo "The existing BFF container has no Docker network; refusing deployment." >&2
  exit 1
fi

rollback() {
  local exit_code=$?
  trap - ERR
  if [[ "$DEPLOYMENT_STARTED" == true ]]; then
    echo "Deployment failed; restoring the previous BFF container." >&2
    if docker inspect "$BACKUP_NAME" >/dev/null 2>&1; then
      docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
      docker rename "$BACKUP_NAME" "$CONTAINER_NAME"
      docker start "$CONTAINER_NAME" >/dev/null
    elif docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
      docker start "$CONTAINER_NAME" >/dev/null 2>&1 || true
    fi
  fi
  exit "$exit_code"
}
trap rollback ERR

DEPLOYMENT_STARTED=true
docker stop "$CONTAINER_NAME"
docker rename "$CONTAINER_NAME" "$BACKUP_NAME"
RUN_ARGS=(--detach --name "$CONTAINER_NAME" --restart "$RESTART_POLICY" --network "$PRIMARY_NETWORK" --env-file "$ENV_FILE")
while IFS='|' read -r host_ip host_port container_port; do
  [[ -n "$container_port" ]] || continue
  container_port="${container_port%/*}"
  if [[ -n "$host_ip" ]]; then
    RUN_ARGS+=(--publish "${host_ip}:${host_port}:${container_port}")
  else
    RUN_ARGS+=(--publish "${host_port}:${container_port}")
  fi
done < "$PORTS_FILE"

docker run "${RUN_ARGS[@]}" "$IMAGE_TAG"

# Reconnect any secondary networks before checking health.
while IFS= read -r network; do
  [[ -n "$network" && "$network" != "$PRIMARY_NETWORK" ]] || continue
  docker network connect "$network" "$CONTAINER_NAME"
done < "$NETWORKS_FILE"

for attempt in $(seq 1 36); do
  if curl --fail --silent "$HEALTH_URL" -o "$WORK_DIR/health.json" 2>/dev/null \
    && grep -q '"status"[[:space:]]*:[[:space:]]*"UP"' "$WORK_DIR/health.json"; then
    trap - ERR
    echo "Deployment healthy: commit=${GITHUB_SHA}; rollback container=${BACKUP_NAME}"
    exit 0
  fi
  sleep 10
done

echo "New BFF container did not become healthy." >&2
false
