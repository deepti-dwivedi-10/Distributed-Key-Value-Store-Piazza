#!/bin/bash
# Stop all running instances of the Distributed Key-Value Store

echo "========================================="
echo "Stopping Distributed Key-Value Store"
echo "========================================="
echo ""

# Find and kill all Java processes related to our JARs
echo "Looking for running processes..."
echo ""

COORD_PID=$(pgrep -f "coordinator.jar")
SLAVE_PIDS=$(pgrep -f "slave.jar")
CLIENT_PID=$(pgrep -f "client.jar")

if [ -z "$COORD_PID" ] && [ -z "$SLAVE_PIDS" ] && [ -z "$CLIENT_PID" ]; then
    echo "No running processes found."
    echo ""
    exit 0
fi

# Kill coordination server
if [ -n "$COORD_PID" ]; then
    echo "Stopping Coordination Server (PID: $COORD_PID)..."
    kill $COORD_PID 2>/dev/null
    echo "✓ Coordination Server stopped"
fi

# Kill slave servers
if [ -n "$SLAVE_PIDS" ]; then
    echo "Stopping Slave Servers (PIDs: $SLAVE_PIDS)..."
    kill $SLAVE_PIDS 2>/dev/null
    echo "✓ Slave Servers stopped"
fi

# Kill client
if [ -n "$CLIENT_PID" ]; then
    echo "Stopping Client (PID: $CLIENT_PID)..."
    kill $CLIENT_PID 2>/dev/null
    echo "✓ Client stopped"
fi

echo ""
echo "========================================="
echo "✓ All processes stopped"
echo "========================================="
echo ""
echo "To start again: ./start-all.sh"
echo ""
