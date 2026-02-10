package com.neo4j.loopy.enterprise;

import com.neo4j.loopy.LoopyConfig;
import org.neo4j.driver.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise cluster support for multi-instance load balancing
 * Supports multiple Neo4j instances with health monitoring and failover
 */
public class ClusterSupport {
    
    private final List<ClusterNode> nodes;
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    private final Map<String, Driver> driverCache = new ConcurrentHashMap<>();
    private final LoadBalancingStrategy strategy;
    
    public enum LoadBalancingStrategy {
        ROUND_ROBIN,    // Distribute requests evenly across nodes
        HEALTH_BASED,   // Prefer healthy nodes with good response times
        RANDOM,         // Random selection
        WEIGHTED        // Weighted distribution based on capacity
    }
    
    public ClusterSupport(List<String> nodeUris, String username, String password, 
                         LoadBalancingStrategy strategy) {
        this.strategy = strategy;
        this.nodes = new ArrayList<>();
        
        // Initialize cluster nodes
        for (String uri : nodeUris) {
            ClusterNode node = new ClusterNode(uri, username, password);
            nodes.add(node);
        }
        
        System.out.printf("🏢 Initialized cluster support with %d nodes%n", nodes.size());
        System.out.printf("   Strategy: %s%n", strategy);
        
        // Perform initial health check
        performHealthCheck();
    }
    
    /**
     * Get a driver for the next available node based on load balancing strategy
     */
    public Driver getDriver() throws RuntimeException {
        ClusterNode selectedNode = selectNode();
        
        if (selectedNode == null) {
            throw new RuntimeException("No healthy cluster nodes available");
        }
        
        return getOrCreateDriver(selectedNode);
    }
    
    /**
     * Select a node based on the configured load balancing strategy
     */
    private ClusterNode selectNode() {
        List<ClusterNode> healthyNodes = getHealthyNodes();
        
        if (healthyNodes.isEmpty()) {
            return null;
        }
        
        switch (strategy) {
            case ROUND_ROBIN:
                return selectRoundRobin(healthyNodes);
            case HEALTH_BASED:
                return selectHealthBased(healthyNodes);
            case RANDOM:
                return selectRandom(healthyNodes);
            case WEIGHTED:
                return selectWeighted(healthyNodes);
            default:
                return selectRoundRobin(healthyNodes);
        }
    }
    
    private ClusterNode selectRoundRobin(List<ClusterNode> healthyNodes) {
        int index = roundRobinCounter.getAndIncrement() % healthyNodes.size();
        return healthyNodes.get(index);
    }
    
    private ClusterNode selectHealthBased(List<ClusterNode> healthyNodes) {
        // Select node with best (lowest) average response time
        return healthyNodes.stream()
            .min(Comparator.comparing(ClusterNode::getAverageResponseTime))
            .orElse(healthyNodes.get(0));
    }
    
    private ClusterNode selectRandom(List<ClusterNode> healthyNodes) {
        Random random = new Random();
        return healthyNodes.get(random.nextInt(healthyNodes.size()));
    }
    
    private ClusterNode selectWeighted(List<ClusterNode> healthyNodes) {
        // Simple weighted selection based on capacity
        int totalWeight = healthyNodes.stream()
            .mapToInt(ClusterNode::getCapacityWeight)
            .sum();
        
        Random random = new Random();
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (ClusterNode node : healthyNodes) {
            currentWeight += node.getCapacityWeight();
            if (randomWeight < currentWeight) {
                return node;
            }
        }
        
        return healthyNodes.get(0); // Fallback
    }
    
    /**
     * Get list of healthy nodes
     */
    private List<ClusterNode> getHealthyNodes() {
        return nodes.stream()
            .filter(ClusterNode::isHealthy)
            .toList();
    }
    
    /**
     * Get or create a driver for the specified node
     */
    private Driver getOrCreateDriver(ClusterNode node) {
        return driverCache.computeIfAbsent(node.getUri(), uri -> {
            return GraphDatabase.driver(uri, AuthTokens.basic(node.getUsername(), node.getPassword()));
        });
    }
    
    /**
     * Perform health check on all cluster nodes
     */
    public void performHealthCheck() {
        System.out.println("🔍 Performing cluster health check...");
        
        for (ClusterNode node : nodes) {
            checkNodeHealth(node);
        }
        
        long healthyCount = nodes.stream().filter(ClusterNode::isHealthy).count();
        System.out.printf("   Healthy nodes: %d/%d%n", healthyCount, nodes.size());
        
        if (healthyCount == 0) {
            System.err.println("⚠️ WARNING: No healthy cluster nodes available!");
        }
    }
    
    /**
     * Check health of a single node
     */
    private void checkNodeHealth(ClusterNode node) {
        try {
            Driver driver = getOrCreateDriver(node);
            long startTime = System.currentTimeMillis();
            
            try (Session session = driver.session()) {
                session.run("RETURN 1").consume();
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            node.updateHealth(true, responseTime);
            
            System.out.printf("   ✅ %s (response: %dms)%n", node.getUri(), responseTime);
            
        } catch (Exception e) {
            node.updateHealth(false, -1);
            System.out.printf("   ❌ %s (error: %s)%n", node.getUri(), e.getMessage());
        }
    }
    
    /**
     * Get cluster status information
     */
    public ClusterStatus getClusterStatus() {
        long totalNodes = nodes.size();
        long healthyNodes = nodes.stream().filter(ClusterNode::isHealthy).count();
        double averageResponseTime = nodes.stream()
            .filter(ClusterNode::isHealthy)
            .mapToLong(ClusterNode::getAverageResponseTime)
            .average()
            .orElse(0.0);
        
        return new ClusterStatus(totalNodes, healthyNodes, averageResponseTime, strategy);
    }
    
    /**
     * Shutdown all drivers and cleanup resources
     */
    public void shutdown() {
        System.out.println("🔌 Shutting down cluster support...");
        
        for (Driver driver : driverCache.values()) {
            try {
                driver.close();
            } catch (Exception e) {
                System.err.println("Error closing driver: " + e.getMessage());
            }
        }
        
        driverCache.clear();
        System.out.println("✅ Cluster support shutdown complete");
    }
    
    /**
     * Cluster node representation
     */
    public static class ClusterNode {
        private final String uri;
        private final String username;
        private final String password;
        private volatile boolean healthy = true;
        private volatile long lastCheckTime = System.currentTimeMillis();
        private volatile long averageResponseTime = 0;
        private final int capacityWeight;
        
        public ClusterNode(String uri, String username, String password) {
            this.uri = uri;
            this.username = username;
            this.password = password;
            
            // Simple capacity weight based on URI (could be configurable)
            this.capacityWeight = extractPortFromUri(uri) % 10 + 1; // 1-10 weight
        }
        
        private int extractPortFromUri(String uri) {
            try {
                return URI.create(uri).getPort();
            } catch (Exception e) {
                return 7687; // default
            }
        }
        
        public void updateHealth(boolean healthy, long responseTime) {
            this.healthy = healthy;
            this.lastCheckTime = System.currentTimeMillis();
            if (responseTime > 0) {
                // Simple moving average
                this.averageResponseTime = (this.averageResponseTime + responseTime) / 2;
            }
        }
        
        // Getters
        public String getUri() { return uri; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public boolean isHealthy() { return healthy; }
        public long getLastCheckTime() { return lastCheckTime; }
        public long getAverageResponseTime() { return averageResponseTime; }
        public int getCapacityWeight() { return capacityWeight; }
    }
    
    /**
     * Cluster status information
     */
    public static class ClusterStatus {
        private final long totalNodes;
        private final long healthyNodes;
        private final double averageResponseTime;
        private final LoadBalancingStrategy strategy;
        
        public ClusterStatus(long totalNodes, long healthyNodes, double averageResponseTime, 
                           LoadBalancingStrategy strategy) {
            this.totalNodes = totalNodes;
            this.healthyNodes = healthyNodes;
            this.averageResponseTime = averageResponseTime;
            this.strategy = strategy;
        }
        
        public void printStatus() {
            System.out.printf("🏢 Cluster Status:%n");
            System.out.printf("   Total nodes: %d%n", totalNodes);
            System.out.printf("   Healthy nodes: %d%n", healthyNodes);
            System.out.printf("   Availability: %.1f%%%n", (healthyNodes * 100.0 / totalNodes));
            System.out.printf("   Average response time: %.1fms%n", averageResponseTime);
            System.out.printf("   Load balancing: %s%n", strategy);
        }
        
        // Getters
        public long getTotalNodes() { return totalNodes; }
        public long getHealthyNodes() { return healthyNodes; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public LoadBalancingStrategy getStrategy() { return strategy; }
    }
}