#!/bin/bash
# Automated startup script for Distributed Key-Value Store
# Opens 4 terminal windows and starts all components

echo "========================================="
echo "Starting Distributed Key-Value Store"
echo "========================================="
echo ""

# Get the current directory
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Project directory: $PROJECT_DIR"
echo ""

# Check if JARs exist
if [ ! -f "$PROJECT_DIR/target/coordinator.jar" ]; then
    echo "Error: JAR files not found!"
    echo "Please run ./build-simple.sh first"
    exit 1
fi

echo "✓ JAR files found"
echo ""
echo "Opening 4 terminal windows..."
echo ""

# Use AppleScript to open 4 Terminal windows
osascript <<EOF
tell application "Terminal"
    -- Activate Terminal
    activate

    -- Window 1: Coordination Server
    do script "cd '$PROJECT_DIR' && echo '=========================================' && echo 'TERMINAL 1: COORDINATION SERVER' && echo '=========================================' && echo '' && ./run-coordinator.sh"
    delay 2

    -- Window 2: Slave Server 1
    do script "cd '$PROJECT_DIR' && echo '=========================================' && echo 'TERMINAL 2: SLAVE SERVER 1' && echo '=========================================' && echo '' && sleep 3 && ./run-slave.sh 127.0.0.1 8081"
    delay 1

    -- Window 3: Slave Server 2
    do script "cd '$PROJECT_DIR' && echo '=========================================' && echo 'TERMINAL 3: SLAVE SERVER 2' && echo '=========================================' && echo '' && sleep 4 && ./run-slave.sh 127.0.0.1 8082"
    delay 1

    -- Window 4: Client
    do script "cd '$PROJECT_DIR' && echo '=========================================' && echo 'TERMINAL 4: CLIENT' && echo '=========================================' && echo '' && echo 'Waiting for servers to start...' && sleep 6 && ./run-client.sh"
end tell
EOF

echo ""
echo "========================================="
echo "✓ All terminals opened!"
echo "========================================="
echo ""
echo "You should now see 4 Terminal windows:"
echo "  1. Coordination Server (port 8080)"
echo "  2. Slave Server 1 (port 8081)"
echo "  3. Slave Server 2 (port 8082)"
echo "  4. Client (interactive)"
echo ""
echo "In the CLIENT window, try these commands:"
echo "  put:username:alice"
echo "  get:username"
echo "  update:username:bob"
echo "  get:username"
echo "  delete:username"
echo "  exit"
echo ""
echo "To stop all servers: ./stop-all.sh"
echo ""
