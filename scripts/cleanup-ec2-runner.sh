#!/usr/bin/env bash
set -Eeuo pipefail

# Script to clean up EC2 runner disk space before deployment
# Run this on the self-hosted runner when disk is full

echo "=== EC2 Runner Cleanup ==="
echo ""

# Check current disk usage
echo "Current disk usage:"
df -h /

echo ""
echo "Cleaning up..."

# Remove old Maven cache
echo "Removing Maven repository cache..."
rm -rf ~/.m2/repository/* 2>/dev/null || true

# Remove dangling Docker images (safe)
echo "Removing dangling Docker images..."
docker image prune -f 2>/dev/null || true

# Remove old Docker builder cache
echo "Removing Docker builder cache..."
docker builder prune -f 2>/dev/null || true

# Remove old GitHub Actions logs
echo "Removing old Actions workflow logs..."
rm -rf /home/ec2-user/actions-runner/_diag/pages/* 2>/dev/null || true

echo ""
echo "After cleanup:"
df -h /

AVAILABLE=$(df / | awk 'NR==2 {print $4}')
if [ "$AVAILABLE" -gt 1000000 ]; then
  echo "✓ Cleanup successful! $(($AVAILABLE / 1024 / 1024)) GB available"
  exit 0
else
  echo "⚠ Warning: Only $(($AVAILABLE / 1024)) MB available. May need manual intervention."
  exit 1
fi
