# Build Instructions for Loopy

## Prerequisites

- Java 11 or later
- Neo4j database (running locally or remotely)

## Building with Maven (Recommended)

If you have Maven installed:

```bash
mvn clean package
java -jar target/loopy-1.0.0.jar
```

## Building without Maven

1. Create a `lib` directory and download the Neo4j Java driver:

```bash
mkdir lib
cd lib
curl -O https://repo1.maven.org/maven2/org/neo4j/driver/neo4j-java-driver/5.15.0/neo4j-java-driver-5.15.0.jar
cd ..
```

2. Use the compile script:

```bash
chmod +x compile.sh
./compile.sh
```

3. Run the application:

```bash
java -cp "lib/*:build/loopy.jar" com.neo4j.loopy.LoopyApplication
```

## Running Examples

### Basic usage (default configuration):
```bash
java -jar target/loopy-1.0.0.jar
```

### With custom parameters:
```bash
java -jar target/loopy-1.0.0.jar --threads=8 --duration.seconds=600 --neo4j.uri=bolt://remote:7687
```

### With custom configuration file:
```bash
java -jar target/loopy-1.0.0.jar --config=custom-config.properties
```

### Enable CSV logging:
```bash
java -jar target/loopy-1.0.0.jar --csv.logging.enabled=true --csv.logging.file=my-stats.csv
```

## Configuration

Edit `src/main/resources/config.properties` or create your own configuration file with these parameters:

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

## Docker Usage (Optional)

You can also create a simple Dockerfile:

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