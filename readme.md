# Loopy

A lightweight load generator for Neo4j databases. Loopy creates realistic database activity by executing read and write operations, helping you test cluster behaviour, connection resilience, and performance under load.

## Use Cases

- **Performance Benchmarking** - Measure throughput and response times under load
- **Cluster Operations** - Maintain database activity during rolling restarts or failovers
- **Leadership Elections** - Validate cluster behaviour during leadership switches  
- **Connection Testing** - Test failover scenarios and connection resilience
- **Maintenance Windows** - Simulate realistic activity during database maintenance

## Requirements

- **Java 21** or later 
- **Neo4j database** 

## Download

Download the latest release from [GitHub Releases](https://github.com/Sandalorian/loopy/releases):

- `loopy-X.Y.Z-dist.zip` - For Windows/cross-platform
- `loopy-X.Y.Z-dist.tar.gz` - For macOS/Linux

Extract the archive:

```bash
# macOS/Linux
tar -xzf loopy-3.0.0-dist.tar.gz
cd loopy-3.0.0

# Windows (PowerShell)
Expand-Archive loopy-3.0.0-dist.zip -DestinationPath .
cd loopy-3.0.0
```

### What's Included

```
loopy-3.0.0/
├── loopy-3.0.0.jar          # Executable JAR
├── config.properties         # Default configuration (edit this)
├── example-workload.yaml     # Example YAML workload
├── readme.md                 # This documentation
├── CHANGELOG.md              # Version history
└── scripts/
    ├── loopy-completion.bash # Bash shell completion
    ├── loopy-completion.zsh  # Zsh shell completion
    ├── loopy.1               # Man page
    └── install-shell-integration.sh
```

## Quick Start

### 1. Test Your Connection

```bash
java -jar loopy-3.0.0.jar test-connection -u bolt://localhost:7687 -U neo4j -P password
```

### 2. Run a Simple Load Test

Generate load for 60 seconds with 4 threads:

```bash
java -jar loopy-3.0.0.jar run -u bolt://localhost:7687 -U neo4j -P password -t 4 -d 60
```

### 3. Run with a Custom Workload

Use YAML-defined Cypher queries for realistic workloads:

```bash
java -jar loopy-3.0.0.jar run --cypher-file=example-workload.yaml -u bolt://localhost:7687 -U neo4j -P password
```

## Configuration

Loopy can be configured via command-line arguments or a properties file. Edit the included `config.properties` file to set your defaults:

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

Then run with your configuration:

```bash
java -jar loopy-3.0.0.jar run --config=config.properties
```

Command-line arguments override configuration file settings.

## Usage Examples

### Basic Load Generation

```bash
# 4 threads for 5 minutes (300 seconds)
java -jar loopy-3.0.0.jar run -t 4 -d 300 -u bolt://localhost:7687 -U neo4j -P password

# 8 threads with 80% write operations
java -jar loopy-3.0.0.jar run -t 8 -d 600 -w 0.8 -u bolt://localhost:7687 -U neo4j -P password

# Connect to a remote cluster
java -jar loopy-3.0.0.jar run -t 4 -d 300 -u neo4j://cluster.example.com:7687 -U neo4j -P password
```

### Custom Data Patterns

```bash
# Custom node labels and relationship types
java -jar loopy-3.0.0.jar run -t 4 -d 300 \
  --node-labels="Customer,Product,Order" \
  --relationship-types="PURCHASED,REVIEWED,RECOMMENDED" \
  -u bolt://localhost:7687 -U neo4j -P password
```

### Enable Logging

```bash
# CSV output for analysis
java -jar loopy-3.0.0.jar run -t 4 -d 300 --csv-logging --csv-file=results.csv \
  -u bolt://localhost:7687 -U neo4j -P password

# Verbose output
java -jar loopy-3.0.0.jar run -t 4 -d 300 --verbose \
  -u bolt://localhost:7687 -U neo4j -P password

# JSON statistics format
java -jar loopy-3.0.0.jar run -t 4 -d 300 --stats-format=json \
  -u bolt://localhost:7687 -U neo4j -P password
```

## Output

Loopy displays real-time statistics during execution:

```
[2026-02-10 10:30:00] Operations: 12,450 | Write/sec: 285 | Read/sec: 122 | Avg Response: 15ms | Errors: 0
[2026-02-10 10:30:10] Operations: 15,230 | Write/sec: 278 | Read/sec: 118 | Avg Response: 18ms | Errors: 2
```

**Metrics reported:**
- Operations per second (read/write separately)
- Average response times
- Error counts and connection issues
- Total operations completed

## Command Reference

### Commands

| Command | Description |
|---------|-------------|
| `run` | Execute load generation |
| `test-connection` | Test database connectivity (supports cluster node testing) |
| `validate` | Validate a YAML workload file |
| `benchmark` | Run performance benchmarks |
| `setup` | Interactive setup wizard |
| `config` | View/edit configuration |
| `report` | Generate performance reports |
| `security` | Manage credentials |

### Global Options

| Option | Short | Description |
|--------|-------|-------------|
| `--neo4j-uri` | `-u` | Neo4j connection URI |
| `--username` | `-U` | Neo4j username |
| `--password` | `-P` | Neo4j password |
| `--config` | `-c` | Configuration file path |
| `--help` | `-h` | Show help message |
| `--version` | `-V` | Print version information |
| `--verbose` | `-v` | Enable verbose output |

### Run Command Options

| Option | Short | Description | Default |
|--------|-------|-------------|---------|
| `--threads` | `-t` | Number of worker threads | 4 |
| `--duration` | `-d` | Test duration in seconds | 300 |
| `--write-ratio` | `-w` | Write operation ratio (0.0-1.0) | 0.7 |
| `--batch-size` | `-b` | Batch size for operations | 100 |
| `--node-labels` | `-n` | Comma-separated node labels | Person,Product,Order |
| `--relationship-types` | `-r` | Comma-separated relationship types | KNOWS,PURCHASED,CONTAINS |
| `--property-size` | | Property size in bytes | 1024 |
| `--report-interval` | | Statistics interval in seconds | 10 |
| `--csv-logging` | | Enable CSV logging | false |
| `--csv-file` | | CSV output file path | loopy-stats.csv |
| `--stats-format` | | Output format: summary, detailed, json | summary |
| `--cypher-file` | `-f` | Path to YAML workload file | |
| `--dry-run` | | Validate without executing | false |
| `--fail-fast` | | Abort on first query failure | false |
| `--verbose-stats` | | Enable per-query statistics | false |

## YAML Workloads

For realistic application simulation, define custom Cypher queries in a YAML file. This provides fine-grained control over query types, frequencies, and parameters.

### Example Workload

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

### Running YAML Workloads

```bash
# Run workload
java -jar loopy-3.0.0.jar run --cypher-file=workload.yaml -t 8 -d 300 \
  -u bolt://localhost:7687 -U neo4j -P password

# Validate workload before running
java -jar loopy-3.0.0.jar validate --cypher-file=workload.yaml

# Dry run (validate + test connection, no execution)
java -jar loopy-3.0.0.jar run --cypher-file=workload.yaml --dry-run \
  -u bolt://localhost:7687 -U neo4j -P password

# Enable per-query statistics
java -jar loopy-3.0.0.jar run --cypher-file=workload.yaml --verbose-stats \
  -u bolt://localhost:7687 -U neo4j -P password
```

### Parameter Generators

| Generator | Format | Description | Example Output |
|-----------|--------|-------------|----------------|
| UUID | `random:uuid` | Random UUID | `550e8400-e29b-41d4-a716-446655440000` |
| Integer | `random:int:min:max` | Random integer in range | `random:int:1:1000` → `42` |
| Double | `random:double:min:max` | Random decimal in range | `random:double:0:100` → `57.83` |
| String | `random:string:length` | Random alphanumeric | `random:string:8` → `aB3kL9mP` |
| Long | `random:long:min:max` | Random long in range | `random:long:0:9999999999` |
| Boolean | `random:boolean` | Random true/false | `true` or `false` |
| Literal | `any value` | Used as-is | `"fixed-value"` |

### Weight-Based Selection

Queries are selected randomly based on their weights. Higher weights mean more frequent selection.

**Example:** With weights of 30, 10, and 50 (total: 90):
- Query A (weight 30): ~33% of executions
- Query B (weight 10): ~11% of executions  
- Query C (weight 50): ~56% of executions

### Per-Query Statistics

With `--verbose-stats`, Loopy tracks individual query performance:

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

### YAML Workload Notes

- `--cypher-file` cannot be used with `--node-labels` or `--relationship-types`
- `--write-ratio` is ignored (query types are defined in YAML)
- Use `--fail-fast` to stop on the first error

## Shell Completion

Enable tab completion for Loopy commands:

### Bash
```bash
source scripts/loopy-completion.bash
```

### Zsh
```bash
source scripts/loopy-completion.zsh
```

To install permanently, run:
```bash
./scripts/install-shell-integration.sh
```

## Man Page

View the manual page:
```bash
man ./scripts/loopy.1
```

---

# Developer Guide

The following sections are for developers who want to build Loopy from source or contribute to the project.

## Building from Source

### Prerequisites

- Java 21 or later
- Maven 3.6 or later

### Build Commands

```bash
# Clone the repository
git clone https://github.com/Sandalorian/loopy.git
cd loopy

# Build the application
mvn clean package

# Run from source
java -jar target/loopy-3.0.0.jar --help
```

### Build Scripts

```bash
# Build with wrapper script
./build.sh

# Run tests
./test.sh
```

## Project Structure

```
loopy/
├── src/
│   ├── main/
│   │   ├── java/com/neo4j/loopy/    # Source code
│   │   └── resources/                # Config files & examples
│   └── assembly/                     # Distribution packaging
├── scripts/                          # Shell completion & man page
├── pom.xml                           # Maven configuration
└── .github/workflows/                # CI/CD pipelines
```

## Releasing

Loopy uses GitHub Actions for automated releases. When a version tag is pushed, the workflow builds and publishes distribution packages.

### Release Process

1. **Update version:**
   ```bash
   mvn versions:set -DnewVersion=2.1.0
   mvn versions:commit
   ```

2. **Update `CHANGELOG.md`** with release notes

3. **Commit and tag:**
   ```bash
   git add pom.xml CHANGELOG.md
   git commit -m "Release v2.1.0"
   git tag v2.1.0
   git push origin main --tags
   ```

4. **Automated steps:**
   - GitHub Actions builds with Java 21
   - Creates distribution packages (ZIP + TAR.GZ)
   - Generates SHA256 checksums
   - Publishes GitHub Release with artifacts

### Release Artifacts

Each release includes:
- `loopy-X.Y.Z-dist.zip` - Distribution (ZIP)
- `loopy-X.Y.Z-dist.tar.gz` - Distribution (TAR.GZ)
- `checksums-sha256.txt` - SHA256 verification

## Docker (Optional)

Build a Docker image:

```dockerfile
FROM eclipse-temurin:21-jre
COPY loopy-3.0.0.jar /app/loopy.jar
COPY config.properties /app/config.properties
WORKDIR /app
ENTRYPOINT ["java", "-jar", "loopy.jar"]
```

Build and run:
```bash
docker build -t loopy .
docker run loopy run -u bolt://host.docker.internal:7687 -U neo4j -P password -t 4 -d 60
```

## Legacy Argument Support

For backward compatibility, Loopy accepts the old argument format:

```bash
# Old format (still supported)
java -jar loopy-3.0.0.jar --duration.seconds=300 --neo4j.uri=bolt://localhost:7687

# New format (recommended)
java -jar loopy-3.0.0.jar run --duration=300 --neo4j-uri=bolt://localhost:7687
```

## License

See [LICENSE](LICENSE) for details.
