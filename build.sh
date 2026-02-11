#!/bin/bash

echo "Building Loopy application..."

# Build the application
mvn clean package

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "Run the application with:"
    echo "  java -jar target/loopy-0.1.0.jar"
    echo ""
    echo "Or with custom parameters:"
    echo "  java -jar target/loopy-0.1.0.jar --threads=8 --duration.seconds=600"
else
    echo "Build failed!"
    exit 1
fi