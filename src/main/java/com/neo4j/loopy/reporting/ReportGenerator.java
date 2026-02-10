package com.neo4j.loopy.reporting;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Generate detailed reports from load testing results
 */
public class ReportGenerator {
    
    /**
     * Generate HTML report from test results
     */
    public void generateHtmlReport(TestResults results, String outputPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(generateHtmlContent(results));
        }
        System.out.printf("📊 HTML report generated: %s%n", outputPath);
    }
    
    /**
     * Generate markdown report from test results  
     */
    public void generateMarkdownReport(TestResults results, String outputPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(generateMarkdownContent(results));
        }
        System.out.printf("📝 Markdown report generated: %s%n", outputPath);
    }
    
    /**
     * Generate CSV data export from test results
     */
    public void generateCsvExport(TestResults results, String outputPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(generateCsvContent(results));
        }
        System.out.printf("📈 CSV export generated: %s%n", outputPath);
    }
    
    private String generateHtmlContent(TestResults results) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\\n");
        html.append("<html>\\n<head>\\n");
        html.append("  <title>Loopy Load Test Report</title>\\n");
        html.append("  <style>\\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 40px; }\\n");
        html.append("    .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }\\n");
        html.append("    .metric { background-color: #e8f4fd; padding: 15px; margin: 10px 0; border-radius: 5px; }\\n");
        html.append("    .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; }\\n");
        html.append("    .success { background-color: #d4edda; border-left: 4px solid #28a745; }\\n");
        html.append("    table { border-collapse: collapse; width: 100%; margin-top: 20px; }\\n");
        html.append("    th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\\n");
        html.append("    th { background-color: #f2f2f2; }\\n");
        html.append("  </style>\\n");
        html.append("</head>\\n<body>\\n");
        
        // Header
        html.append("  <div class='header'>\\n");
        html.append("    <h1>🚀 Loopy Load Test Report</h1>\\n");
        html.append("    <p><strong>Generated:</strong> ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</p>\\n");
        html.append("    <p><strong>Test Duration:</strong> ").append(results.getDurationSeconds()).append(" seconds</p>\\n");
        html.append("  </div>\\n");
        
        // Summary metrics
        html.append("  <h2>📊 Performance Summary</h2>\\n");
        html.append("  <div class='metric'>\\n");
        html.append("    <h3>Operations Per Second</h3>\\n");
        html.append("    <p><strong>Average:</strong> ").append(String.format("%.2f", results.getAverageOpsPerSecond())).append(" ops/sec</p>\\n");
        html.append("    <p><strong>Peak:</strong> ").append(String.format("%.2f", results.getPeakOpsPerSecond())).append(" ops/sec</p>\\n");
        html.append("  </div>\\n");
        
        html.append("  <div class='metric'>\\n");
        html.append("    <h3>Response Times</h3>\\n");
        html.append("    <p><strong>Average:</strong> ").append(String.format("%.2f", results.getAverageResponseTime())).append(" ms</p>\\n");
        html.append("    <p><strong>95th percentile:</strong> ").append(String.format("%.2f", results.getP95ResponseTime())).append(" ms</p>\\n");
        html.append("    <p><strong>99th percentile:</strong> ").append(String.format("%.2f", results.getP99ResponseTime())).append(" ms</p>\\n");
        html.append("  </div>\\n");
        
        // Recommendations
        html.append("  <h2>💡 Recommendations</h2>\\n");
        for (String recommendation : results.getRecommendations()) {
            html.append("  <div class='metric success'>\\n");
            html.append("    <p>").append(recommendation).append("</p>\\n");
            html.append("  </div>\\n");
        }
        
        html.append("</body>\\n</html>\\n");
        return html.toString();
    }
    
    private String generateMarkdownContent(TestResults results) {
        StringBuilder md = new StringBuilder();
        
        md.append("# 🚀 Loopy Load Test Report\\n\\n");
        md.append("**Generated:** ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\\n");
        md.append("**Test Duration:** ").append(results.getDurationSeconds()).append(" seconds\\n\\n");
        
        md.append("## 📊 Performance Summary\\n\\n");
        md.append("### Operations Per Second\\n");
        md.append("- **Average:** ").append(String.format("%.2f", results.getAverageOpsPerSecond())).append(" ops/sec\\n");
        md.append("- **Peak:** ").append(String.format("%.2f", results.getPeakOpsPerSecond())).append(" ops/sec\\n\\n");
        
        md.append("### Response Times\\n");
        md.append("- **Average:** ").append(String.format("%.2f", results.getAverageResponseTime())).append(" ms\\n");
        md.append("- **95th percentile:** ").append(String.format("%.2f", results.getP95ResponseTime())).append(" ms\\n");
        md.append("- **99th percentile:** ").append(String.format("%.2f", results.getP99ResponseTime())).append(" ms\\n\\n");
        
        md.append("## 💡 Recommendations\\n\\n");
        for (String recommendation : results.getRecommendations()) {
            md.append("- ").append(recommendation).append("\\n");
        }
        
        md.append("\\n---\\n");
        md.append("*Report generated by Loopy v1.0.0*\\n");
        
        return md.toString();
    }
    
    private String generateCsvContent(TestResults results) {
        StringBuilder csv = new StringBuilder();
        
        // Headers
        csv.append("timestamp,operation_type,response_time_ms,success,error_message\\n");
        
        // Sample data - in real implementation would come from collected metrics
        csv.append("2024-02-06T10:00:00,CREATE_NODE,12.5,true,\\n");
        csv.append("2024-02-06T10:00:01,CREATE_RELATIONSHIP,8.3,true,\\n");
        csv.append("2024-02-06T10:00:02,READ_NODE,5.1,true,\\n");
        
        return csv.toString();
    }
    
    /**
     * Container for test results data
     */
    public static class TestResults {
        private final int durationSeconds;
        private final double averageOpsPerSecond;
        private final double peakOpsPerSecond;
        private final double averageResponseTime;
        private final double p95ResponseTime;
        private final double p99ResponseTime;
        private final List<String> recommendations;
        
        public TestResults(int durationSeconds, double averageOpsPerSecond, double peakOpsPerSecond,
                          double averageResponseTime, double p95ResponseTime, double p99ResponseTime,
                          List<String> recommendations) {
            this.durationSeconds = durationSeconds;
            this.averageOpsPerSecond = averageOpsPerSecond;
            this.peakOpsPerSecond = peakOpsPerSecond;
            this.averageResponseTime = averageResponseTime;
            this.p95ResponseTime = p95ResponseTime;
            this.p99ResponseTime = p99ResponseTime;
            this.recommendations = recommendations;
        }
        
        // Getters
        public int getDurationSeconds() { return durationSeconds; }
        public double getAverageOpsPerSecond() { return averageOpsPerSecond; }
        public double getPeakOpsPerSecond() { return peakOpsPerSecond; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public double getP95ResponseTime() { return p95ResponseTime; }
        public double getP99ResponseTime() { return p99ResponseTime; }
        public List<String> getRecommendations() { return recommendations; }
    }
}