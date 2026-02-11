#!/bin/bash
# Run Slave Server

IP="${1:-127.0.0.1}"
PORT="${2:-8081}"

echo "========================================="
echo "Starting Slave Server"
echo "========================================="
echo "IP: $IP"
echo "Port: $PORT"
echo "========================================="
echo ""

java -jar target/slave.jar "$IP" "$PORT"
