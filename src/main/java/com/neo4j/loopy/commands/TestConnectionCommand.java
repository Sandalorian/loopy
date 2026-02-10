package com.neo4j.loopy.commands;

import com.neo4j.loopy.diagnostics.Neo4jDiagnostics;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Test connection command - Test Neo4j connectivity (single node or cluster) with comprehensive diagnostics
 */
@Command(name = "test-connection", 
         description = "Test Neo4j database connectivity (single node or cluster) with comprehensive diagnostics",
         mixinStandardHelpOptions = true)
public class TestConnectionCommand implements Callable<Integer> {
    
    @Option(names = {"--neo4j-uri", "-u"}, 
            description = "Neo4j connection URI (supports bolt://, neo4j://, bolt+s://, neo4j+s://)",
            defaultValue = "${LOOPY_NEO4J_URI:-bolt://localhost:7687}")
    private String neo4jUri;
    
    @Option(names = {"--nodes"}, 
            description = "Comma-separated list of cluster node URIs to test individually",
            split = ",")
    private String[] nodeUris;
    
    @Option(names = {"--username", "-U"}, 
            description = "Neo4j username",
            defaultValue = "${LOOPY_USERNAME:-neo4j}")
    private String username;
    
    @Option(names = {"--password", "-P"}, 
            description = "Neo4j password", 
            interactive = true,
            defaultValue = "${LOOPY_PASSWORD:-password}")
    private String password;
    
    @Option(names = {"--full-diagnostics", "--diag"}, 
            description = "Run comprehensive diagnostics (default: basic test only)")
    private boolean fullDiagnostics;
    
    @Option(names = {"--save-report"}, 
            description = "Save diagnostic report to file")
    private String reportFile;
    
    @Option(names = {"--quick"}, 
            description = "Quick connectivity test only")
    private boolean quickTest;
    
    @Override
    public Integer call() throws Exception {
        
        if (quickTest) {
            return runQuickTest();
        } else if (fullDiagnostics) {
            return runFullDiagnostics();
        } else {
            return runBasicTest();
        }
    }
    
    /**
     * Detect if the URI indicates a cluster connection.
     * neo4j:// and neo4j+s:// schemes indicate routing/cluster mode.
     * bolt:// and bolt+s:// schemes are direct connections.
     */
    private boolean isClusterUri(String uri) {
        return uri != null && (uri.startsWith("neo4j://") || uri.startsWith("neo4j+s://"));
    }
    
    private Integer runQuickTest() {
        // Handle multiple nodes
        if (nodeUris != null && nodeUris.length > 0) {
            System.out.println("\u001B[36mQuick Neo4j cluster connectivity test (" + nodeUris.length + " nodes)...\u001B[0m");
            int successCount = 0;
            int failCount = 0;
            
            for (String uri : nodeUris) {
                System.out.print("  • " + uri + " ... ");
                try (Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password))) {
                    driver.verifyConnectivity();
                    System.out.println("\u001B[32m✓\u001B[0m");
                    successCount++;
                } catch (Exception e) {
                    System.out.println("\u001B[31m✗\u001B[0m (" + e.getMessage() + ")");
                    failCount++;
                }
            }
            
            System.out.println();
            if (failCount == 0) {
                System.out.println("\u001B[32m✓ All " + successCount + " nodes connected successfully!\u001B[0m");
                return 0;
            } else {
                System.out.println("\u001B[33m⚠ " + successCount + " passed, " + failCount + " failed\u001B[0m");
                return 1;
            }
        }
        
        // Single node quick test
        System.out.println("\u001B[36mQuick Neo4j connectivity test...\u001B[0m");
        if (isClusterUri(neo4jUri)) {
            System.out.println("  (Cluster routing URI detected)");
        }
        
        try (Driver driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(username, password))) {
            driver.verifyConnectivity();
            System.out.println("\u001B[32m✓ Connection successful!\u001B[0m");
            return 0;
        } catch (Exception e) {
            System.err.println("\u001B[31m✗ Connection failed: " + e.getMessage() + "\u001B[0m");
            return 1;
        }
    }
    
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
            System.out.println("  (Cluster routing URI detected: neo4j:// scheme)");
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
        
        // Single node result
        if (failCount == 0) {
            System.out.println("\n\u001B[36mTip:\u001B[0m Run with --full-diagnostics for comprehensive analysis");
        }
        
        return (failCount == 0) ? 0 : 1;
    }
    
    /**
     * Test a single Neo4j node with full connectivity and permission checks.
     * 
     * @param uri The Neo4j URI to test
     * @return true if all tests passed, false otherwise
     */
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
            
            // Provide helpful suggestions
            System.err.println("\nTroubleshooting suggestions:");
            System.err.println("  • Verify Neo4j is running and accessible");
            System.err.println("  • Check the URI format (bolt://host:port or neo4j://host:port)");
            System.err.println("  • Verify username and password are correct");
            System.err.println("  • Check network connectivity and firewall settings");
            
            return false;
        }
    }
    
    private Integer runFullDiagnostics() {
        // Handle multiple nodes with diagnostics
        if (nodeUris != null && nodeUris.length > 0) {
            System.out.println("\u001B[36mRunning full diagnostics on " + nodeUris.length + " cluster nodes...\u001B[0m\n");
            
            int successCount = 0;
            int failCount = 0;
            
            for (int i = 0; i < nodeUris.length; i++) {
                String uri = nodeUris[i];
                System.out.println("\u001B[36m=== Node " + (i + 1) + ": " + uri + " ===\u001B[0m");
                
                Neo4jDiagnostics diagnostics = new Neo4jDiagnostics(uri, username, password);
                Neo4jDiagnostics.DiagnosticReport report = diagnostics.runDiagnostics();
                
                // Save individual report if requested
                if (reportFile != null) {
                    String nodeReportFile = reportFile.replace(".", "-node" + (i + 1) + ".");
                    if (diagnostics.saveReport(report, nodeReportFile)) {
                        System.out.println("\n\u001B[32m✓ Diagnostic report saved to: " + nodeReportFile + "\u001B[0m");
                    }
                }
                
                if (report.errors.isEmpty()) {
                    successCount++;
                } else {
                    failCount++;
                }
                
                System.out.println();
            }
            
            // Cluster summary
            System.out.println("\u001B[36m=== Cluster Diagnostic Summary ===\u001B[0m");
            System.out.println("  Total nodes: " + nodeUris.length);
            System.out.println("  Passed: " + successCount);
            System.out.println("  Failed: " + failCount);
            
            if (failCount == 0) {
                System.out.println("\n\u001B[32m✓ All cluster nodes passed diagnostics!\u001B[0m");
                return 0;
            } else {
                System.out.println("\n\u001B[33m⚠ Some nodes have issues - review individual reports above\u001B[0m");
                return 1;
            }
        }
        
        // Single node diagnostics
        if (isClusterUri(neo4jUri)) {
            System.out.println("\u001B[36mRunning full diagnostics on cluster connection...\u001B[0m");
            System.out.println("  (Cluster routing URI detected: neo4j:// scheme)\n");
        }
        
        Neo4jDiagnostics diagnostics = new Neo4jDiagnostics(neo4jUri, username, password);
        Neo4jDiagnostics.DiagnosticReport report = diagnostics.runDiagnostics();
        
        // Save report if requested
        if (reportFile != null) {
            if (diagnostics.saveReport(report, reportFile)) {
                System.out.println("\n\u001B[32m✓ Diagnostic report saved to: " + reportFile + "\u001B[0m");
            } else {
                System.err.println("\n\u001B[31m✗ Failed to save diagnostic report\u001B[0m");
            }
        }
        
        // Print summary
        System.out.println("\n\u001B[36m=== Diagnostic Summary ===\u001B[0m");
        
        if (report.errors.isEmpty()) {
            System.out.println("\u001B[32m✓ All diagnostics passed successfully!\u001B[0m");
            
            if (!report.recommendations.isEmpty()) {
                System.out.println("\n\u001B[33mOptimization recommendations:\u001B[0m");
                for (String rec : report.recommendations) {
                    System.out.println("  • " + rec);
                }
            }
            
            return 0;
        } else {
            System.err.println("\u001B[31m✗ Diagnostic issues found:\u001B[0m");
            for (String error : report.errors) {
                System.err.println("  • " + error);
            }
            return 1;
        }
    }
}