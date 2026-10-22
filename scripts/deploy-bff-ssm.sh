#!/usr/bin/env bash
set -Eeuo pipefail

: "${IMAGE_URI:?IMAGE_URI is required}"
: "${DEPLOYMENT_ID:?DEPLOYMENT_ID is required}"
: "${AWS_REGION:?AWS_REGION is required}"

CONTAINER_NAME="${BFF_CONTAINER_NAME:-vidasalud-bff}"
HEALTH_URL="${BFF_HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"
BACKUP_NAME="${CONTAINER_NAME}-rollback-${DEPLOYMENT_ID:0:12}"
WORK_DIR="$(mktemp -d /tmp/vidasalud-bff-deploy.XXXXXX)"
chmod 700 "$WORK_DIR"
ENV_FILE="$WORK_DIR/container.env"
PORTS_FILE="$WORK_DIR/ports.txt"
NETWORKS_FILE="$WORK_DIR/networks.txt"

cleanup() { rm -rf "$WORK_DIR"; }
trap cleanup EXIT

if ! docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
  echo "Container '$CONTAINER_NAME' was not found; refusing to start with guessed configuration."
  exit 1
fi
if [[ "$(docker inspect --format '{{.State.Running}}' "$CONTAINER_NAME")" != "true" ]]; then
  echo "Container '$CONTAINER_NAME' is not running; refusing deployment."
  exit 1
fi
if docker inspect "$BACKUP_NAME" >/dev/null 2>&1; then
  echo "Rollback container '$BACKUP_NAME' already exists; refusing to overwrite it."
  exit 1
fi

# Reuse the running container's environment, published ports, network, and restart policy.
# The environment file is private and is removed when this script exits.
docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$CONTAINER_NAME" > "$ENV_FILE"
chmod 600 "$ENV_FILE"
docker inspect --format '{{range $port, $bindings := .HostConfig.PortBindings}}{{range $bindings}}{{printf "%s|%s|%s\n" .HostIp .HostPort $port}}{{end}}{{end}}' "$CONTAINER_NAME" > "$PORTS_FILE"
docker inspect --format '{{range $name, $_ := .NetworkSettings.Networks}}{{println $name}}{{end}}' "$CONTAINER_NAME" > "$NETWORKS_FILE"
RESTART_POLICY="$(docker inspect --format '{{.HostConfig.RestartPolicy.Name}}' "$CONTAINER_NAME")"
if [[ -z "$RESTART_POLICY" || "$RESTART_POLICY" == "no" ]]; then
  RESTART_POLICY="unless-stopped"
fi

PRIMARY_NETWORK="$(head -n 1 "$NETWORKS_FILE")"
if [[ -z "$PRIMARY_NETWORK" ]]; then
  echo "The existing BFF container has no Docker network; refusing deployment."
  exit 1
fi

ECR_REGISTRY="${IMAGE_URI%%/*}"
rollback() {
  echo "Deployment failed; restoring the previous BFF container."
  if docker inspect "$BACKUP_NAME" >/dev/null 2>&1; then
    docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
    docker rename "$BACKUP_NAME" "$CONTAINER_NAME"
    docker start "$CONTAINER_NAME"
  elif docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
    docker start "$CONTAINER_NAME" >/dev/null 2>&1 || true
  fi
}

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"
docker pull "$IMAGE_URI"
trap 'rollback' ERR
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

docker run "${RUN_ARGS[@]}" "$IMAGE_URI"

for attempt in $(seq 1 36); do
  if curl --fail --silent --show-error "$HEALTH_URL" -o "$WORK_DIR/health.json" 2>/dev/null \
    && grep -q '"status"[[:space:]]*:[[:space:]]*"UP"' "$WORK_DIR/health.json"; then
    # Reattach secondary networks from the original container, if any.
    while IFS= read -r network; do
      [[ -n "$network" && "$network" != "$PRIMARY_NETWORK" ]] || continue
      docker network connect "$network" "$CONTAINER_NAME"
    done < "$NETWORKS_FILE"
    trap - ERR
    echo "Deployment healthy: image=$IMAGE_URI backup=$BACKUP_NAME"
    exit 0
  fi
  sleep 10
done

echo "New BFF container did not become healthy."
false
