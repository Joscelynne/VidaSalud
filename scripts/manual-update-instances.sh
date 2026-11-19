#!/usr/bin/env bash
set -Eeuo pipefail

# Manual deployment script to update BFF and Kafka instances
# Usage: bash scripts/manual-update-instances.sh

REPO_URL="https://github.com/Joscelynne/VidaSalud.git"
BFF_EC2="172.31.89.28"
KAFKA_EC2="172.31.67.180"

echo "=== VidaSalud Manual Instance Update ==="
echo ""

# Function to run command on remote EC2
run_on_ec2() {
  local instance=$1
  local user=$2
  local cmd=$3
  
  echo "Running on $instance: $cmd"
  ssh -o StrictHostKeyChecking=no -i ~/vockey "$user@$instance" "$cmd"
}

# Detect if running from GitHub Actions or locally
if [ -n "${GITHUB_WORKSPACE:-}" ]; then
  echo "Running in GitHub Actions"
  WORKSPACE="$GITHUB_WORKSPACE"
else
  echo "Running locally"
  WORKSPACE="$(pwd)"
fi

echo "Workspace: $WORKSPACE"
echo ""

# Update BFF
echo "=== Updating BFF Instance ($BFF_EC2) ==="
echo ""

echo "1. Cleaning up disk space..."
run_on_ec2 "$BFF_EC2" "ubuntu" "rm -rf ~/.m2/repository/* && docker system prune -a -f --volumes || true"

echo ""
echo "2. Pulling latest code..."
run_on_ec2 "$BFF_EC2" "ubuntu" "cd /home/ubuntu/VidaSalud 2>/dev/null || (git clone $REPO_URL /home/ubuntu/VidaSalud && cd /home/ubuntu/VidaSalud); git fetch origin && git checkout main && git pull origin main"

echo ""
echo "3. Verifying deployment..."
run_on_ec2 "$BFF_EC2" "ubuntu" "curl -s http://127.0.0.1:8080/actuator/health | grep -q UP && echo '✓ BFF is UP' || echo '✗ BFF is DOWN'"

echo ""
echo "=== Updating Kafka Instance ($KAFKA_EC2) ==="
echo ""

echo "1. Pulling latest code..."
run_on_ec2 "$KAFKA_EC2" "ubuntu" "cd /home/ubuntu/VidaSalud 2>/dev/null || (git clone $REPO_URL /home/ubuntu/VidaSalud && cd /home/ubuntu/VidaSalud); git fetch origin && git checkout main && git pull origin main"

echo ""
echo "2. Restarting Kafka stack..."
run_on_ec2 "$KAFKA_EC2" "ubuntu" "cd /home/ubuntu/VidaSalud && docker-compose -f docker-compose.kafka.yml down && docker-compose -f docker-compose.kafka.yml up -d"

echo ""
echo "3. Verifying Kafka..."
run_on_ec2 "$KAFKA_EC2" "ubuntu" "docker-compose -f docker-compose.kafka.yml ps"

echo ""
echo "=== Verification Complete ==="
echo ""
echo "Check topics on Kafka:"
echo "ssh -i ~/vockey ubuntu@$KAFKA_EC2 'docker-compose -f docker-compose.kafka.yml exec kafka1 /usr/bin/kafka-topics --bootstrap-server localhost:9092 --list'"
echo ""
echo "Check BFF logs:"
echo "ssh -i ~/vockey ubuntu@$BFF_EC2 'docker logs vidasalud-bff | tail -50'"
