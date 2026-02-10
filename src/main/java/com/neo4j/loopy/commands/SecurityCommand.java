package com.neo4j.loopy.commands;

import com.neo4j.loopy.security.CredentialManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;
import java.util.Scanner;

/**
 * Security command for managing credentials and audit logs
 */
@Command(name = "security", 
         description = "Manage security credentials and audit logs",
         mixinStandardHelpOptions = true)
public class SecurityCommand implements Callable<Integer> {
    
    @Option(names = {"--store-credential"}, 
            description = "Store a credential securely (will prompt for value)")
    private String credentialKey;
    
    @Option(names = {"--retrieve-credential"}, 
            description = "Retrieve a stored credential")
    private String retrieveKey;
    
    @Option(names = {"--generate-password"}, 
            description = "Generate a secure password")
    private boolean generatePassword;
    
    @Option(names = {"--password-length"}, 
            description = "Length for generated password",
            defaultValue = "16")
    private int passwordLength;
    
    @Option(names = {"--check-audit-log"}, 
            description = "Check audit log health and show path")
    private boolean checkAuditLog;
    
    @Option(names = {"--security-test"}, 
            description = "Run security feature tests")
    private boolean securityTest;
    
    @Override
    public Integer call() throws Exception {
        if (credentialKey != null) {
            return storeCredential();
        }
        
        if (retrieveKey != null) {
            return retrieveCredential();
        }
        
        if (generatePassword) {
            return generateSecurePassword();
        }
        
        if (checkAuditLog) {
            return checkAuditLogHealth();
        }
        
        if (securityTest) {
            return runSecurityTests();
        }
        
        // Default: show security status
        return showSecurityStatus();
    }
    
    private int storeCredential() {
        System.out.printf("🔐 Storing credential for key: %s%n", credentialKey);
        
        String value = CredentialManager.securePasswordPrompt("Enter credential value");
        if (value == null || value.trim().isEmpty()) {
            System.err.println("❌ No credential value provided");
            return 1;
        }
        
        // Validate password strength if it looks like a password
        if (credentialKey.toLowerCase().contains("password") && !CredentialManager.isPasswordStrong(value)) {
            System.out.println("⚠️ Warning: Password does not meet strong password requirements");
            System.out.println("   Strong passwords should have 8+ chars with upper/lower/digit/special characters");
            
            Scanner scanner = new Scanner(System.in);
            System.out.print("Continue anyway? (y/N): ");
            String answer = scanner.nextLine().trim().toLowerCase();
            if (!answer.equals("y") && !answer.equals("yes")) {
                System.out.println("❌ Credential storage cancelled");
                return 1;
            }
        }
        
        if (CredentialManager.storeCredentials(credentialKey, value)) {
            System.out.println("✅ Credential stored securely");
            CredentialManager.clearSensitiveData(value); // Best effort cleanup
            return 0;
        } else {
            System.err.println("❌ Failed to store credential");
            return 1;
        }
    }
    
    private int retrieveCredential() {
        System.out.printf("🔓 Retrieving credential for key: %s%n", retrieveKey);
        
        String value = CredentialManager.retrieveCredentials(retrieveKey);
        if (value != null) {
            System.out.printf("Credential value: %s%n", maskSensitiveValue(value));
            CredentialManager.clearSensitiveData(value); // Best effort cleanup
            return 0;
        } else {
            System.err.println("❌ Credential not found or failed to retrieve");
            return 1;
        }
    }
    
    private String maskSensitiveValue(String value) {
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
    
    private int generateSecurePassword() {
        System.out.printf("🔐 Generating secure password (%d characters)...%n", passwordLength);
        
        if (passwordLength < 8 || passwordLength > 128) {
            System.err.println("❌ Password length must be between 8 and 128 characters");
            return 1;
        }
        
        String password = CredentialManager.generateSecurePassword(passwordLength);
        System.out.printf("Generated password: %s%n", password);
        
        if (CredentialManager.isPasswordStrong(password)) {
            System.out.println("✅ Password meets strong security requirements");
        } else {
            System.out.println("⚠️ Warning: Generated password may not meet all security requirements");
        }
        
        System.out.println();
        System.out.println("💡 Security tips:");
        System.out.println("   • Store this password securely using --store-credential");
        System.out.println("   • Don't include it in configuration files");
        System.out.println("   • Change it regularly in production environments");
        
        return 0;
    }
    
    private int checkAuditLogHealth() {
        System.out.println("🔍 Checking audit log health...");
        
        boolean healthy = CredentialManager.isAuditLogHealthy();
        String auditPath = CredentialManager.getAuditLogPath();
        
        if (healthy) {
            System.out.println("✅ Audit logging is healthy and working");
        } else {
            System.out.println("❌ Audit logging may have issues");
        }
        
        System.out.printf("📂 Audit log location: %s%n", auditPath);
        
        // Check if audit log exists and show basic stats
        java.io.File auditFile = new java.io.File(auditPath);
        if (auditFile.exists()) {
            System.out.printf("   File size: %d bytes%n", auditFile.length());
            System.out.printf("   Last modified: %s%n", 
                new java.util.Date(auditFile.lastModified()));
        } else {
            System.out.println("   File does not exist yet (will be created on first security event)");
        }
        
        return healthy ? 0 : 1;
    }
    
    private int runSecurityTests() {
        System.out.println("🧪 Running security feature tests...");
        System.out.println();
        
        int testsPassed = 0;
        int totalTests = 4;
        
        // Test 1: Password strength validation
        System.out.print("Test 1: Password strength validation... ");
        boolean strongPassword = CredentialManager.isPasswordStrong("Test123!");
        boolean weakPassword = !CredentialManager.isPasswordStrong("weak");
        if (strongPassword && weakPassword) {
            System.out.println("✅ PASS");
            testsPassed++;
        } else {
            System.out.println("❌ FAIL");
        }
        
        // Test 2: Secure password generation
        System.out.print("Test 2: Secure password generation... ");
        try {
            String generated = CredentialManager.generateSecurePassword(12);
            boolean validLength = generated.length() == 12;
            boolean isStrong = CredentialManager.isPasswordStrong(generated);
            if (validLength && isStrong) {
                System.out.println("✅ PASS");
                testsPassed++;
            } else {
                System.out.println("❌ FAIL (length=" + generated.length() + ", strong=" + isStrong + ")");
            }
        } catch (Exception e) {
            System.out.println("❌ FAIL (" + e.getMessage() + ")");
        }
        
        // Test 3: Credential storage/retrieval
        System.out.print("Test 3: Credential storage/retrieval... ");
        try {
            String testKey = "test_credential_" + System.currentTimeMillis();
            String testValue = "test_value_123";
            
            boolean stored = CredentialManager.storeCredentials(testKey, testValue);
            String retrieved = CredentialManager.retrieveCredentials(testKey);
            
            if (stored && testValue.equals(retrieved)) {
                System.out.println("✅ PASS");
                testsPassed++;
            } else {
                System.out.println("❌ FAIL (stored=" + stored + ", retrieved=" + (retrieved != null) + ")");
            }
        } catch (Exception e) {
            System.out.println("❌ FAIL (" + e.getMessage() + ")");
        }
        
        // Test 4: Audit logging
        System.out.print("Test 4: Audit logging... ");
        try {
            CredentialManager.logSecurityEvent("SECURITY_TEST", "Test event for validation", false);
            boolean healthy = CredentialManager.isAuditLogHealthy();
            if (healthy) {
                System.out.println("✅ PASS");
                testsPassed++;
            } else {
                System.out.println("❌ FAIL");
            }
        } catch (Exception e) {
            System.out.println("❌ FAIL (" + e.getMessage() + ")");
        }
        
        System.out.println();
        System.out.printf("🧪 Security tests completed: %d/%d passed%n", testsPassed, totalTests);
        
        if (testsPassed == totalTests) {
            System.out.println("✅ All security features working correctly");
            return 0;
        } else {
            System.out.println("⚠️ Some security features may have issues");
            return 1;
        }
    }
    
    private int showSecurityStatus() {
        System.out.println("🔐 Loopy Security Status");
        System.out.println("========================");
        System.out.println();
        
        // Check audit log
        boolean auditHealthy = CredentialManager.isAuditLogHealthy();
        System.out.printf("Audit Logging: %s%n", auditHealthy ? "✅ Healthy" : "❌ Issues detected");
        System.out.printf("Audit Log: %s%n", CredentialManager.getAuditLogPath());
        
        System.out.println();
        System.out.println("🛡️ Security Features:");
        System.out.println("   • Secure password prompting (no echo)");
        System.out.println("   • Encrypted credential storage");
        System.out.println("   • Password strength validation");
        System.out.println("   • Secure password generation");
        System.out.println("   • Audit logging for security events");
        System.out.println("   • File permission hardening (Unix/Linux/Mac)");
        
        System.out.println();
        System.out.println("💡 Usage Examples:");
        System.out.println("   loopy security --store-credential neo4j_password");
        System.out.println("   loopy security --retrieve-credential neo4j_password");
        System.out.println("   loopy security --generate-password --password-length 20");
        System.out.println("   loopy security --security-test");
        
        return 0;
    }
}