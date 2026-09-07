package com.neo4j.loopy.config;

import com.neo4j.loopy.TransactionMode;
import com.neo4j.loopy.tx.QueryUnit;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Configuration model for YAML-based Cypher workload definitions.
 * Supports weighted query selection and parameterized queries with random value generators.
 */
public class CypherWorkloadConfig {
    
    private String name;
    private String description;
    private List<QueryDefinition> queries = new ArrayList<>();
    private List<TransactionGroupDefinition> transactionGroups = new ArrayList<>();
    
    // Unified selection pool (individual queries + transaction groups)
    private List<QueryUnit> selectionPool = new ArrayList<>();
    private double[] poolCumulativeWeights;
    private double poolTotalWeight;

    // Kept for backward compatibility
    private double[] cumulativeWeights;
    private double totalWeight;
    
    public CypherWorkloadConfig() {
    }
    
    /**
     * Load workload configuration from a YAML file
     * @param filePath path to the YAML file
     * @return parsed CypherWorkloadConfig
     * @throws FileNotFoundException if file doesn't exist
     * @throws IOException if file can't be read
     */
    public static CypherWorkloadConfig loadFromFile(String filePath) throws IOException {
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(CypherWorkloadConfig.class, loaderOptions));
        
        try (InputStream inputStream = new FileInputStream(filePath)) {
            CypherWorkloadConfig config = yaml.load(inputStream);
            config.computeCumulativeWeights();
            return config;
        }
    }
    
    /**
     * Compute cumulative weights for efficient O(log n) weighted random selection.
     * Builds a unified pool of individual queries and transaction groups.
     */
    private void computeCumulativeWeights() {
        selectionPool = new ArrayList<>();
        List<Double> poolWeights = new ArrayList<>();

        if (queries != null) {
            for (QueryDefinition q : queries) {
                TransactionMode mode = q.getTransactionMode() != null
                    ? TransactionMode.fromString(q.getTransactionMode()) : null;
                selectionPool.add(QueryUnit.single(q, mode));
                poolWeights.add(q.getWeight());
            }
        }

        if (transactionGroups != null) {
            for (TransactionGroupDefinition g : transactionGroups) {
                if (g.getQueries() != null && !g.getQueries().isEmpty()) {
                    TransactionMode mode = g.getTransactionMode() != null
                        ? TransactionMode.fromString(g.getTransactionMode()) : null;
                    selectionPool.add(QueryUnit.group(g.getId(), g.getQueries(), mode));
                    poolWeights.add(g.getWeight());
                }
            }
        }

        poolCumulativeWeights = new double[selectionPool.size()];
        poolTotalWeight = 0;
        for (int i = 0; i < poolWeights.size(); i++) {
            poolTotalWeight += poolWeights.get(i);
            poolCumulativeWeights[i] = poolTotalWeight;
        }

        // Backward-compat arrays for selectWeightedQuery()
        if (queries != null && !queries.isEmpty()) {
            cumulativeWeights = new double[queries.size()];
            totalWeight = 0;
            for (int i = 0; i < queries.size(); i++) {
                totalWeight += queries.get(i).getWeight();
                cumulativeWeights[i] = totalWeight;
            }
        } else {
            cumulativeWeights = new double[0];
            totalWeight = 0;
        }
    }
    
    /**
     * Select a query unit (single query or transaction group) based on configured
     * weights using binary search over the unified selection pool.
     */
    public QueryUnit selectQueryUnit() {
        if (selectionPool.isEmpty()) {
            throw new IllegalStateException("No queries or transaction groups configured");
        }
        double value = ThreadLocalRandom.current().nextDouble() * poolTotalWeight;
        return selectionPool.get(binarySearch(poolCumulativeWeights, value));
    }

    /**
     * Select a query based on configured weights using binary search.
     * @return selected QueryDefinition
     */
    public QueryDefinition selectWeightedQuery() {
        if (queries.isEmpty()) {
            throw new IllegalStateException("No queries configured");
        }
        
        Random random = ThreadLocalRandom.current();
        double value = random.nextDouble() * totalWeight;
        return queries.get(binarySearch(cumulativeWeights, value));
    }

    private int binarySearch(double[] weights, double value) {
        int low = 0;
        int high = weights.length - 1;
        while (low < high) {
            int mid = (low + high) / 2;
            if (weights[mid] < value) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<QueryDefinition> getQueries() { return queries; }
    public void setQueries(List<QueryDefinition> queries) { 
        this.queries = queries; 
        computeCumulativeWeights();
    }

    public List<TransactionGroupDefinition> getTransactionGroups() { return transactionGroups; }
    public void setTransactionGroups(List<TransactionGroupDefinition> transactionGroups) {
        this.transactionGroups = transactionGroups;
        computeCumulativeWeights();
    }
    
    public double getTotalWeight() { return poolTotalWeight; }
    
    /**
     * Represents a single query definition within the workload
     */
    public static class QueryDefinition {
        private String id;
        private String cypher;
        private double weight = 1.0;
        private QueryType type = QueryType.READ;
        private Map<String, String> parameters = new HashMap<>();
        private String transactionMode;  // optional per-query override
        
        public QueryDefinition() {
        }
        
        /**
         * Generate parameter values based on configured generators
         * @return map of parameter names to generated values
         */
        public Map<String, Object> generateParameters() {
            Map<String, Object> generatedParams = new HashMap<>();
            Random random = ThreadLocalRandom.current();
            
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String paramName = entry.getKey();
                String generatorSpec = entry.getValue();
                Object value = generateValue(generatorSpec, random);
                generatedParams.put(paramName, value);
            }
            
            return generatedParams;
        }
        
        /**
         * Generate a value based on the generator specification
         * Supported formats:
         * - random:uuid - generates a UUID string
         * - random:int:min:max - generates an integer between min and max
         * - random:double:min:max - generates a double between min and max
         * - random:string:length - generates a random alphanumeric string of given length
         * - literal value - returned as-is
         */
        private Object generateValue(String spec, Random random) {
            if (spec == null || !spec.startsWith("random:")) {
                // Return as literal value
                return spec;
            }
            
            String[] parts = spec.split(":");
            String type = parts[1].toLowerCase();
            
            switch (type) {
                case "uuid":
                    return UUID.randomUUID().toString();
                    
                case "int":
                    if (parts.length < 4) {
                        throw new IllegalArgumentException("Invalid int generator spec: " + spec + ". Expected format: random:int:min:max");
                    }
                    int minInt = Integer.parseInt(parts[2]);
                    int maxInt = Integer.parseInt(parts[3]);
                    return minInt + random.nextInt(maxInt - minInt + 1);
                    
                case "double":
                    if (parts.length < 4) {
                        throw new IllegalArgumentException("Invalid double generator spec: " + spec + ". Expected format: random:double:min:max");
                    }
                    double minDouble = Double.parseDouble(parts[2]);
                    double maxDouble = Double.parseDouble(parts[3]);
                    return minDouble + random.nextDouble() * (maxDouble - minDouble);
                    
                case "string":
                    if (parts.length < 3) {
                        throw new IllegalArgumentException("Invalid string generator spec: " + spec + ". Expected format: random:string:length");
                    }
                    int length = Integer.parseInt(parts[2]);
                    return generateRandomString(length, random);
                    
                case "long":
                    if (parts.length < 4) {
                        throw new IllegalArgumentException("Invalid long generator spec: " + spec + ". Expected format: random:long:min:max");
                    }
                    long minLong = Long.parseLong(parts[2]);
                    long maxLong = Long.parseLong(parts[3]);
                    return minLong + (long) (random.nextDouble() * (maxLong - minLong + 1));
                    
                case "boolean":
                    return random.nextBoolean();
                    
                default:
                    throw new IllegalArgumentException("Unknown generator type: " + type + " in spec: " + spec);
            }
        }
        
        private String generateRandomString(int length, Random random) {
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            return sb.toString();
        }
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getCypher() { return cypher; }
        public void setCypher(String cypher) { this.cypher = cypher; }
        
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        
        public QueryType getType() { return type; }
        public void setType(QueryType type) { this.type = type; }
        
        public Map<String, String> getParameters() { return parameters; }
        public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
        
        public String getTransactionMode() { return transactionMode; }
        public void setTransactionMode(String transactionMode) { this.transactionMode = transactionMode; }
        
        public boolean isWrite() {
            return type == QueryType.WRITE;
        }
        
        public boolean isRead() {
            return type == QueryType.READ;
        }
        
        @Override
        public String toString() {
            return "QueryDefinition{" +
                    "id='" + id + '\'' +
                    ", type=" + type +
                    ", weight=" + weight +
                    (transactionMode != null ? ", transactionMode='" + transactionMode + '\'': "") +
                    '}';
        }
    }

    /**
     * A {@link QueryDefinition} whose parameters are supplied dynamically at runtime
     * rather than from YAML generator specs. Used by {@link com.neo4j.loopy.LoopyWorker}
     * to wrap inline Cypher with live parameter values.
     */
    public static class DynamicQueryDefinition extends QueryDefinition {

        private final Supplier<Map<String, Object>> paramSupplier;

        public DynamicQueryDefinition(String id, String cypher, QueryType type,
                                      Supplier<Map<String, Object>> paramSupplier) {
            setId(id);
            setCypher(cypher);
            setType(type);
            this.paramSupplier = paramSupplier;
        }

        @Override
        public Map<String, Object> generateParameters() {
            return paramSupplier.get();
        }
    }

    /**
     * A named group of queries that should execute together in a single transaction.
     * All queries in the group are run within one transaction callback; the group as
     * a whole participates in the weighted selection pool.
     *
     * <p>YAML example:
     * <pre>
     * transactionGroups:
     *   - id: order-workflow
     *     weight: 10
     *     transactionMode: explicit
     *     queries:
     *       - id: create-order
     *         cypher: "CREATE (o:Order {id: $orderId}) RETURN o"
     *         type: WRITE
     *         parameters:
     *           orderId: "random:uuid"
     *       - id: update-inventory
     *         cypher: "MATCH (p:Product {id: $productId}) SET p.stock = p.stock - 1"
     *         type: WRITE
     *         parameters:
     *           productId: "random:int:1:1000"
     * </pre>
     */
    public static class TransactionGroupDefinition {
        private String id;
        private String description;
        private double weight = 1.0;
        private String transactionMode;
        private List<QueryDefinition> queries = new ArrayList<>();

        public TransactionGroupDefinition() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }

        public String getTransactionMode() { return transactionMode; }
        public void setTransactionMode(String transactionMode) { this.transactionMode = transactionMode; }

        public List<QueryDefinition> getQueries() { return queries; }
        public void setQueries(List<QueryDefinition> queries) { this.queries = queries; }
    }
    
    /**
     * Query type enum for distinguishing read and write operations
     */
    public enum QueryType {
        READ,
        WRITE;
        
        /**
         * Parse from string, case-insensitive
         */
        public static QueryType fromString(String value) {
            if (value == null) {
                return READ;
            }
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return READ;
            }
        }
    }
    
    @Override
    public String toString() {
        return "CypherWorkloadConfig{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", queries=" + queries.size() +
                ", totalWeight=" + totalWeight +
                '}';
    }
}
