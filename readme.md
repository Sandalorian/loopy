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
      --dry-run              Validate YAML and test connection without executing
  -f, --cypher-file          Path to YAML workload file containing Cypher queries
      --fail-fast            Abort on first query failure
  -h, --help                 Show help message and exit
  -n, --node-labels          Comma-separated node labels
  -P, --password             Neo4j password
      --property-size        Property size in bytes
  -r, --relationship-types   Comma-separated relationship types
      --report-interval      Statistics reporting interval in seconds
      --stats-format         Statistics output format: summary, detailed, json
  -t, --threads              Number of worker threads
  -u, --neo4j-uri           Neo4j connection URI
  -U, --username             Neo4j username
  -v, --verbose              Verbose mode - detailed output
      --verbose-stats        Enable per-query statistics
  -V, --version              Print version information and exit
  -w, --write-ratio          Write operation ratio (0.0-1.0)
```

## YAML-Based Cypher Workload

Loopy supports executing custom Cypher queries defined in a YAML file, providing an alternative to programmatic data generation. This allows you to simulate realistic application workloads with weighted query selection.

### Basic Usage

```bash
# Run with a YAML workload file
java -jar target/loopy-1.0.0.jar --cypher-file=workload.yaml -t 8 -d 300

# Validate workload without executing
java -jar target/loopy-1.0.0.jar --cypher-file=workload.yaml --dry-run

# Enable per-query statistics
java -jar target/loopy-1.0.0.jar --cypher-file=workload.yaml --verbose-stats

# Output statistics in JSON format
java -jar target/loopy-1.0.0.jar --cypher-file=workload.yaml --stats-format=json
```

### YAML Workload Format

```yaml
name: "E-commerce Workload"
description: "Simulates typical e-commerce read/write patterns"

queries:
  - id: "find-user-by-id"
    cypher: "MATCH (u:User {id: $userId}) RETURN u"
    weight: 30
    type: read
    parameters:
      userId: "random:uuid"

  - id: "create-order"
    cypher: "CREATE (o:Order {id: $orderId, total: $amount}) RETURN o"
    weight: 10
    type: write
    parameters:
      orderId: "random:uuid"
      amount: "random:double:0:1000"

  - id: "product-search"
    cypher: "MATCH (p:Product) WHERE p.name CONTAINS $term RETURN p LIMIT 20"
    weight: 50
    type: read
    parameters:
      term: "random:string:5"
```

### Parameter Generators

Loopy supports the following parameter generators:

| Generator | Format | Description | Example |
|-----------|--------|-------------|---------|
| UUID | `random:uuid` | Generates a random UUID | `"550e8400-e29b-41d4-a716-446655440000"` |
| Integer | `random:int:min:max` | Random integer in range | `random:int:1:1000` → `42` |
| Double | `random:double:min:max` | Random decimal in range | `random:double:0:100` → `57.832` |
| String | `random:string:length` | Random alphanumeric string | `random:string:8` → `"aB3kL9mP"` |
| Long | `random:long:min:max` | Random long in range | `random:long:0:9999999999` |
| Boolean | `random:boolean` | Random true/false | `true` or `false` |
| Literal | `any value` | Used as-is | `"fixed-value"` |

### Weight-Based Query Selection

Queries are selected based on their configured weights. The weight represents the relative probability of selecting that query compared to others.

**Example:**
- Query A: weight = 30
- Query B: weight = 10
- Query C: weight = 50

Total weight = 90
- Query A will be selected ~33% of the time (30/90)
- Query B will be selected ~11% of the time (10/90)
- Query C will be selected ~56% of the time (50/90)

### Per-Query Statistics

When `--verbose-stats` is enabled, Loopy tracks statistics for each individual query:

```
Per-Query Final Statistics:
----------------------------
find-user-by-id:
  Count: 1234 (writes: 0, reads: 1234)
  Avg Response: 5.2ms
  Percentiles: p50=4.0ms, p95=12.0ms, p99=25.0ms
  Errors: 0
create-order:
  Count: 456 (writes: 456, reads: 0)
  Avg Response: 8.7ms
  Percentiles: p50=7.0ms, p95=18.0ms, p99=35.0ms
  Errors: 2
```

### Example Workload File

See `src/main/resources/example-workload.yaml` for a complete example workload file.

### Validation

Loopy validates the YAML workload file before execution:
- Verifies file exists and is readable
- Parses and validates YAML structure
- Checks all query IDs are unique
- Verifies all weights are positive
- Validates parameter generator specifications
- Optionally validates Cypher syntax against the target database

### Notes

- `--cypher-file` is mutually exclusive with `--node-labels` and `--relationship-types`
- `--write-ratio` is ignored when using `--cypher-file` (queries have explicit types)
- Use `--fail-fast` to abort on the first query failure (default: continue with next query)

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

## Releasing

Loopy uses GitHub Actions for automated releases. When a version tag is pushed, the release workflow automatically builds the application and publishes distribution packages to GitHub Releases.

### Release Steps

1. **Update the version in `pom.xml`:**
   ```bash
   mvn versions:set -DnewVersion=2.1.0
   mvn versions:commit
   ```

2. **Update `CHANGELOG.md`** with release notes for the new version

3. **Commit the version changes:**
   ```bash
   git add pom.xml CHANGELOG.md
   git commit -m "Release v2.1.0"
   ```

4. **Create and push the version tag:**
   ```bash
   git tag v2.1.0
   git push origin main --tags
   ```

5. **Automated release process:**
   - GitHub Actions builds the project with Java 21
   - Creates distribution packages (ZIP and TAR.GZ)
   - Generates SHA256 checksums for verification
   - Publishes a GitHub Release with auto-generated release notes
   - Uploads all artifacts to the release

### Release Artifacts

Each release includes:
- `loopy-X.Y.Z-dist.zip` - Distribution archive (ZIP format)
- `loopy-X.Y.Z-dist.tar.gz` - Distribution archive (TAR.GZ format)
- `checksums-sha256.txt` - SHA256 checksums for verification

Distribution contents:
```
loopy-X.Y.Z/
├── loopy-X.Y.Z.jar          # Executable JAR (run with java -jar)
├── readme.md                 # Documentation
├── CHANGELOG.md              # Version history
├── config.properties         # Default configuration
├── example-workload.yaml     # Example YAML workload
└── scripts/
    ├── loopy-completion.bash # Bash completion
    ├── loopy-completion.zsh  # Zsh completion
    ├── loopy.1               # Man page
    └── install-shell-integration.sh
```

## Use Cases
- Load testing during Neo4j cluster operations
- Simulating realistic database activity during maintenance windows
- Validating cluster behaviors during leadership elections
- Testing connection resilience and failover scenarios
