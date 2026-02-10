# Cluster Command Removal Plan

## Executive Summary
Remove the `cluster` command and integrate cluster connectivity testing into `test-connection`. The enhanced `test-connection` command will automatically detect cluster URIs and test all cluster members without complex load balancing strategies.

**This is a breaking change requiring a major version bump from 1.0.0 to 2.0.0.**

---

## Rationale

### Why Remove `cluster` Command?
1. **Feature Overlap**: Both commands test connectivity, creating confusion
2. **Over-Engineering**: Load balancing strategies (ROUND_ROBIN, HEALTH_BASED, etc.) add unnecessary complexity for a testing tool
3. **Simplification**: Neo4j Driver handles cluster routing natively - we don't need custom load balancing
4. **Single Responsibility**: `test-connection` should handle ALL connection testing scenarios
5. **User Experience**: One command is easier to learn and use than two

### Benefits
- Reduced codebase complexity
- Eliminated confusion between two similar commands
- Simpler CLI interface
- Easier maintenance
- Leverages Neo4j Driver's built-in cluster support

---

## Version Change: 1.0.0 → 2.0.0

**Breaking Change:** The removal of the `cluster` command and `ClusterSupport` class constitutes a breaking change requiring a major version increment per semantic versioning.

### Files to Update:
1. **pom.xml** - Update `<version>1.0.0</version>` to `<version>2.0.0</version>`
2. **LoopyApplication.java** - Update `version = "1.0.0"` to `version = "2.0.0"`

---

## Technical Changes

### Phase 1: Remove Cluster Command Infrastructure

#### 1.1 Delete Files
```
src/main/java/com/neo4j/loopy/commands/ClusterCommand.java
src/main/java/com/neo4j/loopy/enterprise/ClusterSupport.java
```

#### 1.2 Update LoopyApplication.java
**File**: `src/main/java/com/neo4j/loopy/LoopyApplication.java`

Remove from subcommands list:
```java
// REMOVE THIS LINE:
com.neo4j.loopy.commands.ClusterCommand.class,
```

Also remove import if present:
```java
// REMOVE IF PRESENT:
import com.neo4j.loopy.commands.ClusterCommand;
```

---

### Phase 2: Enhance TestConnectionCommand

#### 2.1 Add Cluster Detection
**File**: `src/main/java/com/neo4j/loopy/commands/TestConnectionCommand.java`

Add new method to detect cluster URIs:
```java
/**
 * Detect if the URI indicates a cluster connection
 * neo4j:// and neo4j+s:// schemes indicate routing/cluster mode
 * bolt:// and bolt+s:// schemes are direct connections
 */
private boolean isClusterUri(String uri) {
    return uri.startsWith("neo4j://") || uri.startsWith("neo4j+s://");
}
```

#### 2.2 Add New Option for Multiple Nodes
Add option to accept multiple URIs:
```java
@Option(names = {"--nodes"}, 
        description = "Comma-separated list of cluster node URIs to test (alternative to --neo4j-uri)",
        split = ",")
private String[] nodeUris;
```

#### 2.3 Enhance Basic Test Method
Update `runBasicTest()` to:
1. Check if URI is a cluster URI using `isClusterUri()`
2. If cluster URI detected, test cluster members
3. If `--nodes` specified, test each node individually

```java
private Integer runBasicTest() {
    // Detect cluster vs single node
    boolean isCluster = false;
    List<String> nodesToTest = new ArrayList<>();
    
    if (nodeUris != null && nodeUris.length > 0) {
        // User explicitly provided multiple nodes
        nodesToTest.addAll(Arrays.asList(nodeUris));
        isCluster = true;
        System.out.println("\u001B[36mTesting Neo4j cluster with " + nodesToTest.size() + " nodes...\u001B[0m");
    } else if (isClusterUri(neo4jUri)) {
        // Single cluster URI - Driver will route to cluster
        nodesToTest.add(neo4jUri);
        isCluster = true;
        System.out.println("\u001B[36mTesting Neo4j cluster connection...\u001B[0m");
    } else {
        // Single direct connection
        nodesToTest.add(neo4jUri);
        System.out.println("\u001B[36mTesting Neo4j connection...\u001B[0m");
    }
    
    // Test each node/URI
    int successCount = 0;
    int failCount = 0;
    
    for (String uri : nodesToTest) {
        if (nodesToTest.size() > 1) {
            System.out.println("\n--- Testing node: " + uri + " ---");
        } else {
            System.out.println("URI: " + uri);
        }
        System.out.println("Username: " + username);
        
        if (testSingleNode(uri)) {
            successCount++;
        } else {
            failCount++;
        }
    }
    
    // Summary for cluster tests
    if (isCluster && nodesToTest.size() > 1) {
        System.out.println("\n\u001B[36m=== Cluster Test Summary ===\u001B[0m");
        System.out.println("  Tested nodes: " + nodesToTest.size());
        System.out.println("  Successful: " + successCount);
        System.out.println("  Failed: " + failCount);
        
        if (failCount == 0) {
            System.out.println("\n\u001B[32m✓ All cluster nodes passed! Ready for load testing.\u001B[0m");
            return 0;
        } else if (successCount > 0) {
            System.out.println("\n\u001B[33m⚠ Some nodes failed but cluster may still be operational\u001B[0m");
            return 1;
        } else {
            System.out.println("\n\u001B[31m✗ All nodes failed - cluster is not accessible\u001B[0m");
            return 1;
        }
    }
    
    return (failCount == 0) ? 0 : 1;
}
```

#### 2.4 Extract Single Node Test Logic
Create new private method `testSingleNode()` by extracting current test logic:
```java
private boolean testSingleNode(String uri) {
    try (Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password))) {
        
        // Test basic connectivity
        System.out.print("  • Testing basic connectivity... ");
        driver.verifyConnectivity();
        System.out.println("\u001B[32m✓\u001B[0m");
        
        try (Session session = driver.session()) {
            
            // Test database version
            System.out.print("  • Checking database version... ");
            Result result = session.run("CALL dbms.components() YIELD name, versions, edition");
            if (result.hasNext()) {
                var record = result.next();
                String name = record.get("name").asString();
                String version = record.get("versions").asList().get(0).toString();
                String edition = record.get("edition").asString();
                System.out.println("\u001B[32m✓\u001B[0m");
                System.out.println("    " + name + " " + version + " (" + edition + ")");
            } else {
                System.out.println("\u001B[33m?\u001B[0m (version info not available)");
            }
            
            // Test read permissions
            System.out.print("  • Testing read permissions... ");
            session.run("MATCH (n) RETURN count(n) AS nodeCount LIMIT 1");
            System.out.println("\u001B[32m✓\u001B[0m");
            
            // Test write permissions
            System.out.print("  • Testing write permissions... ");
            session.run("CREATE (test:LoopyTestNode {timestamp: timestamp()}) DELETE test");
            System.out.println("\u001B[32m✓\u001B[0m");
            
            // Test relationship creation
            System.out.print("  • Testing relationship creation... ");
            session.run("CREATE (a:LoopyTestNode)-[r:TEST_REL]->(b:LoopyTestNode) DELETE a, r, b");
            System.out.println("\u001B[32m✓\u001B[0m");
            
            System.out.println("\n\u001B[32m✓ All tests passed!\u001B[0m");
            
            return true;
        }
        
    } catch (Exception e) {
        System.out.println("\u001B[31m✗\u001B[0m");
        System.err.println("\u001B[31mConnection test failed: " + e.getMessage() + "\u001B[0m");
        return false;
    }
}
```

#### 2.5 Add Cluster Member Discovery
Add method to discover cluster members from routing table (optional enhancement):
```java
/**
 * Attempt to discover cluster members from routing table
 * Only works with neo4j:// URIs
 */
private List<String> discoverClusterMembers(String clusterUri) {
    List<String> members = new ArrayList<>();
    
    try (Driver driver = GraphDatabase.driver(clusterUri, AuthTokens.basic(username, password))) {
        driver.verifyConnectivity();
        
        try (Session session = driver.session()) {
            // Query routing table
            Result result = session.run("CALL dbms.routing.getRoutingTable({}, 'system')");
            
            while (result.hasNext()) {
                var record = result.next();
                List<Object> servers = record.get("servers").asList();
                
                for (Object server : servers) {
                    // Extract addresses from routing table
                    // This is a simplified version - actual implementation may vary
                    members.add(server.toString());
                }
            }
        }
    } catch (Exception e) {
        // If discovery fails, just use the original URI
        System.out.println("  ℹ Could not discover cluster members: " + e.getMessage());
    }
    
    return members;
}
```

#### 2.6 Update Help Text
Update command description:
```java
@Command(name = "test-connection", 
         description = "Test Neo4j database connectivity (single node or cluster) with comprehensive diagnostics",
         mixinStandardHelpOptions = true)
```

Add imports:
```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
```

---

### Phase 3: Update Documentation

#### 3.1 Update TRAINING_GUIDE.md

**Section 4.4** - Enhance `test-connection` section:

```markdown
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

**Examples:**
```bash
# Quick connectivity test (single node)
loopy test-connection --quick

# Test cluster with routing URI
loopy test-connection --neo4j-uri neo4j://cluster.example.com:7687

# Test specific cluster nodes individually
loopy test-connection --nodes bolt://node1:7687,bolt://node2:7687,bolt://node3:7687

# Full diagnostics on cluster
loopy test-connection --neo4j-uri neo4j://cluster:7687 --full-diagnostics
```

**URI Schemes:**
- `bolt://` - Direct connection to single instance
- `bolt+s://` - Direct connection with TLS
- `neo4j://` - Cluster routing connection
- `neo4j+s://` - Cluster routing with TLS

**Test Levels:**
[... rest remains the same ...]
```

**Section 4.10** - Remove cluster section entirely

#### 3.2 Update README.md

Search for cluster command references and replace with enhanced test-connection examples.

#### 3.3 Update scripts/loopy-completion.bash and loopy-completion.zsh

Remove `cluster` from command completions:
```bash
# REMOVE:
cluster)
    opts="--nodes --username --password --strategy --health-check --status --test-connections"
    ;;
```

#### 3.4 Update scripts/loopy.1 (man page)

Remove cluster command section and update test-connection section with new options.

---

### Phase 4: Testing & Validation

#### 4.1 Test Single Node Connections
```bash
# Direct connection
loopy test-connection --neo4j-uri bolt://localhost:7687 --quick

# Basic test
loopy test-connection --neo4j-uri bolt://localhost:7687

# Full diagnostics
loopy test-connection --neo4j-uri bolt://localhost:7687 --full-diagnostics
```

#### 4.2 Test Cluster URI Detection
```bash
# Should automatically detect cluster and test routing
loopy test-connection --neo4j-uri neo4j://localhost:7687

# Should test cluster with TLS
loopy test-connection --neo4j-uri neo4j+s://cluster.example.com:7687
```

#### 4.3 Test Multiple Nodes Explicitly
```bash
# Test multiple specific nodes
loopy test-connection --nodes bolt://node1:7687,bolt://node2:7687,bolt://node3:7687

# Verify failure handling with mixed success/failure
loopy test-connection --nodes bolt://localhost:7687,bolt://invalid:7687
```

---

## Migration Guide for Users

### For Users of `loopy cluster`

**Before:**
```bash
loopy cluster --nodes bolt://node1:7687,bolt://node2:7687 --test-connections
```

**After:**
```bash
loopy test-connection --nodes bolt://node1:7687,bolt://node2:7687
```

**Before:**
```bash
loopy cluster --nodes bolt://node1:7687,bolt://node2:7687 --health-check
```

**After:**
```bash
loopy test-connection --nodes bolt://node1:7687,bolt://node2:7687
```

**Before (with cluster URI):**
```bash
loopy cluster --nodes neo4j://cluster:7687 --status
```

**After:**
```bash
loopy test-connection --neo4j-uri neo4j://cluster:7687
```

### Load Balancing Strategy Removal

**What's Removed:**
- `--strategy` option (ROUND_ROBIN, HEALTH_BASED, RANDOM, WEIGHTED)
- Custom load balancing logic in ClusterSupport

**Why:**
Neo4j Driver handles routing and load balancing automatically when using `neo4j://` URIs. The custom load balancing was:
- Redundant (driver does this better)
- Complex to maintain
- Not necessary for a testing tool

**Impact:**
Users who relied on custom load balancing strategies should use Neo4j's built-in cluster routing instead.

---

## Implementation Checklist

### Phase 0: Version Update
- [ ] Update version in `pom.xml` from `1.0.0` to `2.0.0`
- [ ] Update version in `LoopyApplication.java` from `"1.0.0"` to `"2.0.0"`

### Phase 1: Code Removal
- [ ] Delete `ClusterCommand.java`
- [ ] Delete `ClusterSupport.java`
- [ ] Remove cluster imports from `LoopyApplication.java`
- [ ] Remove cluster command from subcommands list
- [ ] Verify no other files reference ClusterCommand or ClusterSupport

### Phase 2: TestConnectionCommand Enhancement
- [ ] Add `isClusterUri()` method
- [ ] Add `--nodes` option
- [ ] Refactor `runBasicTest()` with cluster detection
- [ ] Extract `testSingleNode()` method
- [ ] Add cluster summary reporting
- [ ] Add necessary imports (ArrayList, Arrays, List)
- [ ] Update command description
- [ ] Test with various URI schemes

### Phase 3: Documentation Updates
- [ ] Update TRAINING_GUIDE.md section 4.4
- [ ] Remove TRAINING_GUIDE.md section 4.10 (cluster)
- [ ] Update README.md
- [ ] Update loopy-completion.bash
- [ ] Update loopy-completion.zsh
- [ ] Update loopy.1 man page
- [ ] Search for any other cluster command references

### Phase 4: Testing
- [ ] Test single node connections (bolt://)
- [ ] Test cluster URI detection (neo4j://)
- [ ] Test explicit multiple nodes (--nodes)
- [ ] Test TLS schemes (bolt+s://, neo4j+s://)
- [ ] Test failure scenarios
- [ ] Test all test modes (quick, basic, full-diagnostics)
- [ ] Verify version displays as 2.0.0 with `loopy --version`

### Phase 5: Final Validation
- [ ] Build project successfully with `mvn clean package`
- [ ] Verify built JAR reflects version 2.0.0
- [ ] Run all existing tests
- [ ] Manual testing of all scenarios
- [ ] Create/update CHANGELOG.md with version 2.0.0 breaking changes
- [ ] Document migration path for users
- [ ] Tag release as v2.0.0 in git

---

## Risks & Mitigation

### Risk 1: Breaking Change for Existing Users
**Impact:** Users with scripts using `loopy cluster` will break immediately upon upgrading to v2.0.0

**Mitigation:**
- Major version bump (2.0.0) signals breaking changes per semantic versioning
- Clear migration guide in documentation
- Update CHANGELOG.md with prominent breaking change notice
- Provide simple 1:1 command mapping
- Release notes clearly state this is a breaking change

### Risk 2: Loss of Load Balancing Strategies
**Impact:** Users relying on custom load balancing strategies (`--strategy` option) lose this functionality

**Mitigation:**
- Neo4j Driver provides built-in routing and load balancing
- Document that driver handles this automatically and more efficiently
- For advanced users, suggest using driver configuration or connection strings directly
- Most users never used custom strategies (enterprise feature with low adoption)

### Risk 3: Incomplete Cluster Member Discovery
**Impact:** Automatic cluster member discovery may not work in all network configurations

**Mitigation:**
- Provide explicit `--nodes` option for manual specification
- Clear error messages if discovery fails
- Fallback to testing only the provided URI
- Document both automatic and explicit testing approaches

---

## Timeline Estimate

- **Phase 1 (Removal):** 1 hour
- **Phase 2 (Enhancement):** 3-4 hours
- **Phase 3 (Documentation):** 2 hours
- **Phase 4 (Testing):** 2-3 hours
- **Total:** 8-10 hours

---

## Questions to Resolve

1. Do we want to implement cluster member discovery, or just test the provided URIs?
2. Should `--full-diagnostics` provide additional cluster-specific metrics?
3. Should we support fallback testing (try cluster members one by one if routing fails)?
4. Should we add cluster topology information to the output?
5. Do we want to test cluster role detection (leader/follower)?

---

## CHANGELOG Entry

Add the following to CHANGELOG.md:

```markdown
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

### Migration Guide
- Replace `loopy cluster --nodes X,Y,Z --test-connections` with `loopy test-connection --nodes X,Y,Z`
- Replace `loopy cluster --nodes neo4j://cluster:7687 --status` with `loopy test-connection --neo4j-uri neo4j://cluster:7687`
- Remove any usage of `--strategy` option - Neo4j Driver handles routing automatically
```

---

## Success Criteria

- [ ] Version updated to 2.0.0 in both pom.xml and LoopyApplication.java
- [ ] `loopy cluster` command no longer exists
- [ ] `loopy test-connection` handles single and cluster connections
- [ ] All documentation updated
- [ ] No broken imports or references
- [ ] All tests pass
- [ ] CHANGELOG.md contains breaking change notice
- [ ] Clear migration path documented
- [ ] Simplified codebase with less complexity
- [ ] Git tag v2.0.0 created
