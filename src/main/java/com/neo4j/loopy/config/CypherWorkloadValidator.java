package com.neo4j.loopy.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import com.neo4j.loopy.TransactionMode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates YAML-based Cypher workload configuration files.
 * Performs structural validation, uniqueness checks, and optional Cypher syntax validation.
 */
public class CypherWorkloadValidator {
    
    private final List<ValidationError> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    
    /**
     * Validation error with context information
     */
    public static class ValidationError {
        private final String message;
        private final String queryId;
        private final String field;
        private final ErrorSeverity severity;
        
        public ValidationError(String message, String queryId, String field, ErrorSeverity severity) {
            this.message = message;
            this.queryId = queryId;
            this.field = field;
            this.severity = severity;
        }
        
        public String getMessage() { return message; }
        public String getQueryId() { return queryId; }
        public String getField() { return field; }
        public ErrorSeverity getSeverity() { return severity; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(severity).append("] ");
            if (queryId != null) {
                sb.append("Query '").append(queryId).append("'");
                if (field != null) {
                    sb.append(", field '").append(field).append("'");
                }
                sb.append(": ");
            }
            sb.append(message);
            return sb.toString();
        }
    }
    
    public enum ErrorSeverity {
        ERROR,
        WARNING
    }
    
    /**
     * Validation result containing parsed config and any errors
     */
    public static class ValidationResult {
        private final boolean valid;
        private final CypherWorkloadConfig config;
        private final List<ValidationError> errors;
        private final List<String> warnings;
        
        public ValidationResult(boolean valid, CypherWorkloadConfig config, 
                                List<ValidationError> errors, List<String> warnings) {
            this.valid = valid;
            this.config = config;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }
        
        public boolean isValid() { return valid; }
        public CypherWorkloadConfig getConfig() { return config; }
        public List<ValidationError> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        
        public void printErrors() {
            for (ValidationError error : errors) {
                System.err.println("  \u2022 " + error);
            }
        }
        
        public void printWarnings() {
            for (String warning : warnings) {
                System.out.println("  \u26A0 " + warning);
            }
        }
    }
    
    /**
     * Validate a YAML workload file
     * @param filePath path to the YAML file
     * @return validation result
     */
    public ValidationResult validate(String filePath) {
        return validate(filePath, null, null, null);
    }
    
    /**
     * Validate a YAML workload file with optional Cypher syntax validation against a database
     * @param filePath path to the YAML file
     * @param neo4jUri Neo4j connection URI for syntax validation (optional)
     * @param username Neo4j username (optional)
     * @param password Neo4j password (optional)
     * @return validation result
     */
    public ValidationResult validate(String filePath, String neo4jUri, String username, String password) {
        errors.clear();
        warnings.clear();
        
        // Step 1: Verify file exists and is readable
        File file = new File(filePath);
        if (!file.exists()) {
            addError("File does not exist: " + filePath, null, null);
            return new ValidationResult(false, null, errors, warnings);
        }
        
        if (!file.canRead()) {
            addError("File is not readable: " + filePath, null, null);
            return new ValidationResult(false, null, errors, warnings);
        }
        
        if (!filePath.endsWith(".yaml") && !filePath.endsWith(".yml")) {
            warnings.add("File does not have .yaml or .yml extension: " + filePath);
        }
        
        // Step 2: Parse YAML structure
        CypherWorkloadConfig config;
        try {
            config = parseYaml(filePath);
        } catch (Exception e) {
            addError("Failed to parse YAML: " + e.getMessage(), null, null);
            return new ValidationResult(false, null, errors, warnings);
        }
        
        // Step 3: Validate structure
        validateStructure(config);
        
        // Step 4: Check unique IDs
        validateUniqueIds(config);
        
        // Step 5: Validate weights
        validateWeights(config);
        
        // Step 6: Validate parameter specifications
        validateParameters(config);

        // Step 7: Validate transaction modes and groups
        validateTransactionModes(config);
        
        // Step 8: Optional Cypher syntax validation
        if (neo4jUri != null && !hasErrors()) {
            validateCypherSyntax(config, neo4jUri, username, password);
        }
        
        return new ValidationResult(!hasErrors(), config, errors, warnings);
    }
    
    private CypherWorkloadConfig parseYaml(String filePath) throws IOException {
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(CypherWorkloadConfig.class, loaderOptions));
        
        try (InputStream inputStream = new FileInputStream(filePath)) {
            CypherWorkloadConfig config = yaml.load(inputStream);
            if (config == null) {
                throw new IOException("YAML file is empty or invalid");
            }
            return config;
        }
    }
    
    private void validateStructure(CypherWorkloadConfig config) {
        // Validate workload name
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            warnings.add("Workload has no name specified");
        }
        
        // Validate queries or transaction groups exist
        boolean hasQueries = config.getQueries() != null && !config.getQueries().isEmpty();
        boolean hasGroups = config.getTransactionGroups() != null && !config.getTransactionGroups().isEmpty();
        if (!hasQueries && !hasGroups) {
            addError("Workload must contain at least one query or transaction group", null, null);
            return;
        }
        
        // Validate each query
        int queryIndex = 0;
        for (CypherWorkloadConfig.QueryDefinition query : config.getQueries()) {
            String queryId = query.getId() != null ? query.getId() : "query[" + queryIndex + "]";
            
            // Check required fields
            if (query.getId() == null || query.getId().trim().isEmpty()) {
                addError("Query is missing required 'id' field", "query[" + queryIndex + "]", "id");
            }
            
            if (query.getCypher() == null || query.getCypher().trim().isEmpty()) {
                addError("Query is missing required 'cypher' field", queryId, "cypher");
            }
            
            // Validate query type
            if (query.getType() == null) {
                warnings.add("Query '" + queryId + "' has no type specified, defaulting to READ");
            }
            
            queryIndex++;
        }

        // Validate transaction groups structure
        if (config.getTransactionGroups() != null) {
            int groupIndex = 0;
            for (CypherWorkloadConfig.TransactionGroupDefinition group : config.getTransactionGroups()) {
                String groupId = group.getId() != null ? group.getId() : "group[" + groupIndex + "]";
                if (group.getId() == null || group.getId().trim().isEmpty()) {
                    addError("Transaction group is missing required 'id' field",
                        "group[" + groupIndex + "]", "id");
                }
                if (group.getQueries() == null || group.getQueries().isEmpty()) {
                    addError("Transaction group must contain at least one query", groupId, "queries");
                } else {
                    int qi = 0;
                    for (CypherWorkloadConfig.QueryDefinition q : group.getQueries()) {
                        String qId = q.getId() != null ? q.getId() : groupId + "[" + qi + "]";
                        if (q.getCypher() == null || q.getCypher().trim().isEmpty()) {
                            addError("Query in group is missing required 'cypher' field", qId, "cypher");
                        }
                        qi++;
                    }
                }
                groupIndex++;
            }
        }
    }
    
    private void validateUniqueIds(CypherWorkloadConfig config) {
        Set<String> seenIds = new HashSet<>();
        if (config.getQueries() != null) {
            for (CypherWorkloadConfig.QueryDefinition query : config.getQueries()) {
                if (query.getId() != null) {
                    if (!seenIds.add(query.getId())) {
                        addError("Duplicate ID: " + query.getId(), query.getId(), "id");
                    }
                }
            }
        }
        if (config.getTransactionGroups() != null) {
            for (CypherWorkloadConfig.TransactionGroupDefinition group : config.getTransactionGroups()) {
                if (group.getId() != null) {
                    if (!seenIds.add(group.getId())) {
                        addError("Duplicate ID: " + group.getId(), group.getId(), "id");
                    }
                }
            }
        }
    }
    
    private void validateWeights(CypherWorkloadConfig config) {
        if (config.getQueries() == null) return;
        
        double totalWeight = 0;
        for (CypherWorkloadConfig.QueryDefinition query : config.getQueries()) {
            String queryId = query.getId() != null ? query.getId() : "unknown";
            
            if (query.getWeight() <= 0) {
                addError("Weight must be positive, got: " + query.getWeight(), queryId, "weight");
            } else {
                totalWeight += query.getWeight();
            }
        }
        
        if (totalWeight <= 0) {
            addError("Total weight of all queries must be positive", null, null);
        }
    }
    
    private void validateParameters(CypherWorkloadConfig config) {
        if (config.getQueries() == null) return;
        
        Pattern validGeneratorPattern = Pattern.compile(
            "^random:(uuid|int:\\d+:\\d+|double:[\\d.]+:[\\d.]+|string:\\d+|long:\\d+:\\d+|boolean)$"
        );
        
        for (CypherWorkloadConfig.QueryDefinition query : config.getQueries()) {
            if (query.getParameters() == null) continue;
            
            String queryId = query.getId() != null ? query.getId() : "unknown";
            String cypher = query.getCypher();
            
            for (Map.Entry<String, String> param : query.getParameters().entrySet()) {
                String paramName = param.getKey();
                String generatorSpec = param.getValue();
                
                // Check if parameter is used in the Cypher query
                if (cypher != null && !cypher.contains("$" + paramName)) {
                    warnings.add("Query '" + queryId + "': parameter '$" + paramName + 
                                "' is defined but not used in the Cypher query");
                }
                
                // Validate generator specification
                if (generatorSpec != null && generatorSpec.startsWith("random:")) {
                    if (!validGeneratorPattern.matcher(generatorSpec).matches()) {
                        // Do a more lenient validation for complex specs
                        if (!isValidGeneratorSpec(generatorSpec)) {
                            addError("Invalid parameter generator specification: " + generatorSpec, 
                                    queryId, "parameters." + paramName);
                        }
                    }
                }
            }
            
            // Check for parameters in Cypher that are not defined
            if (cypher != null) {
                Pattern paramPattern = Pattern.compile("\\$([a-zA-Z_][a-zA-Z0-9_]*)");
                java.util.regex.Matcher matcher = paramPattern.matcher(cypher);
                Set<String> usedParams = new HashSet<>();
                while (matcher.find()) {
                    usedParams.add(matcher.group(1));
                }
                
                for (String usedParam : usedParams) {
                    if (query.getParameters() == null || !query.getParameters().containsKey(usedParam)) {
                        addError("Cypher query uses parameter '$" + usedParam + 
                                "' but it is not defined in parameters", queryId, "cypher");
                    }
                }
            }
        }
    }
    
    private void validateTransactionModes(CypherWorkloadConfig config) {
        // Validate per-query transactionMode overrides
        if (config.getQueries() != null) {
            for (CypherWorkloadConfig.QueryDefinition query : config.getQueries()) {
                String modeStr = query.getTransactionMode();
                if (modeStr != null) {
                    try {
                        TransactionMode.fromString(modeStr);
                    } catch (IllegalArgumentException e) {
                        addError("Invalid transactionMode: '" + modeStr +
                            "'. Valid modes: auto-commit, explicit, managed-read, managed-write, execute-query",
                            query.getId(), "transactionMode");
                    }
                }
            }
        }

        // Validate transaction groups
        if (config.getTransactionGroups() != null) {
            for (CypherWorkloadConfig.TransactionGroupDefinition group : config.getTransactionGroups()) {
                String modeStr = group.getTransactionMode();
                if (modeStr != null) {
                    try {
                        TransactionMode mode = TransactionMode.fromString(modeStr);
                        // execute-query cannot group — each query is a separate driver.executeQuery() call
                        if (mode == TransactionMode.EXECUTE_QUERY &&
                                group.getQueries() != null && group.getQueries().size() > 1) {
                            warnings.add("Transaction group '" + group.getId() +
                                "' uses execute-query mode with " + group.getQueries().size() +
                                " queries. Each query will be executed as a separate driver.executeQuery() call.");
                        }
                        // managed-read with write queries will fail at runtime
                        if (mode == TransactionMode.MANAGED_READ && group.getQueries() != null) {
                            boolean hasWrites = group.getQueries().stream().anyMatch(
                                q -> q.getType() == CypherWorkloadConfig.QueryType.WRITE);
                            if (hasWrites) {
                                warnings.add("Transaction group '" + group.getId() +
                                    "' uses managed-read mode but contains WRITE queries. " +
                                    "Write queries will fail in a read-only transaction.");
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        addError("Invalid transactionMode: '" + modeStr +
                            "'. Valid modes: auto-commit, explicit, managed-read, managed-write, execute-query",
                            group.getId(), "transactionMode");
                    }
                }
            }
        }
    }

    private boolean isValidGeneratorSpec(String spec) {
        String[] parts = spec.split(":");
        if (parts.length < 2) return false;
        
        String type = parts[1].toLowerCase();
        switch (type) {
            case "uuid":
            case "boolean":
                return parts.length == 2;
            case "int":
            case "long":
            case "double":
                return parts.length == 4;
            case "string":
                return parts.length == 3;
            default:
                return false;
        }
    }
    
    private void validateCypherSyntax(CypherWorkloadConfig config, String neo4jUri, 
                                       String username, String password) {
        Driver driver = null;
        try {
            driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(username, password));
            
            try (Session session = driver.session()) {
                for (CypherWorkloadConfig.QueryDefinition query : config.getQueries()) {
                    String queryId = query.getId() != null ? query.getId() : "unknown";
                    String cypher = query.getCypher();
                    
                    if (cypher == null || cypher.trim().isEmpty()) continue;
                    
                    try {
                        // Use EXPLAIN to validate syntax without executing
                        session.run("EXPLAIN " + cypher).consume();
                    } catch (Neo4jException e) {
                        addError("Cypher syntax error: " + e.getMessage(), queryId, "cypher");
                    }
                }
            }
        } catch (Exception e) {
            warnings.add("Could not validate Cypher syntax against database: " + e.getMessage());
        } finally {
            if (driver != null) {
                try {
                    driver.close();
                } catch (Exception e) {
                    // Ignore close errors
                }
            }
        }
    }
    
    private void addError(String message, String queryId, String field) {
        errors.add(new ValidationError(message, queryId, field, ErrorSeverity.ERROR));
    }
    
    private boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Get all validation errors
     */
    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * Get all warnings
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
}
