package com.neo4j.loopy.security;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import java.util.Set;

/**
 * Secure credential manager for enterprise environments
 * Handles secure password storage, retrieval, and audit logging
 */
public class CredentialManager {
    
    private static final String CREDENTIAL_DIR = System.getProperty("user.home") + "/.loopy/credentials";
    private static final String CREDENTIAL_FILE = "secure.properties";
    private static final String AUDIT_LOG = "audit.log";
    
    /**
     * Securely prompt for password without echoing to console
     */
    public static String securePasswordPrompt(String prompt) {
        Console console = System.console();
        
        if (console != null) {
            char[] password = console.readPassword("%s: ", prompt);
            if (password != null) {
                String result = new String(password);
                // Clear the password array
                java.util.Arrays.fill(password, ' ');
                logSecurityEvent("PASSWORD_PROMPT", "User prompted for password", false);
                return result;
            }
        } else {
            // Fallback for IDEs or environments without console
            System.out.print(prompt + ": ");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            String password = scanner.nextLine();
            logSecurityEvent("PASSWORD_PROMPT_FALLBACK", "Password prompted via fallback method", true);
            return password;
        }
        
        return null;
    }
    
    /**
     * Store encrypted credentials securely
     */
    public static boolean storeCredentials(String key, String value) {
        try {
            ensureCredentialDirectory();
            
            // Encrypt the value
            String encryptedValue = encryptValue(value);
            
            // Load existing properties
            Properties props = loadCredentialProperties();
            props.setProperty(key, encryptedValue);
            
            // Save securely
            File credentialFile = new File(CREDENTIAL_DIR, CREDENTIAL_FILE);
            try (FileOutputStream fos = new FileOutputStream(credentialFile)) {
                props.store(fos, "Loopy Encrypted Credentials - DO NOT EDIT");
            }
            
            // Set secure file permissions (Unix/Linux/Mac only)
            setSecureFilePermissions(credentialFile.toPath());
            
            logSecurityEvent("CREDENTIAL_STORE", "Stored credential for key: " + key, false);
            return true;
            
        } catch (Exception e) {
            logSecurityEvent("CREDENTIAL_STORE_ERROR", "Failed to store credential: " + e.getMessage(), true);
            return false;
        }
    }
    
    /**
     * Retrieve and decrypt stored credentials
     */
    public static String retrieveCredentials(String key) {
        try {
            Properties props = loadCredentialProperties();
            String encryptedValue = props.getProperty(key);
            
            if (encryptedValue != null) {
                String decryptedValue = decryptValue(encryptedValue);
                logSecurityEvent("CREDENTIAL_RETRIEVE", "Retrieved credential for key: " + key, false);
                return decryptedValue;
            }
            
        } catch (Exception e) {
            logSecurityEvent("CREDENTIAL_RETRIEVE_ERROR", "Failed to retrieve credential: " + e.getMessage(), true);
        }
        
        return null;
    }
    
    /**
     * Simple encryption using Base64 + salt (for demo purposes)
     * In production, use proper encryption like AES-256
     */
    private static String encryptValue(String value) throws Exception {
        byte[] salt = generateSalt();
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        
        // Simple XOR encryption with salt (demo only)
        byte[] encrypted = new byte[valueBytes.length];
        for (int i = 0; i < valueBytes.length; i++) {
            encrypted[i] = (byte) (valueBytes[i] ^ salt[i % salt.length]);
        }
        
        // Combine salt + encrypted data
        byte[] combined = new byte[salt.length + encrypted.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(encrypted, 0, combined, salt.length, encrypted.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    /**
     * Decrypt the encrypted value
     */
    private static String decryptValue(String encryptedValue) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedValue);
        
        // Extract salt and encrypted data
        byte[] salt = new byte[16];
        byte[] encrypted = new byte[combined.length - 16];
        System.arraycopy(combined, 0, salt, 0, 16);
        System.arraycopy(combined, 16, encrypted, 0, encrypted.length);
        
        // Decrypt using XOR
        byte[] decrypted = new byte[encrypted.length];
        for (int i = 0; i < encrypted.length; i++) {
            decrypted[i] = (byte) (encrypted[i] ^ salt[i % salt.length]);
        }
        
        return new String(decrypted, StandardCharsets.UTF_8);
    }
    
    /**
     * Generate random salt for encryption
     */
    private static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }
    
    /**
     * Ensure credential directory exists with secure permissions
     */
    private static void ensureCredentialDirectory() throws IOException {
        File dir = new File(CREDENTIAL_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
            setSecureDirectoryPermissions(dir.toPath());
        }
    }
    
    /**
     * Load credential properties file
     */
    private static Properties loadCredentialProperties() {
        Properties props = new Properties();
        File credentialFile = new File(CREDENTIAL_DIR, CREDENTIAL_FILE);
        
        if (credentialFile.exists()) {
            try (FileInputStream fis = new FileInputStream(credentialFile)) {
                props.load(fis);
            } catch (IOException e) {
                logSecurityEvent("CREDENTIAL_LOAD_ERROR", "Failed to load credentials: " + e.getMessage(), true);
            }
        }
        
        return props;
    }
    
    /**
     * Set secure file permissions (Unix/Linux/Mac only)
     */
    private static void setSecureFilePermissions(Path path) {
        try {
            // Set permissions to 600 (read/write for owner only)
            Set<PosixFilePermission> perms = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(path, perms);
        } catch (Exception e) {
            // Ignore on Windows or if POSIX not supported
            logSecurityEvent("PERMISSION_WARNING", "Could not set secure file permissions: " + e.getMessage(), true);
        }
    }
    
    /**
     * Set secure directory permissions
     */
    private static void setSecureDirectoryPermissions(Path path) {
        try {
            // Set permissions to 700 (read/write/execute for owner only)
            Set<PosixFilePermission> perms = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            );
            Files.setPosixFilePermissions(path, perms);
        } catch (Exception e) {
            // Ignore on Windows or if POSIX not supported
        }
    }
    
    /**
     * Log security events for audit purposes
     */
    public static void logSecurityEvent(String eventType, String description, boolean isWarning) {
        try {
            ensureCredentialDirectory();
            
            String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String logLevel = isWarning ? "WARN" : "INFO";
            String logEntry = String.format("[%s] %s %s: %s%n", timestamp, logLevel, eventType, description);
            
            Path auditFile = Paths.get(CREDENTIAL_DIR, AUDIT_LOG);
            Files.write(auditFile, logEntry.getBytes(StandardCharsets.UTF_8), 
                java.nio.file.StandardOpenOption.CREATE, 
                java.nio.file.StandardOpenOption.APPEND);
            
        } catch (Exception e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }
    
    /**
     * Validate password strength
     */
    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasLower && hasUpper && hasDigit && hasSpecial;
    }
    
    /**
     * Generate a secure random password
     */
    public static String generateSecurePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
    
    /**
     * Clear sensitive data from memory
     */
    public static void clearSensitiveData(String... sensitiveStrings) {
        // In Java, strings are immutable, so we can't actually clear them
        // This is a placeholder for best practices
        logSecurityEvent("MEMORY_CLEAR", "Attempted to clear sensitive data from memory", false);
        
        // Force garbage collection (not guaranteed, but may help)
        System.gc();
    }
    
    /**
     * Check if audit logging is enabled and working
     */
    public static boolean isAuditLogHealthy() {
        try {
            logSecurityEvent("AUDIT_CHECK", "Health check performed", false);
            Path auditFile = Paths.get(CREDENTIAL_DIR, AUDIT_LOG);
            return Files.exists(auditFile) && Files.isWritable(auditFile);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get path to audit log for review
     */
    public static String getAuditLogPath() {
        return Paths.get(CREDENTIAL_DIR, AUDIT_LOG).toString();
    }
}