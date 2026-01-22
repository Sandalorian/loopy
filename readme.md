# Loopy

Loopy is a simple Java application that generates load for a Neo4j database by creating and reading nodes, relationships, and properties in a loop. 

The application is designed to keep the database busy while other operations on the DBMS are being performed. For example, load can be applied during a rolling restart of a Neo4j cluster, or a leadership election/ switch.

Loopy will output key statistics about the load generation process, including the number of operations performed and the time taken for each operation, and any connectivity issues encountered.

## Features
- Simple configuration via properties file or command-line arguments
- Configurable load generation parameters (concurrency, duration, operation ratios)
- Supports creating nodes, relationships, and properties with realistic data patterns
- Reads nodes, relationships, and properties to simulate real-world usage
- Real-time progress reporting with key performance metrics
- Graceful error handling and connection retry logic
- Lightweight with minimal dependencies (Neo4j driver only)

## Configuration

Loopy can be configured through a `config.properties` file or command-line arguments:

```properties
# Neo4j Connection
neo4j.uri=bolt://localhost:7687
neo4j.username=neo4j
neo4j.password=password

# Load Parameters
threads=4
duration.seconds=300
write.ratio=0.7
batch.size=100

# Data Generation
node.labels=Person,Product,Order
relationship.types=KNOWS,PURCHASED,CONTAINS
property.size.bytes=1024
```

## Output

Loopy provides real-time monitoring with periodic reports:

```
[2025-11-14 10:30:00] Operations: 12,450 | Write/sec: 285 | Read/sec: 122 | Avg Response: 15ms | Errors: 0
[2025-11-14 10:30:10] Operations: 15,230 | Write/sec: 278 | Read/sec: 118 | Avg Response: 18ms | Errors: 2
```

Key metrics include:
- Operations per second (read/write separately)
- Average response times
- Error rates and connection issues
- Total operations completed

## Building

```bash
# Build with Maven
mvn clean package

# Or use the build script
./build.sh
```

## Usage

```bash
# Run with default configuration
java -jar target/loopy-1.0.0.jar

# Override configuration via command line  
java -jar target/loopy-1.0.0.jar --threads=8 --duration.seconds=600 --neo4j.uri=bolt://remote:7687

# Use custom configuration file
java -jar target/loopy-1.0.0.jar --config=/path/to/custom.properties
```

## Use Cases
- Load testing during Neo4j cluster operations
- Simulating realistic database activity during maintenance windows
- Validating cluster behaviors during leadership elections
- Testing connection resilience and failover scenarios
