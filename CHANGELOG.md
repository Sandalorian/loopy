# Changelog

All notable changes to Loopy will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
