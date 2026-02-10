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

Loopy can be configured through a `config.properties` file or command-line arguments. Edit `src/main/resources/config.properties` or create your own configuration file with these parameters:

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

# Reporting
report.interval.seconds=10
csv.logging.enabled=false
csv.logging.file=loopy-stats.csv
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

## Prerequisites

- Java 11 or later
- Maven 3.6 or later
- Neo4j database (running locally or remotely)

## Building

### Using Maven directly:
```bash
mvn clean package
```

### Using the build script:
```bash
./build.sh
```

### Testing the build:
```bash
./test.sh
```

## Usage

### Basic usage (default configuration):
```bash
java -jar target/loopy-1.0.0.jar
```

### Show help and all available options:
```bash
java -jar target/loopy-1.0.0.jar --help
```

### With short options:
```bash
java -jar target/loopy-1.0.0.jar -t 8 -d 600 -u bolt://remote:7687 -U neo4j -P mypassword
```

### With long options:
```bash
java -jar target/loopy-1.0.0.jar --threads=8 --duration=600 --neo4j-uri=bolt://remote:7687
```

### Mixed short and long options:
```bash
java -jar target/loopy-1.0.0.jar -t 4 --duration=300 --write-ratio=0.8 -n "Person,Product"
```

### With custom configuration file:
```bash
java -jar target/loopy-1.0.0.jar --config=custom-config.properties --threads=16
```

### Enable CSV logging:
```bash
java -jar target/loopy-1.0.0.jar --csv-logging --csv-file=my-stats.csv
```

### All available CLI options:
```
  -b, --batch-size           Batch size for operations
  -c, --config               Configuration file path  
      --csv-file             CSV output file path
      --csv-logging          Enable CSV logging
  -d, --duration             Test duration in seconds
  -h, --help                 Show help message and exit
  -n, --node-labels          Comma-separated node labels
  -P, --password             Neo4j password
      --property-size        Property size in bytes
  -r, --relationship-types   Comma-separated relationship types
      --report-interval      Statistics reporting interval in seconds
  -t, --threads              Number of worker threads
  -u, --neo4j-uri           Neo4j connection URI
  -U, --username             Neo4j username
  -V, --version              Print version information and exit
  -w, --write-ratio          Write operation ratio (0.0-1.0)
```

### Backward Compatibility

Loopy maintains backward compatibility with the old argument format:
```bash
# Old format (still supported)
java -jar target/loopy-1.0.0.jar --duration.seconds=300 --neo4j.uri=bolt://localhost:7687

# New format (recommended)
java -jar target/loopy-1.0.0.jar --duration=300 --neo4j-uri=bolt://localhost:7687
```

## Docker Usage (Optional)

You can also containerize the application using Docker:

```dockerfile
FROM openjdk:11-jre-slim
COPY target/loopy-1.0.0.jar /app/loopy.jar
WORKDIR /app
CMD ["java", "-jar", "loopy.jar"]
```

Build and run:
```bash
docker build -t loopy .
docker run -e NEO4J_URI=bolt://host.docker.internal:7687 loopy --neo4j.uri=$NEO4J_URI
```

## Use Cases
- Load testing during Neo4j cluster operations
- Simulating realistic database activity during maintenance windows
- Validating cluster behaviors during leadership elections
- Testing connection resilience and failover scenarios
