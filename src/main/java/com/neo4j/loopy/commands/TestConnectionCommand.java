package com.neo4j.loopy.commands;

import com.neo4j.loopy.LoopyConfig;
import com.neo4j.loopy.diagnostics.Neo4jDiagnostics;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Test connection command - Test Neo4j connectivity with comprehensive diagnostics
 */
@Command(name = "test-connection", 
         description = "Test Neo4j database connectivity and run comprehensive diagnostics",
         mixinStandardHelpOptions = true)
public class TestConnectionCommand implements Callable<Integer> {
    
    @Option(names = {"--neo4j-uri", "-u"}, 
            description = "Neo4j connection URI",
            defaultValue = "${LOOPY_NEO4J_URI:-bolt://localhost:7687}")
    private String neo4jUri;
    
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
    
    private Integer runQuickTest() {
        System.out.println("\u001B[36mQuick Neo4j connectivity test...\u001B[0m");
        
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
        System.out.println("\u001B[36mTesting Neo4j connection...\u001B[0m");
        System.out.println("URI: " + neo4jUri);
        System.out.println("Username: " + username);
        
        try (Driver driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(username, password))) {
            
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
                
                System.out.println("\n\u001B[32m✓ All basic tests passed! Ready for load testing.\u001B[0m");
                System.out.println("\n\u001B[36mTip:\u001B[0m Run with --full-diagnostics for comprehensive analysis");
                
                return 0;
                
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
            
            return 1;
        }
    }
    
    private Integer runFullDiagnostics() {
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