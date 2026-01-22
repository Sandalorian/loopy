#!/bin/bash

echo "Simple compilation script for Loopy"
echo "Note: This requires Neo4j Java Driver JAR file"
echo ""

# Check if Neo4j driver is available
if [ ! -f "lib/neo4j-java-driver-5.15.0.jar" ]; then
    echo "Error: Neo4j Java driver not found in lib/ directory"
    echo "Please download neo4j-java-driver-5.15.0.jar and place it in lib/ directory"
    echo "Download from: https://repo1.maven.org/maven2/org/neo4j/driver/neo4j-java-driver/5.15.0/"
    exit 1
fi

# Create output directory
mkdir -p build/classes

# Compile Java files
echo "Compiling Java sources..."
javac -cp "lib/*" -d build/classes src/main/java/com/neo4j/loopy/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    
    # Copy resources
    mkdir -p build/classes/
    cp src/main/resources/* build/classes/ 2>/dev/null || true
    
    # Create simple JAR
    cd build/classes
    jar -cf ../loopy.jar .
    cd ../..
    
    echo "JAR created: build/loopy.jar"
    echo ""
    echo "Run with:"
    echo "  java -cp \"lib/*:build/loopy.jar\" com.neo4j.loopy.LoopyApplication"
else
    echo "Compilation failed!"
    exit 1
fi