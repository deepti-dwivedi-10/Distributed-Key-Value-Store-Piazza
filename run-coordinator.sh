#!/bin/bash
# Run Coordination Server

IP="${1:-127.0.0.1}"
PORT="${2:-8080}"

echo "========================================="
echo "Starting Coordination Server"
echo "========================================="
echo "IP: $IP"
echo "Port: $PORT"
echo "========================================="
echo ""

java -jar target/coordinator.jar "$IP" "$PORT"
