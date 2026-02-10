package com.neo4j.loopy;

/**
 * Simple test to verify compilation
 */
public class TestCLI {
    public static void main(String[] args) {
        System.out.println("Testing CLI compilation...");
        LoopyApplication app = new LoopyApplication();
        System.out.println("LoopyApplication created successfully");
        
        // Test Picocli integration
        try {
            picocli.CommandLine cli = new picocli.CommandLine(app);
            System.out.println("Picocli integration working");
        } catch (Exception e) {
            System.err.println("Picocli error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}