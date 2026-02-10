package com.neo4j.loopy.diagnostics;

import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.Neo4jException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive Neo4j connection diagnostics and testing
 */
public class Neo4jDiagnostics {
    
    private final String uri;
    private final String username;
    private final String password;
    
    public Neo4jDiagnostics(String uri, String username, String password) {
        this.uri = uri;
        this.username = username;
        this.password = password;
    }
    
    /**
     * Run comprehensive diagnostics
     */
    public DiagnosticReport runDiagnostics() {
        DiagnosticReport report = new DiagnosticReport();
        report.startTime = LocalDateTime.now();
        
        System.out.println("\u001B[36m╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║            Neo4j Comprehensive Diagnostics Report           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\u001B[0m\n");
        
        try (Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password))) {
            
            // Basic connectivity
            testBasicConnectivity(driver, report);
            
            // Database information
            getDatabaseInfo(driver, report);
            
            // Performance metrics
            testPerformanceMetrics(driver, report);
            
            // Security and permissions
            testPermissions(driver, report);
            
            // System resources
            testSystemResources(driver, report);
            
            // Recommendations
            generateRecommendations(report);
            
        } catch (Exception e) {
            report.addError("Failed to connect to Neo4j: " + e.getMessage());
        }
        
        report.endTime = LocalDateTime.now();
        return report;
    }
    
    private void testBasicConnectivity(Driver driver, DiagnosticReport report) {
        System.out.println("\u001B[33m=== Basic Connectivity ===\u001B[0m");
        
        try {
            System.out.print("  • Driver connectivity... ");
            long startTime = System.currentTimeMillis();
            driver.verifyConnectivity();
            long connectTime = System.currentTimeMillis() - startTime;
            
            System.out.println("\u001B[32m✓\u001B[0m (" + connectTime + "ms)");
            report.addSuccess("Driver connectivity successful (" + connectTime + "ms)");
            report.connectionLatency = connectTime;
            
        } catch (Exception e) {
            System.out.println("\u001B[31m✗\u001B[0m");
            report.addError("Driver connectivity failed: " + e.getMessage());
        }
    }
    
    private void getDatabaseInfo(Driver driver, DiagnosticReport report) {
        System.out.println("\n\u001B[33m=== Database Information ===\u001B[0m");
        
        try (Session session = driver.session()) {
            
            // Database version and edition
            System.out.print("  • Database version... ");
            Result result = session.run("CALL dbms.components() YIELD name, versions, edition");
            if (result.hasNext()) {
                var record = result.next();
                String name = record.get("name").asString();
                String version = record.get("versions").asList().get(0).toString();
                String edition = record.get("edition").asString();
                
                System.out.println("\u001B[32m✓\u001B[0m");
                System.out.println("    " + name + " " + version + " (" + edition + ")");
                
                report.databaseName = name;
                report.databaseVersion = version;
                report.databaseEdition = edition;
                report.addSuccess("Database: " + name + " " + version + " (" + edition + ")");
            }
            
            // Database name and size
            System.out.print("  • Database details... ");
            try {
                Result dbResult = session.run("CALL db.info()");
                if (dbResult.hasNext()) {
                    var dbRecord = dbResult.next();
                    String dbName = dbRecord.get("name").asString();
                    report.currentDatabase = dbName;
                    System.out.println("\u001B[32m✓\u001B[0m");
                    System.out.println("    Current database: " + dbName);
                }
            } catch (ClientException e) {
                // Try alternative for older versions
                Result dbResult = session.run("CALL db.schema.nodeTypeProperties()");
                System.out.println("\u001B[32m✓\u001B[0m (limited info available)");
            }
            
            // Node and relationship counts
            System.out.print("  • Data statistics... ");
            Result nodeCount = session.run("MATCH (n) RETURN count(n) AS nodeCount");
            Result relCount = session.run("MATCH ()-[r]->() RETURN count(r) AS relCount");
            
            if (nodeCount.hasNext() && relCount.hasNext()) {
                long nodes = nodeCount.next().get("nodeCount").asLong();
                long rels = relCount.next().get("relCount").asLong();
                
                System.out.println("\u001B[32m✓\u001B[0m");
                System.out.println("    Nodes: " + String.format("%,d", nodes));
                System.out.println("    Relationships: " + String.format("%,d", rels));
                
                report.nodeCount = nodes;
                report.relationshipCount = rels;
            }
            
        } catch (Exception e) {
            System.out.println("\u001B[31m✗\u001B[0m");
            report.addError("Failed to get database info: " + e.getMessage());
        }
    }
    
    private void testPerformanceMetrics(Driver driver, DiagnosticReport report) {
        System.out.println("\n\u001B[33m=== Performance Metrics ===\u001B[0m");
        
        try (Session session = driver.session()) {
            
            // Test simple query latency
            System.out.print("  • Simple query latency... ");
            long startTime = System.nanoTime();
            session.run("RETURN 1").consume();
            long queryLatency = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            
            System.out.println("\u001B[32m✓\u001B[0m (" + queryLatency + "ms)");
            report.simpleQueryLatency = queryLatency;
            
            // Test write performance
            System.out.print("  • Write performance... ");
            startTime = System.nanoTime();
            session.run("CREATE (test:LoopyDiagnostic {timestamp: timestamp(), id: randomUUID()}) RETURN test.id AS id");
            long writeLatency = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            
            System.out.println("\u001B[32m✓\u001B[0m (" + writeLatency + "ms)");
            report.writeLatency = writeLatency;
            
            // Cleanup test node
            session.run("MATCH (test:LoopyDiagnostic) DELETE test");
            
            // Test batch operation
            System.out.print("  • Batch operation (10 nodes)... ");
            startTime = System.nanoTime();
            session.run("""
                UNWIND range(1, 10) AS i
                CREATE (test:LoopyDiagnosticBatch {id: i, timestamp: timestamp()})
                """);
            session.run("MATCH (test:LoopyDiagnosticBatch) DELETE test");
            long batchLatency = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            
            System.out.println("\u001B[32m✓\u001B[0m (" + batchLatency + "ms)");
            report.batchLatency = batchLatency;
            
        } catch (Exception e) {
            System.out.println("\u001B[31m✗\u001B[0m");
            report.addError("Performance test failed: " + e.getMessage());
        }
    }
    
    private void testPermissions(Driver driver, DiagnosticReport report) {
        System.out.println("\n\u001B[33m=== Security & Permissions ===\u001B[0m");
        
        try (Session session = driver.session()) {
            
            // Test read permissions
            System.out.print("  • Read permissions... ");
            session.run("MATCH (n) RETURN count(n) LIMIT 1");
            System.out.println("\u001B[32m✓\u001B[0m");
            report.canRead = true;
            
            // Test write permissions
            System.out.print("  • Write permissions... ");
            session.run("CREATE (test:LoopyPermTest) DELETE test");
            System.out.println("\u001B[32m✓\u001B[0m");
            report.canWrite = true;
            
            // Test schema operations
            System.out.print("  • Schema permissions... ");
            try {
                session.run("CREATE INDEX loopy_test_index IF NOT EXISTS FOR (n:LoopyTest) ON (n.testProp)");
                session.run("DROP INDEX loopy_test_index IF EXISTS");
                System.out.println("\u001B[32m✓\u001B[0m");
                report.canCreateSchema = true;
            } catch (Exception e) {
                System.out.println("\u001B[33m⚠\u001B[0m (limited)");
                report.canCreateSchema = false;
            }
            
            // Check user roles (if available)
            System.out.print("  • User roles... ");
            try {
                Result roleResult = session.run("SHOW CURRENT USER");
                if (roleResult.hasNext()) {
                    System.out.println("\u001B[32m✓\u001B[0m");
                    report.addInfo("User information available");
                } else {
                    System.out.println("\u001B[33m?\u001B[0m (not available)");
                }
            } catch (Exception e) {
                System.out.println("\u001B[33m?\u001B[0m (not available)");
            }
            
        } catch (Exception e) {
            System.out.println("\u001B[31m✗\u001B[0m");
            report.addError("Permission test failed: " + e.getMessage());
        }
    }
    
    private void testSystemResources(Driver driver, DiagnosticReport report) {
        System.out.println("\n\u001B[33m=== System Resources ===\u001B[0m");
        
        // Client-side resources
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        System.out.println("  • Client JVM Memory:");
        System.out.println("    Max: " + formatBytes(maxMemory));
        System.out.println("    Used: " + formatBytes(usedMemory) + " (" + 
            Math.round((double) usedMemory / totalMemory * 100) + "%)");
        System.out.println("    Available: " + formatBytes(maxMemory - usedMemory));
        
        System.out.println("  • Client CPU:");
        System.out.println("    Processors: " + runtime.availableProcessors());
        
        report.clientMaxMemory = maxMemory;
        report.clientUsedMemory = usedMemory;
        report.clientProcessors = runtime.availableProcessors();
        
        // Try to get server-side info
        try (Session session = driver.session()) {
            System.out.print("  • Server configuration... ");
            try {
                // This requires admin privileges
                Result configResult = session.run("CALL dbms.listConfig() YIELD name, value WHERE name CONTAINS 'memory'");
                System.out.println("\u001B[32m✓\u001B[0m");
                report.addInfo("Server configuration accessible");
            } catch (Exception e) {
                System.out.println("\u001B[33m?\u001B[0m (requires admin)");
            }
        } catch (Exception e) {
            report.addWarning("Could not access server information: " + e.getMessage());
        }
    }
    
    private void generateRecommendations(DiagnosticReport report) {
        System.out.println("\n\u001B[33m=== Recommendations ===\u001B[0m");
        
        List<String> recommendations = new ArrayList<>();
        
        // Connection latency recommendations
        if (report.connectionLatency > 100) {
            recommendations.add("High connection latency (" + report.connectionLatency + 
                "ms). Consider using a Neo4j instance closer to your application.");
        }
        
        // Query performance recommendations
        if (report.writeLatency > 50) {
            recommendations.add("Write latency is high (" + report.writeLatency + 
                "ms). Consider optimizing database configuration or hardware.");
        }
        
        // Memory recommendations
        if (report.clientUsedMemory > report.clientMaxMemory * 0.8) {
            recommendations.add("JVM memory usage is high. Consider increasing heap size with -Xmx parameter.");
        }
        
        // Thread recommendations based on CPU
        int recommendedThreads = Math.max(2, Math.min(report.clientProcessors * 2, 16));
        recommendations.add("Recommended thread count for this system: " + recommendedThreads + 
            " (based on " + report.clientProcessors + " available processors)");
        
        // Data size recommendations
        if (report.nodeCount > 1_000_000) {
            recommendations.add("Large dataset detected (" + String.format("%,d", report.nodeCount) + 
                " nodes). Consider using smaller batch sizes and longer test durations.");
        }
        
        // Permission recommendations
        if (!report.canCreateSchema) {
            recommendations.add("Schema creation permissions limited. Some advanced features may not be available.");
        }
        
        // Print recommendations
        if (recommendations.isEmpty()) {
            System.out.println("  \u001B[32m✓ No specific recommendations. Configuration looks good!\u001B[0m");
        } else {
            for (String rec : recommendations) {
                System.out.println("  • " + rec);
                report.addRecommendation(rec);
            }
        }
    }
    
    private String formatBytes(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double value = bytes;
        
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", value, units[unitIndex]);
    }
    
    /**
     * Save diagnostic report to file
     */
    public boolean saveReport(DiagnosticReport report, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("# Loopy Neo4j Diagnostic Report\n");
            writer.write("# Generated: " + report.startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n\n");
            
            writer.write("## Connection Information\n");
            writer.write("URI: " + uri + "\n");
            writer.write("Username: " + username + "\n");
            writer.write("Connection Latency: " + report.connectionLatency + "ms\n\n");
            
            if (report.databaseName != null) {
                writer.write("## Database Information\n");
                writer.write("Name: " + report.databaseName + "\n");
                writer.write("Version: " + report.databaseVersion + "\n");
                writer.write("Edition: " + report.databaseEdition + "\n");
                writer.write("Current Database: " + report.currentDatabase + "\n");
                writer.write("Node Count: " + String.format("%,d", report.nodeCount) + "\n");
                writer.write("Relationship Count: " + String.format("%,d", report.relationshipCount) + "\n\n");
            }
            
            writer.write("## Performance Metrics\n");
            writer.write("Simple Query Latency: " + report.simpleQueryLatency + "ms\n");
            writer.write("Write Latency: " + report.writeLatency + "ms\n");
            writer.write("Batch Latency (10 nodes): " + report.batchLatency + "ms\n\n");
            
            writer.write("## Permissions\n");
            writer.write("Read: " + (report.canRead ? "✓" : "✗") + "\n");
            writer.write("Write: " + (report.canWrite ? "✓" : "✗") + "\n");
            writer.write("Schema: " + (report.canCreateSchema ? "✓" : "✗") + "\n\n");
            
            writer.write("## System Resources\n");
            writer.write("Client Processors: " + report.clientProcessors + "\n");
            writer.write("Client Max Memory: " + formatBytes(report.clientMaxMemory) + "\n");
            writer.write("Client Used Memory: " + formatBytes(report.clientUsedMemory) + "\n\n");
            
            if (!report.recommendations.isEmpty()) {
                writer.write("## Recommendations\n");
                for (String rec : report.recommendations) {
                    writer.write("- " + rec + "\n");
                }
                writer.write("\n");
            }
            
            if (!report.errors.isEmpty()) {
                writer.write("## Errors\n");
                for (String error : report.errors) {
                    writer.write("- " + error + "\n");
                }
                writer.write("\n");
            }
            
            writer.write("## Test Duration\n");
            writer.write("Start: " + report.startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
            writer.write("End: " + report.endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Failed to save diagnostic report: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Diagnostic report data structure
     */
    public static class DiagnosticReport {
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        
        // Connection info
        public long connectionLatency;
        
        // Database info
        public String databaseName;
        public String databaseVersion;
        public String databaseEdition;
        public String currentDatabase;
        public long nodeCount;
        public long relationshipCount;
        
        // Performance metrics
        public long simpleQueryLatency;
        public long writeLatency;
        public long batchLatency;
        
        // Permissions
        public boolean canRead;
        public boolean canWrite;
        public boolean canCreateSchema;
        
        // System resources
        public long clientMaxMemory;
        public long clientUsedMemory;
        public int clientProcessors;
        
        // Results
        public List<String> successes = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> info = new ArrayList<>();
        public List<String> recommendations = new ArrayList<>();
        
        public void addSuccess(String message) { successes.add(message); }
        public void addError(String message) { errors.add(message); }
        public void addWarning(String message) { warnings.add(message); }
        public void addInfo(String message) { info.add(message); }
        public void addRecommendation(String message) { recommendations.add(message); }
    }
}