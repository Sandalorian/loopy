# Changelog

All notable changes to Loopy will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-02-10

### BREAKING CHANGES
- **Removed `cluster` command entirely** - Cluster testing is now integrated into `test-connection`
- **Removed `ClusterSupport` class** - Custom load balancing strategies no longer available
- **Removed load balancing strategies** - ROUND_ROBIN, HEALTH_BASED, RANDOM, WEIGHTED options removed

### Added
- `test-connection` now automatically detects cluster URIs (`neo4j://` scheme)
- `test-connection --nodes` option to test multiple cluster nodes explicitly
- Automatic cluster member testing with summary reporting
- Better integration with Neo4j Driver's built-in cluster routing

### Changed
- `test-connection` command description updated to mention cluster support
- Simplified CLI interface by consolidating connection testing into single command
- Updated shell completion scripts with new `--nodes` option
- Updated man page documentation

### Migration Guide
- Replace `loopy cluster --nodes X,Y,Z --test-connections` with `loopy test-connection --nodes X,Y,Z`
- Replace `loopy cluster --nodes neo4j://cluster:7687 --status` with `loopy test-connection --neo4j-uri neo4j://cluster:7687`
- Remove any usage of `--strategy` option - Neo4j Driver handles routing automatically

## [1.0.0] - 2026-01-15

### Added
- Initial release of Loopy load generator
- YAML-based Cypher workload support
- Programmatic data generation mode
- Commands: run, validate, benchmark, test-connection, setup, config, tune, report, schedule, cluster, security
- Shell completion for bash and zsh
- Man page documentation
- CSV logging and reporting
- Multiple load profiles (light, medium, heavy, stress)
