#!/bin/bash
# Simple build script without Maven dependency

echo "========================================="
echo "Building Distributed Key-Value Store"
echo "(Simple javac build)"
echo "========================================="
echo ""

# Create directories
echo "Creating build directories..."
mkdir -p target/classes
mkdir -p target/lib

# Use JSON library from Maven cache
JSON_JAR="$HOME/.m2/repository/org/json/json/20230227/json-20230227.jar"

if [ ! -f "$JSON_JAR" ]; then
    echo "❌ JSON library not found at $JSON_JAR"
    echo "Please ensure Maven has downloaded the dependency"
    exit 1
fi

echo "✓ Using JSON library from Maven cache"
cp "$JSON_JAR" target/lib/

# Compile all Java files
echo ""
echo "Compiling Java sources..."

# Compile common package first
javac -d target/classes \
      -cp "target/lib/json-20230227.jar" \
      src/main/java/com/kvstore/common/*.java

if [ $? -ne 0 ]; then
    echo "❌ Compilation of common package failed!"
    exit 1
fi

# Compile coordinator package
javac -d target/classes \
      -cp "target/classes:target/lib/json-20230227.jar" \
      src/main/java/com/kvstore/coordinator/*.java

if [ $? -ne 0 ]; then
    echo "❌ Compilation of coordinator package failed!"
    exit 1
fi

# Compile slave package
javac -d target/classes \
      -cp "target/classes:target/lib/json-20230227.jar" \
      src/main/java/com/kvstore/slave/*.java

if [ $? -ne 0 ]; then
    echo "❌ Compilation of slave package failed!"
    exit 1
fi

# Compile client package
javac -d target/classes \
      -cp "target/classes:target/lib/json-20230227.jar" \
      src/main/java/com/kvstore/client/*.java

if [ $? -ne 0 ]; then
    echo "❌ Compilation of client package failed!"
    exit 1
fi

echo "✓ Compilation successful"

# Extract JSON library classes into target/classes
echo ""
echo "Packaging dependencies..."
cd target/classes
jar xf ../lib/json-20230227.jar
rm -rf META-INF
cd ../..

# Create executable JARs with embedded dependencies
echo ""
echo "Creating executable JARs..."

# Coordinator JAR
echo "Main-Class: com.kvstore.coordinator.CoordinationServer" > target/manifest-coordinator.txt
jar cfm target/coordinator.jar target/manifest-coordinator.txt -C target/classes .
echo "✓ Created coordinator.jar"

# Slave JAR
echo "Main-Class: com.kvstore.slave.SlaveServer" > target/manifest-slave.txt
jar cfm target/slave.jar target/manifest-slave.txt -C target/classes .
echo "✓ Created slave.jar"

# Client JAR
echo "Main-Class: com.kvstore.client.Client" > target/manifest-client.txt
jar cfm target/client.jar target/manifest-client.txt -C target/classes .
echo "✓ Created client.jar"

# Clean up
rm target/manifest-*.txt

echo ""
echo "========================================="
echo "✅ Build successful!"
echo "========================================="
echo ""
echo "Created executables:"
echo "  - target/coordinator.jar"
echo "  - target/slave.jar"
echo "  - target/client.jar"
echo ""
echo "To run:"
echo "  ./run-coordinator.sh"
echo "  ./run-slave.sh 127.0.0.1 8081"
echo "  ./run-client.sh"
echo ""
