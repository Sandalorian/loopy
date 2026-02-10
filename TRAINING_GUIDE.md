# Loopy Training Guide
## Comprehensive User Documentation for Neo4j Load Testing

**Version:** 1.0.0  
**Last Updated:** February 6, 2026

---

## Table of Contents

1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Global Options](#global-options)
4. [Command Reference](#command-reference)
5. [Hands-On Training Modules](#hands-on-training-modules)
6. [Advanced Usage Scenarios](#advanced-usage-scenarios)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

---

## 1. Introduction

### What is Loopy?

Loopy is an enterprise-grade Neo4j load testing tool that generates configurable workloads to test database performance. It provides:

- **Flexible load patterns** - Configure threads, duration, and operation types
- **Interactive setup** - Guided configuration creation
- **Performance optimization** - Auto-tuning and load profiles
- **Advanced reporting** - HTML, Markdown, and CSV outputs
- **Enterprise features** - Scheduling, clustering, and security
- **Shell integration** - Autocompletion and man pages

### Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                  Loopy CLI                          │
├─────────────────────────────────────────────────────┤
│  Commands: run | setup | config | tune | report    │
│            test-connection | benchmark | validate   │
│            schedule | cluster | security            │
├─────────────────────────────────────────────────────┤
│              Configuration Layer                     │
│  (config.properties + CLI overrides)                │
├─────────────────────────────────────────────────────┤
│              Worker Thread Pool                      │
│  (Parallel Neo4j operations)                        │
├─────────────────────────────────────────────────────┤
│              Neo4j Driver                           │
│  (bolt:// or neo4j:// connections)                 │
└─────────────────────────────────────────────────────┘
```

---

## 2. Getting Started

### Installation

```bash
# Build the project
mvn clean package

# Run Loopy
java -jar target/loopy-1.0.0.jar --help
```

### Quick Start (3 minutes)

**Step 1:** Interactive Setup
```bash
java -jar loopy.jar setup
```
Follow the prompts to create your first configuration file.

**Step 2:** Test Connection
```bash
java -jar loopy.jar test-connection --quick
```

**Step 3:** Run Your First Load Test
```bash
java -jar loopy.jar run --duration 30 --threads 2
```

---

## 3. Global Options

These options are available for the main `loopy` command and apply across all subcommands:

### Connection Options

| Option | Short | Description | Default | Example |
|--------|-------|-------------|---------|---------|
| `--neo4j-uri` | `-u` | Neo4j connection URI | `bolt://localhost:7687` | `-u bolt://prod-server:7687` |
| `--username` | `-U` | Neo4j username | `neo4j` | `-U admin` |
| `--password` | `-P` | Neo4j password (interactive prompt) | `password` | `-P` (prompts securely) |
| `--config` | `-c` | Configuration file path | `config.properties` | `-c prod-config.properties` |

**Environment Variables:**
- `LOOPY_NEO4J_URI` - Default URI
- `LOOPY_USERNAME` - Default username
- `LOOPY_PASSWORD` - Default password
- `LOOPY_CONFIG_FILE` - Default config file

### Load Testing Options

| Option | Short | Description | Default | Range | Example |
|--------|-------|-------------|---------|-------|---------|
| `--threads` | `-t` | Number of worker threads | `4` | 1-100 | `-t 8` |
| `--duration` | `-d` | Test duration (seconds) | `300` | ≥1 | `-d 60` |
| `--write-ratio` | `-w` | Write operation ratio | `0.7` | 0.0-1.0 | `-w 0.5` |
| `--batch-size` | `-b` | Batch size for operations | `100` | ≥1 | `-b 500` |

### Data Configuration Options

| Option | Short | Description | Default | Example |
|--------|-------|-------------|---------|---------|
| `--node-labels` | `-n` | Comma-separated node labels | `Person,Company` | `-n User,Product,Order` |
| `--relationship-types` | `-r` | Comma-separated rel types | `WORKS_FOR,KNOWS` | `-r PURCHASED,REVIEWED` |
| `--property-size` | | Property size in bytes | `1024` | `--property-size 2048` |

### Reporting Options

| Option | Description | Default | Example |
|--------|-------------|---------|---------|
| `--report-interval` | Stats reporting interval (seconds) | `10` | `--report-interval 30` |
| `--csv-logging` | Enable CSV logging | `false` | `--csv-logging` |
| `--csv-file` | CSV output file path | `loopy-results.csv` | `--csv-file results.csv` |

### System Options

| Option | Short | Description |
|--------|-------|-------------|
| `--help` | `-h` | Show help information |
| `--version` | `-V` | Show version information |
| `--quiet` | `-q` | Minimal output |
| `--verbose` | `-v` | Detailed output |

---

## 4. Command Reference

### 4.1 `run` - Execute Load Test

**Purpose:** Run the main load testing operation with configured parameters.

**Usage:**
```bash
loopy run [OPTIONS]
```

**Examples:**
```bash
# Basic run with defaults
loopy run

# Quick 30-second test
loopy run --duration 30 --threads 2

# High-intensity test
loopy run --threads 16 --duration 600 --write-ratio 0.8 --batch-size 2000

# Using configuration file
loopy run --config production.properties

# Custom data model
loopy run --node-labels User,Product --relationship-types PURCHASED,REVIEWED
```

**Training Exercise:**
1. Run a 30-second test with 2 threads
2. Compare with a 60-second test with 4 threads
3. Observe the throughput differences
4. Enable CSV logging to track detailed metrics

---

### 4.2 `setup` - Interactive Setup Wizard

**Purpose:** Guided configuration creation with real-time validation.

**Usage:**
```bash
loopy setup [OPTIONS]
```

**Options:**
- `--output`, `-o` - Output file path (default: `config.properties`)
- `--skip-test` - Skip connection testing during setup

**Examples:**
```bash
# Interactive setup with default output
loopy setup

# Setup with custom output location
loopy setup --output ~/configs/loopy-prod.properties

# Setup without connection testing
loopy setup --skip-test
```

**Interactive Prompts:**
1. **Neo4j Connection:**
   - URI (e.g., `bolt://localhost:7687`)
   - Username (default: `neo4j`)
   - Password (secure, no echo)

2. **Load Testing Parameters:**
   - Worker threads (1-100)
   - Test duration (seconds)
   - Write ratio (0.0-1.0)
   - Batch size

3. **Data Configuration:**
   - Node labels
   - Relationship types
   - Property size

4. **Reporting:**
   - Report interval
   - CSV logging
   - CSV file path

**Training Exercise:**
1. Run `loopy setup`
2. Follow prompts to create a development configuration
3. Test the connection during setup
4. Save as `dev-config.properties`
5. Run a test using your new config: `loopy run -c dev-config.properties`

---

### 4.3 `config` - Configuration Management

**Purpose:** Manage configuration files with validation and editing capabilities.

**Usage:**
```bash
loopy config <SUBCOMMAND> [OPTIONS]
```

#### Subcommands:

##### `config init` - Generate Default Configuration
```bash
loopy config init --output config.properties
```

##### `config validate` - Validate Configuration File
```bash
loopy config validate --config myconfig.properties
```

**Validation checks:**
- File existence and format
- Required properties
- Value ranges (threads, duration, ratios)
- Neo4j URI format
- Warning for suboptimal values

##### `config show` - Display Current Configuration
```bash
loopy config show --config config.properties
```

**Displays:**
- Neo4j connection details
- Load testing parameters
- Data configuration
- Reporting settings

##### `config edit` - Open in Editor
```bash
loopy config edit --config config.properties
```

**Opens file in:**
- macOS: TextEdit (or `$EDITOR`)
- Linux: nano/vim/gedit (or `$EDITOR`)
- Windows: notepad

**Training Exercise:**
1. Generate a default config: `loopy config init`
2. View the configuration: `loopy config show`
3. Edit it: `loopy config edit`
4. Validate your changes: `loopy config validate`

---

### 4.4 `test-connection` - Connection Testing & Diagnostics

**Purpose:** Test Neo4j connectivity (single node or cluster) with comprehensive diagnostics.

**Usage:**
```bash
loopy test-connection [OPTIONS]
```

**Options:**
- `--neo4j-uri`, `-u` - Neo4j URI to test (supports bolt://, neo4j://, bolt+s://, neo4j+s://)
- `--nodes` - Comma-separated list of cluster nodes to test individually
- `--username`, `-U` - Username
- `--password`, `-P` - Password
- `--quick` - Quick connectivity test only
- `--full-diagnostics`, `--diag` - Comprehensive diagnostics
- `--save-report` - Save diagnostic report to file

**Cluster Support:**
The command automatically detects cluster URIs (neo4j://) and provides cluster-specific testing:
- Tests routing capabilities
- Validates cluster connectivity
- Reports on member availability

**URI Schemes:**
- `bolt://` - Direct connection to single instance
- `bolt+s://` - Direct connection with TLS
- `neo4j://` - Cluster routing connection (auto-detected)
- `neo4j+s://` - Cluster routing with TLS (auto-detected)

**Examples:**
```bash
# Quick connectivity test (single node)
loopy test-connection --quick

# Basic test with details
loopy test-connection --neo4j-uri bolt://prod:7687 --username neo4j

# Test cluster with routing URI (auto-detected)
loopy test-connection --neo4j-uri neo4j://cluster.example.com:7687

# Test specific cluster nodes individually
loopy test-connection --nodes bolt://node1:7687,bolt://node2:7687,bolt://node3:7687

# Full diagnostics with report
loopy test-connection --full-diagnostics --save-report diagnostics.txt

# Full diagnostics on cluster nodes (creates separate reports)
loopy test-connection --nodes bolt://node1:7687,bolt://node2:7687 \
                      --full-diagnostics --save-report cluster-diag.txt
```

**Test Levels:**

**Quick Test:**
- Basic connectivity check
- Fast verification (< 5 seconds)

**Basic Test (default):**
- Connectivity verification
- Database version check
- Read permissions test
- Write permissions test
- Relationship creation test

**Full Diagnostics:**
- All basic tests
- Performance metrics
- Latency analysis
- Permission validation
- System resource check
- Configuration recommendations

**Training Exercise:**
1. Run a quick test: `loopy test-connection --quick`
2. Run full diagnostics: `loopy test-connection --full-diagnostics`
3. Save a report: `loopy test-connection --diag --save-report report.txt`
4. Test multiple nodes: `loopy test-connection --nodes bolt://node1:7687,bolt://node2:7687`
5. Review recommendations in the report

---

### 4.5 `tune` - Performance Optimization

**Purpose:** Analyze system resources and provide optimization recommendations.

**Usage:**
```bash
loopy tune [OPTIONS]
```

**Options:**
- `--config`, `-c` - Configuration file to analyze
- `--profile`, `-p` - Load profile (light, medium, heavy, stress)
- `--auto-tune` - Apply automatic tuning recommendations
- `--show-profiles` - Show all available load profiles

**Examples:**
```bash
# Show available profiles
loopy tune --show-profiles

# Show specific profile details
loopy tune --profile medium

# System analysis
loopy tune

# Auto-tuning recommendations
loopy tune --auto-tune
```

**Load Profiles:**

| Profile | Threads | Batch Size | Duration | Write Ratio | Use Case |
|---------|---------|------------|----------|-------------|----------|
| **light** | 2 | 100 | 30s | 30% | Development testing |
| **medium** | 4 | 500 | 120s | 50% | Staging environments |
| **heavy** | 8 | 1000 | 300s | 70% | Production testing |
| **stress** | 16 | 2000 | 600s | 80% | Stress testing |

**Training Exercise:**
1. View all profiles: `loopy tune --show-profiles`
2. Check medium profile: `loopy tune --profile medium`
3. Run system analysis: `loopy tune`
4. Note CPU cores and memory recommendations
5. Run auto-tuning: `loopy tune --auto-tune`
6. Apply recommendations to your config file

---

### 4.6 `report` - Generate Reports

**Purpose:** Generate detailed reports from test results.

**Usage:**
```bash
loopy report [OPTIONS]
```

**Options:**
- `--input`, `-i` - Input CSV file with test results
- `--output`, `-o` - Output file path (default: `loopy-report.html`)
- `--format`, `-f` - Output format (html, markdown, csv)
- `--template` - Template (basic, detailed, executive)

**Examples:**
```bash
# Generate HTML report
loopy report --output results.html

# Generate Markdown report
loopy report --format markdown --output report.md

# Generate CSV export
loopy report --format csv --output data.csv

# Using specific template
loopy report --template detailed --output detailed-report.html
```

**Report Formats:**

**HTML Report:**
- Rich, interactive format
- Charts and metrics
- Professional styling
- Browser-viewable

**Markdown Report:**
- Text-based format
- Git-friendly
- Documentation-ready
- Easy to share

**CSV Export:**
- Raw data export
- Spreadsheet compatible
- Analysis-ready
- Time-series data

**Report Contents:**
- Test configuration summary
- Performance metrics (ops/sec, latency)
- Response time percentiles (p50, p95, p99)
- Throughput analysis
- Optimization recommendations

**Training Exercise:**
1. Run a test with CSV logging:
   ```bash
   loopy run --duration 60 --csv-logging --csv-file test-data.csv
   ```
2. Generate HTML report:
   ```bash
   loopy report --output my-report.html
   ```
3. Generate Markdown report:
   ```bash
   loopy report --format markdown --output report.md
   ```
4. Review and share the reports

---

### 4.7 `validate` - Configuration Validation

**Purpose:** Validate configuration without running tests.

**Usage:**
```bash
loopy validate [OPTIONS]
```

**Examples:**
```bash
# Validate default config
loopy validate

# Validate specific config
loopy validate --config staging.properties
```

**Validation Checks:**
- Configuration file syntax
- Required properties present
- Value ranges valid
- Neo4j URI format correct
- Performance warnings
- Security best practices

---

### 4.8 `benchmark` - Predefined Benchmarks

**Purpose:** Run standardized benchmark scenarios.

**Usage:**
```bash
loopy benchmark [OPTIONS]
```

**Examples:**
```bash
# Run default benchmark
loopy benchmark

# Run specific benchmark profile
loopy benchmark --profile heavy
```

**Benchmark Scenarios:**
- Standard CRUD operations
- Relationship traversals
- Complex pattern matching
- Batch operations

---

### 4.9 `schedule` - Enterprise Scheduling

**Purpose:** Schedule load tests for enterprise environments.

**Usage:**
```bash
loopy schedule [OPTIONS]
```

**Options:**
- `--config`, `-c` - Configuration file for scheduled tests
- `--cron` - Cron expression (minute hour day month dayOfWeek)
- `--delay` - Delay in seconds for one-time execution
- `--max-runs` - Maximum number of runs (0 for unlimited)
- `--daemon` - Run as daemon process
- `--list` - List active scheduled tasks

**Examples:**
```bash
# Schedule one-time test with 60-second delay
loopy schedule --delay 60 --config test.properties

# Schedule recurring test (every hour)
loopy schedule --cron "0 * * * *" --config hourly.properties

# Schedule with max runs
loopy schedule --delay 30 --max-runs 5

# Run as daemon
loopy schedule --cron "0 */2 * * *" --daemon
```

**Interactive Mode Commands:**
- `status` - Show task status
- `cancel` - Cancel the scheduled task
- `quit` - Exit (task continues in background)

**Training Exercise:**
1. Schedule a one-time test:
   ```bash
   loopy schedule --delay 30 --config dev-config.properties
   ```
2. Monitor the execution in interactive mode
3. Check the task status
4. Let it complete and review results

---

### 4.10 `security` - Security Management

**Purpose:** Manage security credentials and audit logs.

**Usage:**
```bash
loopy security [OPTIONS]
```

**Options:**
- `--store-credential` - Store a credential securely (prompts for value)
- `--retrieve-credential` - Retrieve a stored credential
- `--generate-password` - Generate a secure password
- `--password-length` - Length for generated password (default: 16)
- `--check-audit-log` - Check audit log health
- `--security-test` - Run security feature tests

**Examples:**
```bash
# Store a password securely
loopy security --store-credential neo4j_password

# Retrieve stored credential
loopy security --retrieve-credential neo4j_password

# Generate strong password
loopy security --generate-password --password-length 20

# Check audit log
loopy security --check-audit-log

# Run security tests
loopy security --security-test

# Show security status
loopy security
```

**Security Features:**
- Secure password prompting (no echo)
- Encrypted credential storage
- Password strength validation
- Secure password generation
- Audit logging for security events
- File permission hardening (Unix/Linux/Mac)

**Credential Storage Location:**
- `~/.loopy/credentials/secure.properties` (encrypted)
- `~/.loopy/credentials/audit.log` (audit trail)

**Training Exercise:**
1. Generate a strong password:
   ```bash
   loopy security --generate-password
   ```
2. Store it securely:
   ```bash
   loopy security --store-credential prod_password
   ```
3. Check audit log:
   ```bash
   loopy security --check-audit-log
   ```
4. Run security tests:
   ```bash
   loopy security --security-test
   ```

---

## 5. Hands-On Training Modules

### Module 1: Basic Load Testing (30 minutes)

**Objective:** Learn to run basic load tests and interpret results.

**Steps:**
1. Create a configuration:
   ```bash
   loopy setup --output module1-config.properties
   ```

2. Run a 60-second test:
   ```bash
   loopy run --config module1-config.properties --duration 60 --threads 2
   ```

3. Observe the output:
   - Operations per second
   - Response times
   - Success/failure rates

4. Increase threads and compare:
   ```bash
   loopy run --config module1-config.properties --duration 60 --threads 4
   ```

5. Enable CSV logging:
   ```bash
   loopy run --config module1-config.properties --duration 60 --threads 4 \
             --csv-logging --csv-file module1-results.csv
   ```

**Expected Outcomes:**
- Understand basic throughput metrics
- See the impact of thread count
- Generate data for analysis

---

### Module 2: Configuration Management (20 minutes)

**Objective:** Master configuration file operations.

**Steps:**
1. Generate default config:
   ```bash
   loopy config init --output module2-config.properties
   ```

2. View the configuration:
   ```bash
   loopy config show --config module2-config.properties
   ```

3. Edit the configuration:
   ```bash
   loopy config edit --config module2-config.properties
   ```
   - Change `threads=8`
   - Change `duration.seconds=120`
   - Change `write.ratio=0.5`

4. Validate your changes:
   ```bash
   loopy config validate --config module2-config.properties
   ```

5. Run a test with the new config:
   ```bash
   loopy run --config module2-config.properties
   ```

**Expected Outcomes:**
- Comfortable creating and editing configs
- Understand validation warnings
- Know how to override settings

---

### Module 3: Performance Tuning (25 minutes)

**Objective:** Optimize load test performance for your system.

**Steps:**
1. Analyze your system:
   ```bash
   loopy tune
   ```
   - Note CPU cores available
   - Note memory recommendations

2. View load profiles:
   ```bash
   loopy tune --show-profiles
   ```

3. Compare light vs heavy profiles:
   ```bash
   # Light profile
   loopy run --threads 2 --batch-size 100 --duration 30 --write-ratio 0.3
   
   # Heavy profile
   loopy run --threads 8 --batch-size 1000 --duration 30 --write-ratio 0.7
   ```

4. Run auto-tuning:
   ```bash
   loopy tune --auto-tune
   ```

5. Apply recommendations to your config

**Expected Outcomes:**
- Understand system resource constraints
- Know how to select appropriate profiles
- Apply auto-tuning recommendations

---

### Module 4: Diagnostics and Troubleshooting (20 minutes)

**Objective:** Diagnose connection and performance issues.

**Steps:**
1. Quick connection test:
   ```bash
   loopy test-connection --quick
   ```

2. Full diagnostics:
   ```bash
   loopy test-connection --full-diagnostics
   ```

3. Save diagnostic report:
   ```bash
   loopy test-connection --full-diagnostics --save-report module4-diag.txt
   ```

4. Review the report:
   - Connection status
   - Database version
   - Permission checks
   - Performance metrics
   - Recommendations

5. Fix any issues identified

**Expected Outcomes:**
- Able to diagnose connection problems
- Understand performance bottlenecks
- Apply diagnostic recommendations

---

### Module 5: Reporting and Analysis (25 minutes)

**Objective:** Generate and analyze test reports.

**Steps:**
1. Run a test with CSV logging:
   ```bash
   loopy run --duration 120 --threads 4 --csv-logging --csv-file module5-data.csv
   ```

2. Generate HTML report:
   ```bash
   loopy report --output module5-report.html
   ```

3. Open in browser and review:
   - Performance summary
   - Response time distribution
   - Throughput metrics
   - Recommendations

4. Generate Markdown report:
   ```bash
   loopy report --format markdown --output module5-report.md
   ```

5. Share the Markdown report with your team

**Expected Outcomes:**
- Generate professional reports
- Analyze performance data
- Share results effectively

---

### Module 6: Enterprise Features (30 minutes)

**Objective:** Use scheduling and cluster features.

**Steps:**
1. Schedule a delayed test:
   ```bash
   loopy schedule --delay 30 --config module1-config.properties
   ```

2. Monitor execution in interactive mode

3. Test cluster health (if available):
   ```bash
   loopy cluster --nodes bolt://localhost:7687 --health-check
   ```

4. Store credentials securely:
   ```bash
   loopy security --generate-password
   loopy security --store-credential module6_password
   ```

5. Check audit log:
   ```bash
   loopy security --check-audit-log
   ```

**Expected Outcomes:**
- Schedule automated tests
- Manage cluster connections
- Secure credential storage

---

## 6. Advanced Usage Scenarios

### Scenario 1: Production Performance Baseline

**Goal:** Establish baseline performance metrics for a production database.

**Approach:**
```bash
# Step 1: Setup production config
loopy setup --output prod-baseline.properties

# Step 2: Validate configuration
loopy config validate --config prod-baseline.properties

# Step 3: Test connection with diagnostics
loopy test-connection --full-diagnostics --save-report prod-baseline-pre.txt

# Step 4: Run baseline test (low impact)
loopy run --config prod-baseline.properties --threads 2 --duration 300 \
          --write-ratio 0.3 --csv-logging --csv-file prod-baseline.csv

# Step 5: Generate report
loopy report --input prod-baseline.csv --output prod-baseline-report.html
```

**Key Metrics to Track:**
- Average operations per second
- P95 and P99 response times
- Error rates
- Resource utilization

---

### Scenario 2: Capacity Planning

**Goal:** Determine maximum sustainable load.

**Approach:**
```bash
# Incremental load testing
for threads in 2 4 8 16 32; do
    loopy run --threads $threads --duration 120 \
              --csv-logging --csv-file capacity-${threads}t.csv
    echo "Completed test with $threads threads"
    sleep 30  # Cool-down period
done

# Generate comparative reports
for file in capacity-*.csv; do
    loopy report --input $file --output ${file%.csv}-report.html
done
```

**Analysis:**
- Plot throughput vs threads
- Identify saturation point
- Note when response times degrade

---

### Scenario 3: Scheduled Regression Testing

**Goal:** Automated daily performance regression tests.

**Approach:**
```bash
# Create regression test config
loopy config init --output regression-test.properties

# Edit to match production workload patterns
loopy config edit --config regression-test.properties

# Schedule nightly runs (2 AM)
loopy schedule --cron "0 2 * * *" --config regression-test.properties \
               --daemon

# Monitor via audit log
tail -f ~/.loopy/credentials/audit.log
```

**Monitoring:**
- Compare reports day-over-day
- Alert on performance degradation
- Track trends over time

---

### Scenario 4: Cluster Load Balancing Test

**Goal:** Test and optimize cluster load distribution.

**Approach:**
```bash
# Test different strategies
for strategy in ROUND_ROBIN HEALTH_BASED WEIGHTED RANDOM; do
    echo "Testing $strategy strategy"
    loopy cluster --nodes bolt://node1:7687,bolt://node2:7687,bolt://node3:7687 \
                  --strategy $strategy --status
    
    # Run load test with this strategy
    # (would need cluster-aware run command)
done
```

**Evaluation:**
- Compare node utilization
- Analyze failover behavior
- Optimize strategy selection

---

## 7. Best Practices

### Configuration Management

✅ **Do:**
- Use version control for configuration files
- Create separate configs for dev/staging/prod
- Document custom settings
- Validate configs before using
- Use environment variables for secrets

❌ **Don't:**
- Store passwords in plain text configs
- Use production credentials in dev
- Skip configuration validation
- Ignore validation warnings

### Load Testing

✅ **Do:**
- Start with small loads and increase gradually
- Use appropriate load profiles for environment
- Monitor database during tests
- Run tests during off-peak hours
- Enable CSV logging for analysis
- Cool down between heavy tests

❌ **Don't:**
- Start with maximum load immediately
- Run heavy tests on production without approval
- Ignore error rates
- Skip connection testing
- Run continuous stress tests

### Performance Tuning

✅ **Do:**
- Use auto-tuning as starting point
- Match thread count to use case
- Consider system resources
- Benchmark different configurations
- Document optimal settings

❌ **Don't:**
- Use maximum threads by default
- Ignore system warnings
- Skip performance analysis
- Use same settings for all environments

### Security

✅ **Do:**
- Use credential manager for passwords
- Review audit logs regularly
- Use strong passwords
- Rotate credentials periodically
- Restrict file permissions

❌ **Don't:**
- Store passwords in shell history
- Share credential files
- Ignore security warnings
- Use default passwords
- Disable audit logging

### Reporting

✅ **Do:**
- Generate reports after every test
- Store reports for historical comparison
- Share reports with stakeholders
- Use appropriate format for audience
- Include recommendations in reports

❌ **Don't:**
- Delete test data immediately
- Skip report generation
- Ignore trend analysis
- Report without context

---

## 8. Troubleshooting

### Common Issues and Solutions

#### Issue: "Connection refused"

**Symptoms:**
```
❌ Connection failed: Connection refused
```

**Solutions:**
1. Verify Neo4j is running:
   ```bash
   # Check Neo4j status
   neo4j status
   ```

2. Check URI and port:
   ```bash
   loopy test-connection --neo4j-uri bolt://localhost:7687 --quick
   ```

3. Verify firewall settings
4. Check Neo4j logs for errors

---

#### Issue: "Authentication failed"

**Symptoms:**
```
❌ Authentication failed: Invalid username or password
```

**Solutions:**
1. Verify credentials:
   ```bash
   loopy test-connection --username neo4j --password
   ```

2. Check default password hasn't been changed
3. Use credential manager:
   ```bash
   loopy security --store-credential neo4j_password
   ```

---

#### Issue: "Poor performance / Low throughput"

**Symptoms:**
- Low operations per second
- High response times
- Errors during load test

**Solutions:**
1. Run diagnostics:
   ```bash
   loopy test-connection --full-diagnostics
   ```

2. Check system resources:
   ```bash
   loopy tune
   ```

3. Reduce load:
   ```bash
   loopy run --threads 2 --batch-size 100
   ```

4. Review Neo4j configuration:
   - Memory settings
   - Page cache size
   - Bolt thread pool

5. Check network latency

---

#### Issue: "Compilation errors"

**Symptoms:**
```
[ERROR] COMPILATION ERROR
```

**Solutions:**
1. Clean and rebuild:
   ```bash
   mvn clean compile
   ```

2. Check Java version:
   ```bash
   java -version  # Should be Java 21+
   ```

3. Update dependencies:
   ```bash
   mvn clean install
   ```

---

#### Issue: "Out of memory errors"

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions:**
1. Increase JVM heap:
   ```bash
   java -Xmx4g -jar loopy.jar run
   ```

2. Reduce batch size:
   ```bash
   loopy run --batch-size 50
   ```

3. Reduce thread count:
   ```bash
   loopy run --threads 2
   ```

---

### Getting Help

**Command-line help:**
```bash
# General help
loopy --help

# Command-specific help
loopy run --help
loopy setup --help
loopy config --help
```

**Man pages:**
```bash
man loopy
```

**Diagnostic report:**
```bash
loopy test-connection --full-diagnostics --save-report diagnostic-report.txt
```

**Security check:**
```bash
loopy security --security-test
```

---

## Summary Cheat Sheet

### Quick Commands

```bash
# Setup
loopy setup                              # Interactive configuration
loopy config init                        # Generate default config

# Testing
loopy test-connection --quick            # Quick connection test
loopy run --duration 30 --threads 2      # Quick load test

# Optimization
loopy tune --show-profiles               # View load profiles
loopy tune --auto-tune                   # Get recommendations

# Reporting
loopy report --output results.html       # Generate HTML report

# Enterprise
loopy schedule --delay 60 --config test.properties    # Schedule test
loopy cluster --nodes node1,node2 --health-check     # Check cluster
loopy security --generate-password                   # Generate password
```

### Configuration Priority

1. Command-line options (highest priority)
2. Environment variables
3. Configuration file
4. Default values (lowest priority)

### Performance Guidelines

| Environment | Threads | Duration | Write Ratio | Batch Size |
|-------------|---------|----------|-------------|------------|
| Development | 2 | 30s | 0.3 | 100 |
| Staging | 4 | 120s | 0.5 | 500 |
| Production | 8-16 | 300s+ | 0.7 | 1000 |

---

## Appendix A: Configuration File Reference

### Complete Example Configuration

```properties
# Neo4j Connection
neo4j.uri=bolt://localhost:7687
neo4j.username=neo4j
neo4j.password=password

# Load Testing Parameters
threads=4
duration.seconds=300
write.ratio=0.7
batch.size=100

# Data Configuration
node.labels=Person,Company
relationship.types=WORKS_FOR,KNOWS
property.size.bytes=1024

# Reporting
report.interval.seconds=10
csv.logging.enabled=true
csv.logging.file=loopy-results.csv
```

---

## Appendix B: Load Profile Reference

### Detailed Profile Specifications

**Light Profile:**
```properties
threads=2
batch.size=100
duration.seconds=30
write.ratio=0.3
# Use for: Development, smoke testing
```

**Medium Profile:**
```properties
threads=4
batch.size=500
duration.seconds=120
write.ratio=0.5
# Use for: Staging, integration testing
```

**Heavy Profile:**
```properties
threads=8
batch.size=1000
duration.seconds=300
write.ratio=0.7
# Use for: Production testing, capacity planning
```

**Stress Profile:**
```properties
threads=16
batch.size=2000
duration.seconds=600
write.ratio=0.8
# Use for: Stress testing, breaking point analysis
```

---

## Appendix C: Environment Variables

| Variable | Purpose | Example |
|----------|---------|---------|
| `LOOPY_NEO4J_URI` | Default Neo4j URI | `export LOOPY_NEO4J_URI=bolt://prod:7687` |
| `LOOPY_USERNAME` | Default username | `export LOOPY_USERNAME=admin` |
| `LOOPY_PASSWORD` | Default password | `export LOOPY_PASSWORD=secret` |
| `LOOPY_THREADS` | Default threads | `export LOOPY_THREADS=8` |
| `LOOPY_DURATION` | Default duration | `export LOOPY_DURATION=120` |
| `LOOPY_CONFIG_FILE` | Default config | `export LOOPY_CONFIG_FILE=prod.properties` |

---

**End of Training Guide**

For additional support, run `loopy --help` or `man loopy`.