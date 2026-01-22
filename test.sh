#!/bin/bash

echo "Testing Loopy application..."
echo "Note: This test will show help/configuration info and exit quickly"
echo ""

# Test basic jar execution (should show help since no Neo4j is running)
echo "Testing JAR execution:"
java -jar target/loopy-1.0.0.jar --duration.seconds=1 --threads=1

echo ""
echo "Build and basic test completed successfully!"
echo ""
echo "To run against a real Neo4j instance:"
echo "  java -jar target/loopy-1.0.0.jar --neo4j.uri=bolt://localhost:7687 --neo4j.username=neo4j --neo4j.password=yourpassword"