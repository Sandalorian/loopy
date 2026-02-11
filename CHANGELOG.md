# Changelog

All notable changes to Loopy will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-02-11

### Changed
- **Simplified distribution structure** - Wrapper scripts now at distribution root
  - No more `bin/` subdirectory
  - Run directly: `./loopy --help` or add distribution folder to PATH
- Cleaner user experience for direct execution

### Notes
- PATH should now point to the distribution directory itself (e.g., `/path/to/loopy-0.3.0`)

## [0.2.0] - 2026-02-11

### Added
- **Cross-platform wrapper scripts** - Run `loopy` directly without `java -jar` prefix
  - `loopy` - Unix shell script for macOS/Linux
  - `loopy.bat` - Windows CMD batch script
  - `loopy.ps1` - Windows PowerShell script
- **Automatic Java detection** with version validation (requires Java 21+)
- **Environment variable support** - `JAVA_HOME`, `LOOPY_HOME`, `LOOPY_JAVA_OPTS`
- **Comprehensive installation guide** - [INSTALL.md](docs/INSTALL.md)
- **Multi-platform CI testing** - Wrapper scripts tested on Ubuntu, macOS, and Windows

### Changed
- Distribution structure reorganized:
  - JAR moved to `lib/` directory
  - Wrapper scripts at distribution root
  - Documentation references updated to use `loopy` command

### Notes
- JAR filename retains version string for traceability
- Wrapper scripts dynamically locate versioned JAR

## [0.1.0] - 2026-02-11

### Initial Development Release

This is the first public development release of Loopy. The API and features are subject to change as the project evolves.

### Features
- YAML-based Cypher workload support with weighted query selection
- Programmatic data generation mode with configurable node labels and relationship types
- Parameter generators: UUID, integer, double, string, long, boolean
- Commands: `run`, `validate`, `benchmark`, `test-connection`, `setup`, `config`, `report`, `security`
- Cluster-aware connection testing (automatic detection of `neo4j://` scheme)
- Shell completion for bash and zsh
- Man page documentation
- CSV logging and JSON statistics output
- Real-time performance metrics during execution

### Notes
- This is a pre-1.0 release; breaking changes may occur between minor versions
- Feedback and contributions welcome
