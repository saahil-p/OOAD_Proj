import java.util.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

/**
 * Main class for VANET (Vehicular Ad-hoc Network) Optimization
 * Comparison between ML-based and traditional routing approaches
 * With congestion-based and environment-based packet loss
 */
public class VANETOptimizationComparison {
    // Message types
    public static final int SAFETY_CRITICAL = 1;   // High priority, small payload, high rate (1/sec)
    public static final int TELEMETRY = 2;         // Medium priority, medium payload (200B), medium rate
    public static final int INFOTAINMENT = 3;      // Low priority, large payload (1500B), low rate
    
    // Message size configuration (bytes)
    public static final int SAFETY_PAYLOAD_SIZE = 50;
    public static final int TELEMETRY_PAYLOAD_SIZE = 200;
    public static final int INFOTAINMENT_PAYLOAD_SIZE = 1500;
    
    // Message rates (in milliseconds between messages)
    public static final long SAFETY_INTERVAL = 1000;        // 1 message per second
    public static final long TELEMETRY_INTERVAL = 5000;     // 1 message per 5 seconds
    public static final long INFOTAINMENT_INTERVAL = 15000; // 1 message per 15 seconds
    
    // Environment types for packet loss modeling
    public static final int URBAN = 1;     // Dense buildings, high interference
    public static final int SUBURBAN = 2;  // Mixed buildings and open spaces
    public static final int HIGHWAY = 3;   // Open space, high vehicle speeds
    
    /**
     * Main method to run the comparison
     */
    public static void main(String[] args) {
        // Create performance metrics collectors
        PerformanceMetricsCollector mlMetrics = new PerformanceMetricsCollector();
        PerformanceMetricsCollector traditionalMetrics = new PerformanceMetricsCollector();
        
        // Run the same scenario with ML optimization
        System.out.println("Running simulation with ML optimization...");
        runSimulation(true, mlMetrics);
        
        // Run the same scenario without ML optimization
        System.out.println("\nRunning simulation with traditional routing...");
        runSimulation(false, traditionalMetrics);
        
        // Compare and print results
        System.out.println("\n==== VANET Performance Comparison ====");
        compareMetrics(mlMetrics, traditionalMetrics);
        
        // Save results to CSV file
        saveResultsToCSV(mlMetrics, traditionalMetrics);
    }
    
    /**
     * Runs a VANET simulation with or without ML optimization
     * @param useML Whether to use ML optimization
     * @param metrics Collector for performance metrics
     */
    private static void runSimulation(boolean useML, PerformanceMetricsCollector metrics) {
        // Create VANET optimizer with 1km x 1km area
        VANETSimulator vanet = new VANETSimulator(1000, 1000, 300, 0.1, 0.9, useML, metrics);
        
        // Add roads with environment types
        vanet.addRoad(0, 250, 1000, 250, 2, 13.9, URBAN); // 50 km/h horizontal urban road
        vanet.addRoad(0, 750, 1000, 750, 2, 13.9, SUBURBAN); // 50 km/h horizontal suburban road
        vanet.addRoad(250, 0, 250, 1000, 2, 13.9, URBAN); // 50 km/h vertical urban road
        vanet.addRoad(750, 0, 750, 1000, 2, 25.0, HIGHWAY); // 90 km/h vertical highway
        
        // Set congestion zones - areas with higher vehicle density and network congestion
        vanet.addCongestionZone(200, 200, 300, 300, 0.8); // High congestion intersection (80% network load)
        vanet.addCongestionZone(700, 200, 800, 300, 0.6); // Medium congestion intersection (60% network load)
        vanet.addCongestionZone(200, 700, 300, 800, 0.5); // Low congestion intersection (50% network load)
        
        // Add infrastructure (RSUs at intersections)
        vanet.addInfrastructureNode("RSU1", 250, 250, 300);
        vanet.addInfrastructureNode("RSU2", 750, 250, 300);
        vanet.addInfrastructureNode("RSU3", 250, 750, 300);
        vanet.addInfrastructureNode("RSU4", 750, 750, 300);
        
        // Add environmental obstacles (buildings, trees, etc.)
        vanet.addObstacle(100, 100, 200, 200, 0.8); // Large building with 80% signal reduction
        vanet.addObstacle(600, 300, 650, 400, 0.5); // Medium building with 50% signal reduction
        vanet.addObstacle(300, 600, 400, 800, 0.3); // Small building cluster with 30% signal reduction
        
        // Add 200 vehicles with random positions
        Random random = new Random(42); // Fixed seed for reproducibility
        for (int i = 0; i < 200; i++) {
            // Place vehicles on roads
            double x, y, direction, speed;
            
            if (random.nextBoolean()) {
                // On horizontal road
                y = random.nextBoolean() ? 250 : 750;
                x = random.nextDouble() * 1000;
                direction = random.nextBoolean() ? 0 : Math.PI; // East or west
                speed = 8 + (random.nextDouble() * 8); // 8-16 m/s (30-60 km/h)
            } else {
                // On vertical road
                x = random.nextBoolean() ? 250 : 750;
                y = random.nextDouble() * 1000;
                direction = random.nextBoolean() ? Math.PI/2 : 3*Math.PI/2; // North or south
                speed = 8 + (random.nextDouble() * 8); // 8-16 m/s (30-60 km/h)
            }
            
            vanet.addVehicle("V" + i, x, y, direction, speed);
        }
        
        // Run simulation for 3000 steps (300 seconds)
        for (int i = 1; i <= 3000; i++) {
            vanet.simulationStep();
            
            // Print statistics every 5 seconds
            if (i % 50 == 0) {
                System.out.println("Simulation time: " + vanet.getCurrentTime() + "ms");
                System.out.println(vanet.getNetworkStats());
                
                // Print current performance metrics
                System.out.println("Current metrics:");
                System.out.println("  Messages delivered: " + metrics.getTotalMessagesDelivered() + "/" + metrics.getTotalMessagesSent());
                System.out.println("  Average latency: " + metrics.getAverageLatency() + " ms");
                System.out.println("  Delivery ratio: " + String.format("%.2f%%", metrics.getDeliveryRatio() * 100));
                System.out.println("  Packet loss (congestion): " + String.format("%.2f%%", metrics.getCongestionPacketLossRate() * 100));
                System.out.println("  Packet loss (environment): " + String.format("%.2f%%", metrics.getEnvironmentPacketLossRate() * 100));
                System.out.println();
            }
        }
    }
    
    /**
     * Compares and prints metrics between ML and traditional approaches
     * @param mlMetrics Metrics from ML-based optimization
     * @param tradMetrics Metrics from traditional routing
     */
    private static void compareMetrics(PerformanceMetricsCollector mlMetrics, PerformanceMetricsCollector tradMetrics) {
        DecimalFormat df = new DecimalFormat("#.##");
        System.out.println("Metric                           | ML Optimization | Traditional  | Improvement");
        System.out.println("----------------------------------|-----------------|--------------|------------");
        
        // Delivery ratio
        double mlDelivery = mlMetrics.getDeliveryRatio() * 100;
        double tradDelivery = tradMetrics.getDeliveryRatio() * 100;
        double deliveryImprovement = calculateImprovement(mlDelivery, tradDelivery);
        System.out.println("Delivery ratio (%)               | " + 
                         padRight(df.format(mlDelivery), 15) + " | " + 
                         padRight(df.format(tradDelivery), 12) + " | " + 
                         formatImprovement(deliveryImprovement));
        
        // Safety message delivery ratio
        double mlSafetyDelivery = mlMetrics.getSafetyDeliveryRatio() * 100;
        double tradSafetyDelivery = tradMetrics.getSafetyDeliveryRatio() * 100;
        double safetyDeliveryImprovement = calculateImprovement(mlSafetyDelivery, tradSafetyDelivery);
        System.out.println("Safety delivery ratio (%)        | " + 
                         padRight(df.format(mlSafetyDelivery), 15) + " | " + 
                         padRight(df.format(tradSafetyDelivery), 12) + " | " + 
                         formatImprovement(safetyDeliveryImprovement));
        
        // Average latency
        double mlLatency = mlMetrics.getAverageLatency();
        double tradLatency = tradMetrics.getAverageLatency();
        double latencyImprovement = calculateImprovement(tradLatency, mlLatency); // Reversed because lower is better
        System.out.println("Average latency (ms)             | " + 
                         padRight(df.format(mlLatency), 15) + " | " + 
                         padRight(df.format(tradLatency), 12) + " | " + 
                         formatImprovement(latencyImprovement));
        
        // Safety message latency
        double mlSafetyLatency = mlMetrics.getSafetyAverageLatency();
        double tradSafetyLatency = tradMetrics.getSafetyAverageLatency();
        double safetyLatencyImprovement = calculateImprovement(tradSafetyLatency, mlSafetyLatency); // Reversed because lower is better
        System.out.println("Safety latency (ms)              | " + 
                         padRight(df.format(mlSafetyLatency), 15) + " | " + 
                         padRight(df.format(tradSafetyLatency), 12) + " | " + 
                         formatImprovement(safetyLatencyImprovement));
        
        // Network overhead
        double mlOverhead = mlMetrics.getNetworkOverhead();
        double tradOverhead = tradMetrics.getNetworkOverhead();
        double overheadImprovement = calculateImprovement(tradOverhead, mlOverhead); // Reversed because lower is better
        System.out.println("Network overhead (bytes)         | " + 
                         padRight(df.format(mlOverhead), 15) + " | " + 
                         padRight(df.format(tradOverhead), 12) + " | " + 
                         formatImprovement(overheadImprovement));
        
        // Average hop count
        double mlHops = mlMetrics.getAverageHopCount();
        double tradHops = tradMetrics.getAverageHopCount();
        double hopsImprovement = calculateImprovement(tradHops, mlHops); // Reversed because lower is better
        System.out.println("Average hop count                | " + 
                         padRight(df.format(mlHops), 15) + " | " + 
                         padRight(df.format(tradHops), 12) + " | " + 
                         formatImprovement(hopsImprovement));
        
        // Link breaks
        int mlBreaks = mlMetrics.getPathBreaks();
        int tradBreaks = tradMetrics.getPathBreaks();
        double breaksImprovement = calculateImprovement(tradBreaks, mlBreaks); // Reversed because lower is better
        System.out.println("Path breaks                      | " + 
                         padRight("" + mlBreaks, 15) + " | " + 
                         padRight("" + tradBreaks, 12) + " | " + 
                         formatImprovement(breaksImprovement));
        
        // Packet loss due to congestion
        double mlCongestionLoss = mlMetrics.getCongestionPacketLossRate() * 100;
        double tradCongestionLoss = tradMetrics.getCongestionPacketLossRate() * 100;
        double congestionLossImprovement = calculateImprovement(tradCongestionLoss, mlCongestionLoss); // Reversed because lower is better
        System.out.println("Packet loss (congestion) (%)     | " + 
                         padRight(df.format(mlCongestionLoss), 15) + " | " + 
                         padRight(df.format(tradCongestionLoss), 12) + " | " + 
                         formatImprovement(congestionLossImprovement));
        
        // Packet loss due to environment
        double mlEnvironmentLoss = mlMetrics.getEnvironmentPacketLossRate() * 100;
        double tradEnvironmentLoss = tradMetrics.getEnvironmentPacketLossRate() * 100;
        double environmentLossImprovement = calculateImprovement(tradEnvironmentLoss, mlEnvironmentLoss); // Reversed because lower is better
        System.out.println("Packet loss (environment) (%)    | " + 
                         padRight(df.format(mlEnvironmentLoss), 15) + " | " + 
                         padRight(df.format(tradEnvironmentLoss), 12) + " | " + 
                         formatImprovement(environmentLossImprovement));
    }
    
    /**
     * Saves comparison results to a CSV file
     * @param mlMetrics Metrics from ML-based optimization
     * @param tradMetrics Metrics from traditional routing
     */
    private static void saveResultsToCSV(PerformanceMetricsCollector mlMetrics, PerformanceMetricsCollector tradMetrics) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("vanet_comparison_results.csv"))) {
            // Write header
            writer.println("Metric,ML Optimization,Traditional Routing,Improvement (%)");
            
            // Delivery ratios
            writer.println("Overall Delivery Ratio (%)," + 
                         mlMetrics.getDeliveryRatio() * 100 + "," + 
                         tradMetrics.getDeliveryRatio() * 100 + "," + 
                         calculateImprovement(mlMetrics.getDeliveryRatio() * 100, tradMetrics.getDeliveryRatio() * 100));
            
            writer.println("Safety Delivery Ratio (%)," + 
                         mlMetrics.getSafetyDeliveryRatio() * 100 + "," + 
                         tradMetrics.getSafetyDeliveryRatio() * 100 + "," + 
                         calculateImprovement(mlMetrics.getSafetyDeliveryRatio() * 100, tradMetrics.getSafetyDeliveryRatio() * 100));
            
            writer.println("Telemetry Delivery Ratio (%)," + 
                         mlMetrics.getTelemetryDeliveryRatio() * 100 + "," + 
                         tradMetrics.getTelemetryDeliveryRatio() * 100 + "," + 
                         calculateImprovement(mlMetrics.getTelemetryDeliveryRatio() * 100, tradMetrics.getTelemetryDeliveryRatio() * 100));
            
            writer.println("Infotainment Delivery Ratio (%)," + 
                         mlMetrics.getInfotainmentDeliveryRatio() * 100 + "," + 
                         tradMetrics.getInfotainmentDeliveryRatio() * 100 + "," + 
                         calculateImprovement(mlMetrics.getInfotainmentDeliveryRatio() * 100, tradMetrics.getInfotainmentDeliveryRatio() * 100));
            
            // Latency metrics
            writer.println("Average Latency (ms)," + 
                         mlMetrics.getAverageLatency() + "," + 
                         tradMetrics.getAverageLatency() + "," + 
                         calculateImprovement(tradMetrics.getAverageLatency(), mlMetrics.getAverageLatency()));
            
            writer.println("Safety Latency (ms)," + 
                         mlMetrics.getSafetyAverageLatency() + "," + 
                         tradMetrics.getSafetyAverageLatency() + "," + 
                         calculateImprovement(tradMetrics.getSafetyAverageLatency(), mlMetrics.getSafetyAverageLatency()));
            
            writer.println("Telemetry Latency (ms)," + 
                         mlMetrics.getTelemetryAverageLatency() + "," + 
                         tradMetrics.getTelemetryAverageLatency() + "," + 
                         calculateImprovement(tradMetrics.getTelemetryAverageLatency(), mlMetrics.getTelemetryAverageLatency()));
            
            writer.println("Infotainment Latency (ms)," + 
                         mlMetrics.getInfotainmentAverageLatency() + "," + 
                         tradMetrics.getInfotainmentAverageLatency() + "," + 
                         calculateImprovement(tradMetrics.getInfotainmentAverageLatency(), mlMetrics.getInfotainmentAverageLatency()));
            
            // Other metrics
            writer.println("Network Overhead (bytes)," + 
                         mlMetrics.getNetworkOverhead() + "," + 
                         tradMetrics.getNetworkOverhead() + "," + 
                         calculateImprovement(tradMetrics.getNetworkOverhead(), mlMetrics.getNetworkOverhead()));
            
            writer.println("Average Hop Count," + 
                         mlMetrics.getAverageHopCount() + "," + 
                         tradMetrics.getAverageHopCount() + "," + 
                         calculateImprovement(tradMetrics.getAverageHopCount(), mlMetrics.getAverageHopCount()));
            
            writer.println("Path Breaks," + 
                         mlMetrics.getPathBreaks() + "," + 
                         tradMetrics.getPathBreaks() + "," + 
                         calculateImprovement(tradMetrics.getPathBreaks(), mlMetrics.getPathBreaks()));
            
            // Packet loss metrics
            writer.println("Congestion Packet Loss (%)," + 
                         mlMetrics.getCongestionPacketLossRate() * 100 + "," + 
                         tradMetrics.getCongestionPacketLossRate() * 100 + "," + 
                         calculateImprovement(tradMetrics.getCongestionPacketLossRate() * 100, mlMetrics.getCongestionPacketLossRate() * 100));
            
            writer.println("Environment Packet Loss (%)," + 
                         mlMetrics.getEnvironmentPacketLossRate() * 100 + "," + 
                         tradMetrics.getEnvironmentPacketLossRate() * 100 + "," + 
                         calculateImprovement(tradMetrics.getEnvironmentPacketLossRate() * 100, mlMetrics.getEnvironmentPacketLossRate() * 100));
            
            System.out.println("\nResults saved to vanet_comparison_results.csv");
        } catch (IOException e) {
            System.err.println("Error saving results to CSV: " + e.getMessage());
        }
    }
    
    /**
     * Calculates improvement percentage
     * @param newValue New value
     * @param oldValue Old value
     * @return Improvement percentage
     */
    private static double calculateImprovement(double newValue, double oldValue) {
        if (oldValue == 0) return 0;
        return ((newValue - oldValue) / oldValue) * 100;
    }
    
    /**
     * Formats improvement value for display
     * @param value Improvement percentage
     * @return Formatted string
     */
    private static String formatImprovement(double value) {
        DecimalFormat df = new DecimalFormat("+#.##;-#.##");
        return df.format(value) + "%";
    }
    
    /**
     * Pads a string with spaces to reach target length
     * @param s String to pad
     * @param n Target length
     * @return Padded string
     */
    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}

/**
 * Performance metrics collector for VANET optimization
 */
class PerformanceMetricsCollector {
    // Message delivery metrics
    private int totalMessagesSent;
    private int totalMessagesDelivered;
    private int safetyMessagesSent;
    private int safetyMessagesDelivered;
    private int telemetryMessagesSent;
    private int telemetryMessagesDelivered;
    private int infotainmentMessagesSent;
    private int infotainmentMessagesDelivered;
    
    // Latency metrics (in milliseconds)
    private long totalLatency;
    private long safetyLatency;
    private long telemetryLatency;
    private long infotainmentLatency;
    private int latencyCount;
    private int safetyLatencyCount;
    private int telemetryLatencyCount;
    private int infotainmentLatencyCount;
    
    // Network overhead metrics
    private long totalBytesTransmitted;
    private int totalHops;
    private int hopCount;
    
    // Reliability metrics
    private int pathBreaks;
    private int routeRecomputations;
    
    // Packet loss metrics
    private int congestionPacketsLost;
    private int environmentPacketsLost;
    private int totalPacketsAttempted;
    
    // ML performance metrics
    private double initialAvgLinkQuality;
    private double currentAvgLinkQuality;
    private int mlModelUpdates;
    
    // Timestamps to track when metrics were last reset
    private long startTime;
    private long currentTime;
    
    public PerformanceMetricsCollector() {
        resetMetrics();
    }
    
    /**
     * Resets all metrics
     */
    public void resetMetrics() {
        totalMessagesSent = 0;
        totalMessagesDelivered = 0;
        safetyMessagesSent = 0;
        safetyMessagesDelivered = 0;
        telemetryMessagesSent = 0;
        telemetryMessagesDelivered = 0;
        infotainmentMessagesSent = 0;
        infotainmentMessagesDelivered = 0;
        
        totalLatency = 0;
        safetyLatency = 0;
        telemetryLatency = 0;
        infotainmentLatency = 0;
        latencyCount = 0;
        safetyLatencyCount = 0;
        telemetryLatencyCount = 0;
        infotainmentLatencyCount = 0;
        
        totalBytesTransmitted = 0;
        totalHops = 0;
        hopCount = 0;
        
        pathBreaks = 0;
        routeRecomputations = 0;
        
        congestionPacketsLost = 0;
        environmentPacketsLost = 0;
        totalPacketsAttempted = 0;
        
        initialAvgLinkQuality = 0;
        currentAvgLinkQuality = 0;
        mlModelUpdates = 0;
        
        startTime = System.currentTimeMillis();
        currentTime = startTime;
    }
    
    /**
     * Records a message being sent
     * @param messageType Type of message
     * @param size Size of message in bytes
     */
    public void recordMessageSent(int messageType, int size) {
        totalMessagesSent++;
        totalBytesTransmitted += size;
        
        switch (messageType) {
            case VANETOptimizationComparison.SAFETY_CRITICAL:
                safetyMessagesSent++;
                break;
            case VANETOptimizationComparison.TELEMETRY:
                telemetryMessagesSent++;
                break;
            case VANETOptimizationComparison.INFOTAINMENT:
                infotainmentMessagesSent++;
                break;
        }
    }
    
    /**
     * Records a message being delivered
     * @param messageType Type of message
     * @param sentTime Time the message was sent
     * @param deliveryTime Time the message was delivered
     * @param hops Number of hops taken
     * @param size Size of message in bytes
     */
    public void recordMessageDelivered(int messageType, long sentTime, long deliveryTime, int hops, int size) {
        totalMessagesDelivered++;
        long latency = deliveryTime - sentTime;
        totalLatency += latency;
        latencyCount++;
        
        totalHops += hops;
        hopCount++;
        
        switch (messageType) {
            case VANETOptimizationComparison.SAFETY_CRITICAL:
                safetyMessagesDelivered++;
                safetyLatency += latency;
                safetyLatencyCount++;
                break;
            case VANETOptimizationComparison.TELEMETRY:
                telemetryMessagesDelivered++;
                telemetryLatency += latency;
                telemetryLatencyCount++;
                break;
            case VANETOptimizationComparison.INFOTAINMENT:
                infotainmentMessagesDelivered++;
                infotainmentLatency += latency;
                infotainmentLatencyCount++;
                break;
        }
    }
    
    /**
     * Records a packet loss due to congestion
     * @param messageType Type of message that was lost
     * @param size Size of the message in bytes
     */
    public void recordCongestionPacketLoss(int messageType, int size) {
        congestionPacketsLost++;
        totalPacketsAttempted++;
    }
    
    /**
     * Records a packet loss due to environmental factors
     * @param messageType Type of message that was lost
     * @param size Size of the message in bytes
     */
    public void recordEnvironmentPacketLoss(int messageType, int size) {
        environmentPacketsLost++;
        totalPacketsAttempted++;
    }
    
    /**
     * Records a packet transmission attempt (successful or not)
     */
    public void recordPacketAttempt() {
        totalPacketsAttempted++;
    }
    
    /**
     * Records a path break
     */
    public void recordPathBreak() {
        pathBreaks++;
    }
    
    /**
     * Records a route recomputation
     */
    public void recordRouteRecomputation() {
        routeRecomputations++;
    }
    
    /**
     * Records link quality values
     * @param avgQuality Average link quality
     */
    public void recordLinkQuality(double avgQuality) {
        if (initialAvgLinkQuality == 0) {
            initialAvgLinkQuality = avgQuality;
        }
        currentAvgLinkQuality = avgQuality;
    }
    
    /**
     * Records an ML model update
     */
    public void recordModelUpdate() {
        mlModelUpdates++;
    }
    
    /**
     * Records the current simulation time
     * @param time Current simulation time
     */
    public void updateTime(long time) {
        currentTime = time;
    }
    
    /**
     * Gets the overall message delivery ratio
     * @return Delivery ratio between 0 and 1
     */
    public double getDeliveryRatio() {
        return totalMessagesSent > 0 ? (double) totalMessagesDelivered / totalMessagesSent : 0;
    }
    
    /**
     * Gets the safety message delivery ratio
     * @return Safety delivery ratio between 0 and 1
     */
    public double getSafetyDeliveryRatio() {
        return safetyMessagesSent > 0 ? (double) safetyMessagesDelivered / safetyMessagesSent : 0;
    }
    
    /**
     * Gets the telemetry message delivery ratio
     * @return Telemetry delivery ratio between 0 and 1
     */
    public double getTelemetryDeliveryRatio() {
        return telemetryMessagesSent > 0 ? (double) telemetryMessagesDelivered / telemetryMessagesSent : 0;
    }
    
    /**
     * Gets the infotainment message delivery ratio
     * @return Infotainment delivery ratio between 0 and 1
     */
    public double getInfotainmentDeliveryRatio() {
        return infotainmentMessagesSent > 0 ? (double) infotainmentMessagesDelivered / infotainmentMessagesSent : 0;
    }
    
    /**
     * Gets the congestion-based packet loss rate
     * @return Packet loss rate between 0 and 1
     */
    public double getCongestionPacketLossRate() {
        return totalPacketsAttempted > 0 ? (double) congestionPacketsLost / totalPacketsAttempted : 0;
    }
    
    /**
     * Gets the environment-based packet loss rate
     * @return Packet loss rate between 0 and 1
     */
    public double getEnvironmentPacketLossRate() {
        return totalPacketsAttempted > 0 ? (double) environmentPacketsLost / totalPacketsAttempted : 0;
    }
    
    /**
     * Gets the average latency across all messages
     * @return Average latency in milliseconds
     */
    public double getAverageLatency() {
        return latencyCount > 0 ? (double) totalLatency / latencyCount : 0;
    }
    
    /**
     * Gets the average latency for safety messages
     * @return Average safety latency in milliseconds
     */
    public double getSafetyAverageLatency() {
        return safetyLatencyCount > 0 ? (double) safetyLatency / safetyLatencyCount : 0;
    }
    
    /**
     * Gets the average latency for telemetry messages
     * @return Average telemetry latency in milliseconds
     */
    public double getTelemetryAverageLatency() {
        return telemetryLatencyCount > 0 ? (double) telemetryLatency / telemetryLatencyCount : 0;
    }
    
    /**
     * Gets the average latency for infotainment messages
     * @return Average infotainment latency in milliseconds
     */
    public double getInfotainmentAverageLatency() {
        return infotainmentLatencyCount > 0 ? (double) infotainmentLatency / infotainmentLatencyCount : 0;
    }
    
    /**
     * Gets the network overhead
     * @return Total bytes transmitted
     */
    public long getNetworkOverhead() {
        return totalBytesTransmitted;
    }
    
    /**
     * Gets the average hop count
     * @return Average number of hops per delivered message
     */
    public double getAverageHopCount() {
        return hopCount > 0 ? (double) totalHops / hopCount : 0;
    }
    
    /**
     * Gets the number of path breaks
     * @return Number of path breaks
     */
    public int getPathBreaks() {
        return pathBreaks;
    }
    
    /**
     * Gets the number of route recomputations
     * @return Number of route recomputations
     */
    public int getRouteRecomputations() {
        return routeRecomputations;
    }
    
    /**
     * Gets the link quality improvement
     * @return Percentage improvement in link quality
     */
    public double getLinkQualityImprovement() {
        return initialAvgLinkQuality > 0 ? 
            ((currentAvgLinkQuality - initialAvgLinkQuality) / initialAvgLinkQuality) * 100 : 0;
    }
    
    /**
     * Gets the total number of messages sent
     * @return Total messages sent
     */
    public int getTotalMessagesSent() {
        return totalMessagesSent;
    }
    
    /**
     * Gets the total number of messages delivered
     * @return Total messages delivered
     */
    public int getTotalMessagesDelivered() {
        return totalMessagesDelivered;
    }
}

/**
 * VANET Simulator that can use either ML-optimized or traditional routing
 * Now includes congestion-based and environment-based packet loss with Nakagami fading model
 */
class VANETSimulator {
    // Network components
    private Map<String, VehicleNode> vehicles;
    private Map<String, InfrastructureNode> infrastructure;
    private List<Message> messageQueue;
    private Set<String> deliveredMessages;
    
    // Environment parameters
    private double xBoundary;
    private double yBoundary;
    private double transmissionRange;
    private RoadMap roadMap;
    
    // Learning parameters
    private double learningRate;
    private double discountFactor;
    private DeepQNetwork dqn;
    private boolean useMLOptimization;
    
    // Congestion and environment models
    private List<CongestionZone> congestionZones;
    private List<Obstacle> obstacles;
    private Random random;
    
    // Nakagami fading model parameters
    private double nakagamiShapeFactor; // m parameter (m ≥ 0.5)
    private double nakagamiSpreadFactor; // Ω parameter (controls spread/variance)
    
    // Simulation time
    private long currentTime;
    private long timeStep;
    
    // Performance metrics
    private PerformanceMetricsCollector metrics;
    
    /**
     * Constructor for the VANET Simulator
     * @param xBoundary Maximum x-coordinate of the simulation area
     * @param yBoundary Maximum y-coordinate of the simulation area
     * @param transmissionRange Radio transmission range of vehicles in meters
     * @param learningRate Learning rate for the ML algorithm
     * @param discountFactor Discount factor for future rewards
     * @param useMLOptimization Whether to use ML optimization
     * @param metrics Collector for performance metrics
     */
    public VANETSimulator(double xBoundary, double yBoundary, double transmissionRange,
                         double learningRate, double discountFactor, boolean useMLOptimization,
                         PerformanceMetricsCollector metrics) {
        this.vehicles = new HashMap<>();
        this.infrastructure = new HashMap<>();
        this.messageQueue = new ArrayList<>();
        this.deliveredMessages = new HashSet<>();
        this.congestionZones = new ArrayList<>();
        this.obstacles = new ArrayList<>();
        
        this.xBoundary = xBoundary;
        this.yBoundary = yBoundary;
        this.transmissionRange = transmissionRange;
        this.roadMap = new RoadMap(xBoundary, yBoundary);
        
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.useMLOptimization = useMLOptimization;
        
        this.currentTime = 0;
        this.timeStep = 100; // 100ms per step
        
        this.metrics = metrics;
        this.random = new Random(42); // Fixed seed for reproducibility
        
        // Initialize Nakagami fading model parameters
        // Urban: m=1 (Rayleigh), Suburban: m=2, Highway: m=3
        this.nakagamiShapeFactor = 1.5; // Default (can be adjusted based on environment)
        this.nakagamiSpreadFactor = 1.0; // Default (normalized power)
        
        // Initialize deep Q-network for reinforcement learning
        this.dqn = new DeepQNetwork(learningRate);
        
        System.out.println("VANET Simulator initialized with " + 
                         (useMLOptimization ? "ML optimization" : "traditional routing") +
                         " and realistic packet loss models");
    }
    
    /**
     * Adds a congestion zone to the simulation
     * @param x1 Left x-coordinate
     * @param y1 Top y-coordinate
     * @param x2 Right x-coordinate
     * @param y2 Bottom y-coordinate
     * @param networkLoad Network load factor (0-1)
     */
    public void addCongestionZone(double x1, double y1, double x2, double y2, double networkLoad) {
        congestionZones.add(new CongestionZone(x1, y1, x2, y2, networkLoad));
    }
    
    /**
     * Adds an obstacle to the simulation
     * @param x1 Left x-coordinate
     * @param y1 Top y-coordinate
     * @param x2 Right x-coordinate
     * @param y2 Bottom y-coordinate
     * @param attenuation Signal attenuation factor (0-1)
     */
    public void addObstacle(double x1, double y1, double x2, double y2, double attenuation) {
        obstacles.add(new Obstacle(x1, y1, x2, y2, attenuation));
    }
    
    /**
     * Adds a vehicle to the network
     * @param id Unique vehicle identifier
     * @param x Initial x-coordinate
     * @param y Initial y-coordinate
     * @param direction Initial movement direction in radians
     * @param speed Initial speed in meters per second
     * @return The created vehicle node
     */
    public VehicleNode addVehicle(String id, double x, double y, double direction, double speed) {
        VehicleNode vehicle = new VehicleNode(id, x, y, direction, speed, transmissionRange);
        
        // Configure application traffic patterns
        vehicle.addApplication(new Application(VANETOptimizationComparison.SAFETY_CRITICAL, 
                                             VANETOptimizationComparison.SAFETY_PAYLOAD_SIZE, 
                                             VANETOptimizationComparison.SAFETY_INTERVAL));
        
        vehicle.addApplication(new Application(VANETOptimizationComparison.TELEMETRY, 
                                             VANETOptimizationComparison.TELEMETRY_PAYLOAD_SIZE, 
                                             VANETOptimizationComparison.TELEMETRY_INTERVAL));
        
        vehicle.addApplication(new Application(VANETOptimizationComparison.INFOTAINMENT, 
                                             VANETOptimizationComparison.INFOTAINMENT_PAYLOAD_SIZE, 
                                             VANETOptimizationComparison.INFOTAINMENT_INTERVAL));
        
        vehicles.put(id, vehicle);
        return vehicle;
    }
    
    /**
     * Adds an infrastructure node (RSU - Road Side Unit) to the network
     * @param id Unique infrastructure identifier
     * @param x X-coordinate (fixed position)
     * @param y Y-coordinate (fixed position)
     * @param transmissionRange Radio transmission range in meters
     * @return The created infrastructure node
     */
    public InfrastructureNode addInfrastructureNode(String id, double x, double y, double transmissionRange) {
        InfrastructureNode node = new InfrastructureNode(id, x, y, transmissionRange);
        infrastructure.put(id, node);
        return node;
    }
    
    /**
     * Adds a road to the simulation environment
     * @param startX Starting x-coordinate
     * @param startY Starting y-coordinate
     * @param endX Ending x-coordinate
     * @param endY Ending y-coordinate
     * @param lanes Number of lanes
     * @param speedLimit Speed limit in meters per second
     * @param environmentType Type of environment (URBAN, SUBURBAN, HIGHWAY)
     */
    public void addRoad(double startX, double startY, double endX, double endY, int lanes, double speedLimit, int environmentType) {
        roadMap.addRoad(startX, startY, endX, endY, lanes, speedLimit, environmentType);
    }
    
    /**
     * Main simulation step - progresses the simulation by one time step
     */
    public void simulationStep() {
        currentTime += timeStep;
        metrics.updateTime(currentTime);
        
        // Update vehicle positions
        for (VehicleNode vehicle : vehicles.values()) {
            // Apply road constraints to vehicle movement
            Road currentRoad = roadMap.getNearestRoad(vehicle.getX(), vehicle.getY());
            if (currentRoad != null) {
                vehicle.alignToRoad(currentRoad);
                vehicle.setMaxSpeed(currentRoad.getSpeedLimit());
            }
            
            // Move the vehicle
            vehicle.updatePosition(timeStep);
            
            // Wrap around when vehicles leave the simulation area
            if (vehicle.getX() < 0) vehicle.setX(xBoundary);
            if (vehicle.getX() > xBoundary) vehicle.setX(0);
            if (vehicle.getY() < 0) vehicle.setY(yBoundary);
            if (vehicle.getY() > yBoundary) vehicle.setY(0);
            
            // Generate application messages
            for (Application app : vehicle.getApplications()) {
                if (currentTime - app.getLastSentTime() >= app.getInterval()) {
                    // Time to send a new message from this application
                    
                    // Choose a destination - for this simulation:
                    // - Safety messages are broadcast
                    // - Telemetry messages go to the nearest RSU
                    // - Infotainment messages go to a random vehicle
                    String destination = null;
                    
                    if (app.getType() != VANETOptimizationComparison.SAFETY_CRITICAL) {
                        if (app.getType() == VANETOptimizationComparison.TELEMETRY) {
                            // Find nearest RSU
                            destination = findNearestRSU(vehicle);
                        } else {
                            // Find random vehicle (not self)
                            destination = findRandomVehicle(vehicle.getId());
                        }
                    }
                    
                    Message msg = new Message(
                        UUID.randomUUID().toString(),
                        vehicle.getId(),
                        destination,
                        app.getType(),
                        app.getPayloadSize(),
                        currentTime
                    );
                    
                    messageQueue.add(msg);
                    app.setLastSentTime(currentTime);
                    
                    // Record metrics
                    metrics.recordMessageSent(app.getType(), app.getPayloadSize());
                }
            }
        }
        
        // Update network topology (create/remove links based on distance)
        updateNetworkTopology();
        
        // Process message queue
        processMessages();
        
        // Train the reinforcement learning model
        if (useMLOptimization && currentTime % 1000 == 0) { // Train every 1 second of simulation time
            trainModel();
            metrics.recordModelUpdate();
        }
        
        // Record current average link quality
        double avgLinkQuality = calculateAverageLinkQuality();
        metrics.recordLinkQuality(avgLinkQuality);
    }
    
    /**
     * Updates the network topology based on node positions
     */
    private void updateNetworkTopology() {
        // Clear all existing links
        for (VehicleNode vehicle : vehicles.values()) {
            vehicle.clearLinks();
        }
        for (InfrastructureNode infra : infrastructure.values()) {
            infra.clearLinks();
        }
        
        // Create vehicle-to-vehicle (V2V) links
        for (VehicleNode v1 : vehicles.values()) {
            for (VehicleNode v2 : vehicles.values()) {
                if (v1.getId().equals(v2.getId())) continue; // Skip self
                
                double distance = calculateDistance(v1.getX(), v1.getY(), v2.getX(), v2.getY());
                if (distance <= v1.getTransmissionRange()) {
                    // Get environment type and adjust Nakagami parameters
                    Road nearestRoad = roadMap.getNearestRoad(v1.getX(), v1.getY());
                    int environmentType = VANETOptimizationComparison.URBAN; // Default to urban
                    if (nearestRoad != null) {
                        environmentType = nearestRoad.getEnvironmentType();
                    }
                    
                    // Adjust Nakagami shape factor based on environment
                    double shapeFactor = 1.0; // Rayleigh fading (urban default)
                    switch (environmentType) {
                        case VANETOptimizationComparison.URBAN:
                            shapeFactor = 1.0; // Rayleigh fading (more severe)
                            break;
                        case VANETOptimizationComparison.SUBURBAN:
                            shapeFactor = 2.0; // Moderate fading
                            break;
                        case VANETOptimizationComparison.HIGHWAY:
                            shapeFactor = 3.0; // Less severe fading
                            break;
                    }
                    
                    // Check for obstacles in the path and accumulate attenuation
                    double totalAttenuation = calculateObstacleAttenuation(v1.getX(), v1.getY(), v2.getX(), v2.getY());
                    boolean hasLineOfSight = totalAttenuation < 0.8; // Line of sight if attenuation is not severe
                    
                    // Calculate signal strength with Nakagami fading
                    double signalStrength = calculateNakagamiSignalStrength(
                        distance, 
                        v1.getTransmissionRange(), 
                        shapeFactor, 
                        totalAttenuation
                    );
                    
                    double reliability = hasLineOfSight ? signalStrength : signalStrength * 0.6; // Reduced if obstructed
                    
                    // Use the relative speed to predict link stability
                    double relativeSpeed = calculateRelativeSpeed(v1, v2);
                    double linkDuration = estimateLinkDuration(distance, relativeSpeed, v1.getTransmissionRange());
                    
                    // Calculate quality based on multiple factors
                    double linkQuality;
                    if (useMLOptimization) {
                        // ML approach: use more sophisticated formula with learning
                        linkQuality = calculateLinkQuality(signalStrength, reliability, linkDuration, relativeSpeed);
                    } else {
                        // Traditional approach: simple formula based on signal strength
                        linkQuality = signalStrength;
                    }
                    
                    // Calculate congestion effect at this location
                    double congestionFactor = calculateCongestionFactor(v1.getX(), v1.getY());
                    reliability *= (1.0 - congestionFactor); // Reduce reliability based on congestion
                    
                    // Create bidirectional links (with potentially different properties)
                    v1.addLink(v2.getId(), linkQuality, reliability, linkDuration);
                    v2.addLink(v1.getId(), linkQuality, reliability, linkDuration);
                }
            }
        }
        
        // Create vehicle-to-infrastructure (V2I) links
        for (VehicleNode vehicle : vehicles.values()) {
            for (InfrastructureNode infra : infrastructure.values()) {
                double distance = calculateDistance(vehicle.getX(), vehicle.getY(), 
                                                  infra.getX(), infra.getY());
                                                  
                if (distance <= infra.getTransmissionRange()) {
                    // Get environment type for RSU location
                    Road nearestRoad = roadMap.getNearestRoad(infra.getX(), infra.getY());
                    int environmentType = VANETOptimizationComparison.URBAN; // Default
                    if (nearestRoad != null) {
                        environmentType = nearestRoad.getEnvironmentType();
                    }
                    
                    // Adjust Nakagami parameters for RSU links (usually more reliable)
                    double shapeFactor = 1.5; // Default
                    switch (environmentType) {
                        case VANETOptimizationComparison.URBAN:
                            shapeFactor = 1.5; // Less severe than V2V in urban
                            break;
                        case VANETOptimizationComparison.SUBURBAN:
                            shapeFactor = 2.5; // Less severe
                            break;
                        case VANETOptimizationComparison.HIGHWAY:
                            shapeFactor = 3.5; // Least severe
                            break;
                    }
                    
                    // Check for obstacles between vehicle and RSU
                    double totalAttenuation = calculateObstacleAttenuation(
                        vehicle.getX(), vehicle.getY(), infra.getX(), infra.getY());
                    
                    // RSUs typically have better signal strength and reliability
                    double signalStrength = calculateNakagamiSignalStrength(
                        distance, 
                        infra.getTransmissionRange(), 
                        shapeFactor, 
                        totalAttenuation
                    );
                    
                    // RSUs have higher baseline reliability
                    double reliability = 0.9 * signalStrength; 
                    
                    // Calculate congestion at RSU location
                    double congestionFactor = calculateCongestionFactor(infra.getX(), infra.getY());
                    reliability *= (1.0 - congestionFactor * 0.5); // RSUs handle congestion better
                    
                    // Create bidirectional links
                    vehicle.addLink(infra.getId(), signalStrength, reliability, Double.MAX_VALUE);
                    infra.addLink(vehicle.getId(), signalStrength, reliability, Double.MAX_VALUE);
                }
            }
        }
    }
    
    /**
     * Processes messages in the queue according to priority
     */
    private void processMessages() {
        // Sort messages by priority (message type)
        Collections.sort(messageQueue, Comparator.comparingInt(Message::getType));
        
        // Create a list to store messages that should be kept
        List<Message> messagesToKeep = new ArrayList<>();
        
        for (Message message : messageQueue) {
            // Skip if already delivered
            if (deliveredMessages.contains(message.getId())) {
                continue;
            }
            
            // Get source node
            NetworkNode sourceNode = getNodeById(message.getSource());
            if (sourceNode == null) {
                // Source node no longer exists, drop the message
                continue;
            }
            
            boolean shouldKeep = true;
            
            // Handle based on destination
            if (message.getDestination() == null) {
                // Broadcast message (safety critical)
                int hops = broadcastMessage(message, sourceNode);
                deliveredMessages.add(message.getId());
                
                // Record delivered broadcast message
                metrics.recordMessageDelivered(
                    message.getType(),
                    message.getCreationTime(),
                    currentTime,
                    hops,
                    message.getSize()
                );
                
                shouldKeep = false;
            } else {
                // Unicast message (telemetry or infotainment)
                NetworkNode destNode = getNodeById(message.getDestination());
                
                if (destNode != null) {
                    // Find path using different routing strategies
                    List<String> path;
                    if (useMLOptimization) {
                        path = findOptimalPath(sourceNode.getId(), destNode.getId(), message.getType());
                    } else {
                        path = findTraditionalPath(sourceNode.getId(), destNode.getId());
                    }
                    
                    if (path.size() > 1) {
                        // Forward along the path
                        DeliveryResult result = forwardMessage(message, path);
                        if (result.delivered) {
                            deliveredMessages.add(message.getId());
                            
                            // Record delivered unicast message
                            metrics.recordMessageDelivered(
                                message.getType(),
                                message.getCreationTime(),
                                currentTime,
                                result.hops,
                                message.getSize()
                            );
                            
                            shouldKeep = false;
                        } else if (result.pathBroken) {
                            // Path was broken, record it
                            metrics.recordPathBreak();
                            metrics.recordRouteRecomputation();
                        }
                    }
                }
            }
            
            if (shouldKeep) {
                messagesToKeep.add(message);
            }
        }
        
        // Replace message queue with messages to keep
        messageQueue.clear();
        messageQueue.addAll(messagesToKeep);
    }
    
    /**
     * Broadcasts a message to all nodes within range of the source
     * @param message The message to broadcast
     * @param source The source node
     * @return Number of hops used (1 for direct broadcast)
     */
    private int broadcastMessage(Message message, NetworkNode source) {
        // For safety messages, use flooding with duplicate suppression
        int coveredNodes = 0;
        int successfulDeliveries = 0;
        
        if (message.getType() == VANETOptimizationComparison.SAFETY_CRITICAL) {
            // Simple flooding for safety messages
            for (String neighborId : source.getLinks().keySet()) {
                Link link = source.getLinks().get(neighborId);
                boolean delivered = true;
                
                // Account for congestion-based packet loss
                double congestionFactor = calculateCongestionFactor(source.getX(), source.getY());
                if (random.nextDouble() < congestionFactor * 0.7) {  // Max 70% loss chance under full congestion
                    delivered = false;
                    metrics.recordCongestionPacketLoss(message.getType(), message.getSize());
                    // System.out.println("Broadcast message " + message.getId() + " lost to " + neighborId + " due to congestion");
                }
                
                // Account for environment-based packet loss
                if (delivered && random.nextDouble() > link.getReliability()) {
                    delivered = false;
                    metrics.recordEnvironmentPacketLoss(message.getType(), message.getSize());
                    // System.out.println("Broadcast message " + message.getId() + " lost to " + neighborId + " due to environmental factors");
                }
                
                metrics.recordPacketAttempt();
                coveredNodes++;
                
                if (delivered) {
                    successfulDeliveries++;
                    // System.out.println("Safety message " + message.getId() + " from " + source.getId() + 
                    //                  " delivered to " + neighborId);
                }
            }
        } else {
            // Use more efficient broadcasting
            Set<String> selectedRebroadcasters = selectRebroadcastNodes(source);
            
            for (String nodeId : selectedRebroadcasters) {
                Link link = source.getLinks().get(nodeId);
                boolean delivered = true;
                
                // Account for congestion-based packet loss
                double congestionFactor = calculateCongestionFactor(source.getX(), source.getY());
                if (random.nextDouble() < congestionFactor * 0.7) {
                    delivered = false;
                    metrics.recordCongestionPacketLoss(message.getType(), message.getSize());
                }
                
                // Account for environment-based packet loss
                if (delivered && random.nextDouble() > link.getReliability()) {
                    delivered = false;
                    metrics.recordEnvironmentPacketLoss(message.getType(), message.getSize());
                }
                
                metrics.recordPacketAttempt();
                coveredNodes++;
                
                if (delivered) {
                    successfulDeliveries++;
                    // System.out.println("Message " + message.getId() + " from " + source.getId() + 
                    //                  " forwarded to " + nodeId + " for rebroadcast");
                }
            }
        }
        
        return successfulDeliveries > 0 ? 1 : 0; // Count as successful if at least one neighbor received it
    }
    
    /**
     * Selects a subset of neighbors for efficient rebroadcasting
     * @param source The source node
     * @return Set of node IDs selected for rebroadcasting
     */
    private Set<String> selectRebroadcastNodes(NetworkNode source) {
        Set<String> selectedNodes = new HashSet<>();
        Set<String> neighbors = source.getLinks().keySet();
        
        // Skip if no neighbors
        if (neighbors.isEmpty()) {
            return selectedNodes;
        }
        
        // Calculate coverage for each neighbor
        Map<String, Set<String>> coverage = new HashMap<>();
        for (String neighborId : neighbors) {
            NetworkNode neighbor = getNodeById(neighborId);
            if (neighbor != null) {
                coverage.put(neighborId, new HashSet<>(neighbor.getLinks().keySet()));
            }
        }
        
        // Use greedy algorithm to select minimum set of rebroadcasters
        // that cover the 2-hop neighborhood
        Set<String> coveredNodes = new HashSet<>(neighbors);
        coveredNodes.add(source.getId()); // Source already covered
        
        while (!neighbors.isEmpty()) {
            String bestNode = null;
            int maxNewCovered = 0;
            
            for (String neighborId : new HashSet<>(neighbors)) {
                Set<String> neighborCoverage = coverage.get(neighborId);
                if (neighborCoverage == null) continue;
                
                int newCovered = 0;
                for (String coveredId : neighborCoverage) {
                    if (!coveredNodes.contains(coveredId)) {
                        newCovered++;
                    }
                }
                
                if (newCovered > maxNewCovered) {
                    maxNewCovered = newCovered;
                    bestNode = neighborId;
                }
            }
            
            if (bestNode == null || maxNewCovered == 0) {
                break; // No improvement possible
            }
            
            // Add best node to selected set
            selectedNodes.add(bestNode);
            neighbors.remove(bestNode);
            
            // Update covered nodes
            coveredNodes.addAll(coverage.get(bestNode));
        }
        
        return selectedNodes;
    }
    
    /**
     * Forwards a message along a specific path
     * @param message The message to forward
     * @param path The path to follow
     * @return Result of the forwarding attempt
     */
    private DeliveryResult forwardMessage(Message message, List<String> path) {
        // In a real implementation, this would actually deliver the message
        // hop by hop, with acknowledgements, retransmissions, etc.
        
        // For simplicity, we'll just check if the path is currently valid
        boolean pathValid = true;
        int successfulHops = 0;
        
        for (int i = 0; i < path.size() - 1; i++) {
            NetworkNode current = getNodeById(path.get(i));
            String nextHopId = path.get(i + 1);
            
            if (current == null || !current.getLinks().containsKey(nextHopId)) {
                pathValid = false;
                break; // Link broken
            }
            
            // Get the link to the next hop
            Link link = current.getLinks().get(nextHopId);
            
            // Check for packet loss at this hop
            boolean packetLost = false;
            
            // Check for congestion-based packet loss
            double congestionFactor = calculateCongestionFactor(current.getX(), current.getY());
            if (random.nextDouble() < congestionFactor * 0.8) {  // Max 80% loss chance under full congestion
                packetLost = true;
                metrics.recordCongestionPacketLoss(message.getType(), message.getSize());
                // System.out.println("Message " + message.getId() + " lost at hop " + i + " due to congestion");
            }
            
            // Check for environment-based packet loss using link reliability
            if (!packetLost && random.nextDouble() > link.getReliability()) {
                packetLost = true;
                metrics.recordEnvironmentPacketLoss(message.getType(), message.getSize());
                // System.out.println("Message " + message.getId() + " lost at hop " + i + " due to environmental factors");
            }
            
            if (packetLost) {
                // Record a packet loss and stop forwarding
                metrics.recordPacketAttempt();
                return new DeliveryResult(false, false, successfulHops);
            }
            
            // This hop was successful
            successfulHops++;
            metrics.recordPacketAttempt();
        }
        
        if (pathValid) {
            System.out.println("Message " + message.getId() + " delivered via path: " + 
                             String.join(" → ", path));
            return new DeliveryResult(true, false, path.size() - 1);
        } else {
            return new DeliveryResult(false, true, successfulHops);
        }
    }
    
    /**
     * Finds the optimal path between two nodes using ML-enhanced routing
     * @param sourceId Source node ID
     * @param destId Destination node ID
     * @param messageType Type of message (affects routing priorities)
     * @return List of node IDs representing the path
     */
    public List<String> findOptimalPath(String sourceId, String destId, int messageType) {
        // Initialize data structures for Dijkstra's algorithm
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previousNodes = new HashMap<>();
        PriorityQueue<NodeDistancePair> queue = new PriorityQueue<>();
        Set<String> visited = new HashSet<>();
        
        // Get all node IDs in the network
        Set<String> allNodes = new HashSet<>();
        allNodes.addAll(vehicles.keySet());
        allNodes.addAll(infrastructure.keySet());
        // Initialize distances to infinity except for source
        for (String nodeId : allNodes) {
            distances.put(nodeId, Double.POSITIVE_INFINITY);
        }
        distances.put(sourceId, 0.0);
        queue.add(new NodeDistancePair(sourceId, 0.0));
        
        while (!queue.isEmpty()) {
            NodeDistancePair current = queue.poll();
            String currentId = current.getNodeId();
            
            if (currentId.equals(destId)) {
                break; // Found destination
            }
            
            if (visited.contains(currentId)) {
                continue;
            }
            
            visited.add(currentId);
            
            NetworkNode currentNode = getNodeById(currentId);
            if (currentNode == null) continue;
            
            // Check all neighbors
            for (Map.Entry<String, Link> entry : currentNode.getLinks().entrySet()) {
                String neighborId = entry.getKey();
                Link link = entry.getValue();
                
                // Calculate edge weight based on link quality and message type
                double weight = calculateMLEdgeWeight(link, messageType);
                
                double newDistance = distances.get(currentId) + weight;
                
                if (newDistance < distances.getOrDefault(neighborId, Double.POSITIVE_INFINITY)) {
                    distances.put(neighborId, newDistance);
                    previousNodes.put(neighborId, currentId);
                    queue.add(new NodeDistancePair(neighborId, newDistance));
                }
            }
        }
        
        // Reconstruct path
        List<String> path = new ArrayList<>();
        String current = destId;
        
        while (current != null) {
            path.add(0, current);
            current = previousNodes.get(current);
        }
        
        // Check if we found a path
        if (path.size() <= 1 || !path.get(0).equals(sourceId)) {
            return new ArrayList<>(); // No path found
        }
        
        return path;
    }
    
    /**
     * Finds a path using traditional (non-ML) routing
     * @param sourceId Source node ID
     * @param destId Destination node ID
     * @return List of node IDs representing the path
     */
    public List<String> findTraditionalPath(String sourceId, String destId) {
        // Traditional routing just uses hop count and signal strength
        // without considering message type, link stability, etc.
        
        // Initialize data structures for Dijkstra's algorithm
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previousNodes = new HashMap<>();
        PriorityQueue<NodeDistancePair> queue = new PriorityQueue<>();
        Set<String> visited = new HashSet<>();
        
        // Get all node IDs in the network
        Set<String> allNodes = new HashSet<>();
        allNodes.addAll(vehicles.keySet());
        allNodes.addAll(infrastructure.keySet());
        
        // Initialize distances to infinity except for source
        for (String nodeId : allNodes) {
            distances.put(nodeId, Double.POSITIVE_INFINITY);
        }
        distances.put(sourceId, 0.0);
        queue.add(new NodeDistancePair(sourceId, 0.0));
        
        while (!queue.isEmpty()) {
            NodeDistancePair current = queue.poll();
            String currentId = current.getNodeId();
            
            if (currentId.equals(destId)) {
                break; // Found destination
            }
            
            if (visited.contains(currentId)) {
                continue;
            }
            
            visited.add(currentId);
            
            NetworkNode currentNode = getNodeById(currentId);
            if (currentNode == null) continue;
            
            // Check all neighbors
            for (Map.Entry<String, Link> entry : currentNode.getLinks().entrySet()) {
                String neighborId = entry.getKey();
                Link link = entry.getValue();
                
                // Traditional routing just uses hop count and basic signal strength
                // Each hop has a base cost of 1, modified by signal strength
                double weight = 1.0 / link.getReliability();
                
                double newDistance = distances.get(currentId) + weight;
                
                if (newDistance < distances.getOrDefault(neighborId, Double.POSITIVE_INFINITY)) {
                    distances.put(neighborId, newDistance);
                    previousNodes.put(neighborId, currentId);
                    queue.add(new NodeDistancePair(neighborId, newDistance));
                }
            }
        }
        
        // Reconstruct path
        List<String> path = new ArrayList<>();
        String current = destId;
        
        while (current != null) {
            path.add(0, current);
            current = previousNodes.get(current);
        }
        
        // Check if we found a path
        if (path.size() <= 1 || !path.get(0).equals(sourceId)) {
            return new ArrayList<>(); // No path found
        }
        
        return path;
    }
    
    /**
     * Calculates edge weight for ML-based path finding based on link quality and message type
     * @param link The link to evaluate
     * @param messageType Type of message
     * @return Edge weight (lower is better)
     */
    private double calculateMLEdgeWeight(Link link, int messageType) {
        // Base weight is inverse of link quality (higher quality = lower weight)
        double weight = 1.0 / Math.max(0.1, link.getQuality());
        
        // Adjust based on message type
        switch (messageType) {
            case VANETOptimizationComparison.SAFETY_CRITICAL:
                // For safety messages, prioritize reliability and low delay
                weight = weight / (link.getReliability() * 2.0);
                break;
                
            case VANETOptimizationComparison.TELEMETRY:
                // For telemetry, balance reliability and link duration
                weight = weight / (link.getReliability() * link.getDuration() / 30.0);
                break;
                
            case VANETOptimizationComparison.INFOTAINMENT:
                // For infotainment, prioritize stable, long-duration links
                weight = weight / (link.getDuration() / 60.0);
                break;
                
            default:
                // Default weighting
                break;
        }
        
        return weight;
    }
    
    /**
     * Trains the ML model based on observed network performance
     */
    private void trainModel() {
        // Collect training samples from recent network performance
        List<TrainingSample> samples = collectTrainingSamples();
        
        // Skip if not enough samples
        if (samples.size() < 10) return;
        
        // Train the deep Q-network
        dqn.trainOnBatch(samples);
        
        // Update link qualities using the trained model
        updateLinkQualities();
    }
    
    /**
     * Collects training samples from recent network performance
     * @return List of training samples
     */
    private List<TrainingSample> collectTrainingSamples() {
        List<TrainingSample> samples = new ArrayList<>();
        
        // In a real implementation, this would collect actual performance metrics
        // For simulation, we'll generate synthetic samples
        
        for (VehicleNode vehicle : vehicles.values()) {
            for (Map.Entry<String, Link> entry : vehicle.getLinks().entrySet()) {
                String neighborId = entry.getKey();
                Link link = entry.getValue();
                
                NetworkNode neighbor = getNodeById(neighborId);
                if (neighbor == null) continue;
                
                // Create feature vector for this link
                double[] features = {
                    link.getReliability(),
                    link.getDuration() / 60.0, // Normalized by 1 minute
                    calculateRelativeSpeed(vehicle, neighbor) / 30.0, // Normalized by typical max speed
                    vehicle.getSpeed() / 30.0, // Normalized by typical max speed
                };
                
                // Create reward based on link performance
                // In a real system, this would be based on measured throughput, delay, etc.
                double reward = link.getQuality();
                
                samples.add(new TrainingSample(features, reward));
            }
        }
        
        return samples;
    }
    
    /**
     * Updates link qualities based on the trained ML model
     */
    private void updateLinkQualities() {
        for (VehicleNode vehicle : vehicles.values()) {
            for (Map.Entry<String, Link> entry : vehicle.getLinks().entrySet()) {
                String neighborId = entry.getKey();
                Link link = entry.getValue();
                
                NetworkNode neighbor = getNodeById(neighborId);
                if (neighbor == null) continue;
                
                // Create feature vector for prediction
                double[] features = {
                    link.getReliability(),
                    link.getDuration() / 60.0,
                    calculateRelativeSpeed(vehicle, neighbor) / 30.0,
                    vehicle.getSpeed() / 30.0,
                };
                
                // Predict new Q-value using the trained model
                double predictedQuality = dqn.predict(features);
                
                // Update link quality using exponential moving average
                double alpha = 0.3; // Smoothing factor
                double newQuality = (alpha * predictedQuality) + ((1 - alpha) * link.getQuality());
                
                link.setQuality(newQuality);
            }
        }
    }
    
    /**
     * Finds the nearest RSU to a vehicle
     * @param vehicle The vehicle
     * @return ID of the nearest RSU, or null if none found
     */
    private String findNearestRSU(VehicleNode vehicle) {
        String nearestId = null;
        double minDistance = Double.MAX_VALUE;
        
        for (InfrastructureNode rsu : infrastructure.values()) {
            double distance = calculateDistance(vehicle.getX(), vehicle.getY(), rsu.getX(), rsu.getY());
            if (distance < minDistance) {
                minDistance = distance;
                nearestId = rsu.getId();
            }
        }
        
        return nearestId;
    }
    
    /**
     * Finds a random vehicle (not the given one)
     * @param excludeId ID to exclude
     * @return ID of a random vehicle, or null if none found
     */
    private String findRandomVehicle(String excludeId) {
        List<String> otherVehicles = new ArrayList<>();
        
        for (String vehicleId : vehicles.keySet()) {
            if (!vehicleId.equals(excludeId)) {
                otherVehicles.add(vehicleId);
            }
        }
        
        if (otherVehicles.isEmpty()) {
            return null;
        }
        
        // Pick a random vehicle
        int index = (int)(Math.random() * otherVehicles.size());
        return otherVehicles.get(index);
    }
    
    /**
     * Gets a node by ID
     * @param nodeId The node ID to look up
     * @return The network node, or null if not found
     */
    private NetworkNode getNodeById(String nodeId) {
        if (vehicles.containsKey(nodeId)) {
            return vehicles.get(nodeId);
        } else if (infrastructure.containsKey(nodeId)) {
            return infrastructure.get(nodeId);
        }
        return null;
    }
    
    /**
     * Calculates distance between two points
     * @param x1 First point x-coordinate
     * @param y1 First point y-coordinate
     * @param x2 Second point x-coordinate
     * @param y2 Second point y-coordinate
     * @return Distance in meters
     */
    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    /**
     * Calculates signal strength based on distance and range
     * @param distance Distance between nodes
     * @param range Maximum transmission range
     * @return Signal strength value between 0 and 1
     */
    private double calculateSignalStrength(double distance, double range) {
        // Simple model: linear decay with distance
        return Math.max(0, 1.0 - (distance / range));
    }
    
    /**
     * Calculates signal strength using the Nakagami fading model
     * @param distance Distance between nodes
     * @param range Maximum transmission range
     * @param shapeFactor Nakagami shape parameter (m ≥ 0.5)
     * @param attenuation Additional attenuation from obstacles (0-1)
     * @return Signal strength value between 0 and 1
     */
    private double calculateNakagamiSignalStrength(double distance, double range, 
                                                double shapeFactor, double attenuation) {
        // Basic path loss (deterministic component)
        double pathLoss = Math.max(0, 1.0 - (distance / range));
        
        // Apply obstacle attenuation
        pathLoss *= (1.0 - attenuation);
        
        // Generate Nakagami-distributed random variable for fading
        // For Nakagami-m distribution with parameters m and Ω
        double m = shapeFactor; // Shape parameter (m ≥ 0.5)
        double omega = this.nakagamiSpreadFactor; // Spread parameter (Ω > 0)
        
        // Generate Nakagami-m distributed random variable
        // Method: Generate gamma distributed random variable with shape parameter m and scale parameter Ω/m
        // Then take the square root
        double gamma = generateGamma(m, omega / m);
        double nakagami = Math.sqrt(gamma);
        
        // Scale the fading effect - higher shape factors have less severe fading
        double fadingEffect = nakagami * (1.0 / Math.sqrt(m));
        
        // Combine path loss and fading
        return Math.min(1.0, Math.max(0.0, pathLoss * fadingEffect));
    }
    
    /**
     * Generates a random number from the Gamma distribution
     * @param shape Shape parameter (k > 0)
     * @param scale Scale parameter (θ > 0)
     * @return Gamma distributed random number
     */
    private double generateGamma(double shape, double scale) {
        // For shape >= 1, use Marsaglia and Tsang's method
        if (shape >= 1.0) {
            double d = shape - 1.0/3.0;
            double c = 1.0 / Math.sqrt(9.0 * d);
            while (true) {
                double x, v, u;
                do {
                    x = random.nextGaussian();
                    v = 1.0 + c * x;
                } while (v <= 0);
                
                v = v * v * v;
                u = random.nextDouble();
                
                if (u < 1.0 - 0.0331 * x * x * x * x || 
                    Math.log(u) < 0.5 * x * x + d * (1.0 - v + Math.log(v))) {
                    return scale * d * v;
                }
            }
        } 
        // For shape < 1, use Ahrens and Dieter's method
        else {
            double b = (Math.E + shape) / Math.E;
            double p = b * random.nextDouble();
            
            if (p <= 1.0) {
                double x = Math.pow(p, 1.0 / shape);
                if (random.nextDouble() <= Math.exp(-x)) {
                    return scale * x;
                }
            } else {
                double x = -Math.log((b - p) / shape);
                if (random.nextDouble() <= Math.pow(x, shape - 1.0)) {
                    return scale * x;
                }
            }
            
            // If we reach here, retry
            return generateGamma(shape, scale);
        }
    }
    
    /**
     * Calculates the total attenuation from obstacles between two points
     * @param x1 First point x-coordinate
     * @param y1 First point y-coordinate
     * @param x2 Second point x-coordinate
     * @param y2 Second point y-coordinate
     * @return Attenuation factor (0-1, where 1 means complete blockage)
     */
    private double calculateObstacleAttenuation(double x1, double y1, double x2, double y2) {
        double totalAttenuation = 0.0;
        
        for (Obstacle obstacle : obstacles) {
            if (obstacle.intersectsLine(x1, y1, x2, y2)) {
                // Accumulate attenuation factors using a product model
                // (multiple obstacles have cumulative effect)
                totalAttenuation = totalAttenuation + (1 - totalAttenuation) * obstacle.getAttenuation();
            }
        }
        
        return totalAttenuation;
    }
    
    /**
     * Calculates congestion factor at a given location
     * @param x X-coordinate
     * @param y Y-coordinate
     * @return Congestion factor (0-1, where 1 means maximum congestion)
     */
    private double calculateCongestionFactor(double x, double y) {
        double congestion = 0.0;
        
        // Check all congestion zones
        for (CongestionZone zone : congestionZones) {
            if (zone.containsPoint(x, y)) {
                // Take the highest congestion if in multiple zones
                congestion = Math.max(congestion, zone.getNetworkLoad());
            }
        }
        
        // Also consider vehicle density in the area
        double localDensity = calculateLocalVehicleDensity(x, y, 100.0); // 100m radius
        congestion = Math.max(congestion, Math.min(1.0, localDensity / 20.0)); // 20 vehicles for max congestion
        
        return congestion;
    }
    
    /**
     * Calculates the local vehicle density in an area
     * @param x Center x-coordinate
     * @param y Center y-coordinate
     * @param radius Radius to consider
     * @return Number of vehicles in the area
     */
    private double calculateLocalVehicleDensity(double x, double y, double radius) {
        int count = 0;
        
        for (VehicleNode vehicle : vehicles.values()) {
            double distance = calculateDistance(x, y, vehicle.getX(), vehicle.getY());
            if (distance <= radius) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Calculates relative speed between two nodes
     * @param node1 First node
     * @param node2 Second node
     * @return Relative speed in meters per second
     */
    private double calculateRelativeSpeed(NetworkNode node1, NetworkNode node2) {
        if (node1 instanceof VehicleNode && node2 instanceof VehicleNode) {
            VehicleNode v1 = (VehicleNode) node1;
            VehicleNode v2 = (VehicleNode) node2;
            
            // Calculate velocity components
            double v1x = v1.getSpeed() * Math.cos(v1.getDirection());
            double v1y = v1.getSpeed() * Math.sin(v1.getDirection());
            
            double v2x = v2.getSpeed() * Math.cos(v2.getDirection());
            double v2y = v2.getSpeed() * Math.sin(v2.getDirection());
            
            // Calculate relative velocity
            double relVx = v1x - v2x;
            double relVy = v1y - v2y;
            
            // Return magnitude of relative velocity
            return Math.sqrt(relVx * relVx + relVy * relVy);
        } else {
            // If one node is infrastructure, use the vehicle's speed
            if (node1 instanceof VehicleNode) {
                return ((VehicleNode) node1).getSpeed();
            } else if (node2 instanceof VehicleNode) {
                return ((VehicleNode) node2).getSpeed();
            } else {
                return 0.0; // Both are infrastructure (static)
            }
        }
    }
    
    /**
     * Estimates how long a link will remain valid
     * @param distance Current distance between nodes
     * @param relativeSpeed Relative speed between nodes
     * @param range Transmission range
     * @return Estimated duration in seconds
     */
    private double estimateLinkDuration(double distance, double relativeSpeed, double range) {
        if (relativeSpeed < 0.1) {
            return 300.0; // 5 minutes if barely moving relative to each other
        }
        
        // Calculate how long until nodes are out of range
        double remainingDistance = range - distance;
        return remainingDistance / relativeSpeed;
    }
    
    /**
     * Calculates overall link quality based on multiple factors
     * @param signalStrength Signal strength (0-1)
     * @param reliability Link reliability (0-1)
     * @param duration Estimated link duration in seconds
     * @param relativeSpeed Relative speed between nodes in m/s
     * @return Link quality value
     */
    private double calculateLinkQuality(double signalStrength, double reliability, 
                                      double duration, double relativeSpeed) {
        // Normalize duration (cap at 60 seconds)
        double normalizedDuration = Math.min(duration, 60.0) / 60.0;
        
        // Normalize relative speed (higher relative speed means less stable link)
        double normalizedRelSpeed = Math.max(0, 1.0 - (relativeSpeed / 30.0));
        
        // Weighted sum of factors
        return (0.3 * signalStrength) + (0.3 * reliability) + 
               (0.2 * normalizedDuration) + (0.2 * normalizedRelSpeed);
    }
    
    /**
     * Calculates the average link quality across all links
     * @return Average link quality
     */
    private double calculateAverageLinkQuality() {
        double totalQuality = 0.0;
        int linkCount = 0;
        
        for (VehicleNode vehicle : vehicles.values()) {
            for (Link link : vehicle.getLinks().values()) {
                totalQuality += link.getQuality();
                linkCount++;
            }
        }
        
        return linkCount > 0 ? totalQuality / linkCount : 0.0;
    }
    
    /**
     * Gets network statistics
     * @return Map of various network statistics
     */
    public Map<String, Object> getNetworkStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("vehicleCount", vehicles.size());
        stats.put("infrastructureCount", infrastructure.size());
        stats.put("messageQueueSize", messageQueue.size());
        stats.put("deliveredMessages", deliveredMessages.size());
        stats.put("simulationTime", currentTime);
        
        // Calculate average link quality
        double totalLinkQuality = 0.0;
        int linkCount = 0;
        
        for (VehicleNode vehicle : vehicles.values()) {
            for (Link link : vehicle.getLinks().values()) {
                totalLinkQuality += link.getQuality();
                linkCount++;
            }
        }
        
        stats.put("averageLinkQuality", linkCount > 0 ? totalLinkQuality / linkCount : 0.0);
        stats.put("totalLinks", linkCount);
        
        return stats;
    }
    
    /**
     * Gets the current simulation time
     * @return Current simulation time in milliseconds
     */
    public long getCurrentTime() {
        return currentTime;
    }
}

/**
 * Result of a message delivery attempt
 */
class DeliveryResult {
    public boolean delivered;
    public boolean pathBroken;
    public int hops;
    
    public DeliveryResult(boolean delivered, boolean pathBroken, int hops) {
        this.delivered = delivered;
        this.pathBroken = pathBroken;
        this.hops = hops;
    }
}

/**
 * Base class for all network nodes
 */
abstract class NetworkNode {
    private String id;
    protected Map<String, Link> links;
    
    public NetworkNode(String id) {
        this.id = id;
        this.links = new HashMap<>();
    }
    
    public String getId() {
        return id;
    }
    
    public Map<String, Link> getLinks() {
        return links;
    }
    
    public void addLink(String destId, double quality, double reliability, double duration) {
        links.put(destId, new Link(destId, quality, reliability, duration));
    }
    
    public void clearLinks() {
        links.clear();
    }
    
    public abstract double getX();
    public abstract double getY();
    public abstract double getTransmissionRange();
}

/**
 * Represents a vehicle node in the VANET
 */
class VehicleNode extends NetworkNode {
    private double x;
    private double y;
    private double direction; // in radians
    private double speed; // in m/s
    private double maxSpeed;
    private double transmissionRange;
    private List<Application> applications;
    
    public VehicleNode(String id, double x, double y, double direction, 
                      double speed, double transmissionRange) {
        super(id);
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.speed = speed;
        this.maxSpeed = 30.0; // Default 30 m/s (108 km/h)
        this.transmissionRange = transmissionRange;
        this.applications = new ArrayList<>();
    }
    
    public void updatePosition(long timeStep) {
        // Convert time step from milliseconds to seconds
        double timeInSeconds = timeStep / 1000.0;
        
        // Update position based on direction and speed
        x += speed * Math.cos(direction) * timeInSeconds;
        y += speed * Math.sin(direction) * timeInSeconds;
    }
    
    public void setDirection(double direction) {
        this.direction = direction;
    }
    
    public void setSpeed(double speed) {
        this.speed = Math.min(speed, maxSpeed);
    }
    
    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
        this.speed = Math.min(speed, maxSpeed);
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getDirection() {
        return direction;
    }
    
    public double getSpeed() {
        return speed;
    }
    
    public double getTransmissionRange() {
        return transmissionRange;
    }
    
    public void addApplication(Application app) {
        applications.add(app);
    }
    
    public List<Application> getApplications() {
        return applications;
    }
    
    /**
     * Aligns vehicle direction to the nearest road
     * @param road The road to align to
     */
    public void alignToRoad(Road road) {
        // Calculate road direction
        double roadDirection = Math.atan2(road.getEndY() - road.getStartY(), 
                                         road.getEndX() - road.getStartX());
        
        // Check if vehicle is already aligned with road (or opposite direction)
        double angleDiff = Math.abs(normalizeAngle(direction - roadDirection));
        double oppositeAngleDiff = Math.abs(normalizeAngle(direction - (roadDirection + Math.PI)));
        
        if (angleDiff > 0.1 && oppositeAngleDiff > 0.1) {
            // Vehicle is not aligned with road, adjust direction
            // Choose the smaller angle to align to (road direction or opposite)
            if (angleDiff <= oppositeAngleDiff) {
                direction = roadDirection;
            } else {
                direction = normalizeAngle(roadDirection + Math.PI);
            }
        }
    }
    
    /**
     * Normalizes an angle to be between 0 and 2π
     * @param angle Angle in radians
     * @return Normalized angle
     */
    private double normalizeAngle(double angle) {
        return (angle + 2 * Math.PI) % (2 * Math.PI);
    }
}

/**
 * Represents an infrastructure node (RSU - Road Side Unit)
 */
class InfrastructureNode extends NetworkNode {
    private double x;
    private double y;
    private double transmissionRange;
    
    public InfrastructureNode(String id, double x, double y, double transmissionRange) {
        super(id);
        this.x = x;
        this.y = y;
        this.transmissionRange = transmissionRange;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getTransmissionRange() {
        return transmissionRange;
    }
}

/**
 * Represents a link between two nodes
 */
class Link {
    private String destId;
    private double quality;
    private double reliability;
    private double duration;
    
    public Link(String destId, double quality, double reliability, double duration) {
        this.destId = destId;
        this.quality = quality;
        this.reliability = reliability;
        this.duration = duration;
    }
    
    public String getDestId() {
        return destId;
    }
    
    public double getQuality() {
        return quality;
    }
    
    public void setQuality(double quality) {
        this.quality = quality;
    }
    
    public double getReliability() {
        return reliability;
    }
    
    public double getDuration() {
        return duration;
    }
}

/**
 * Represents a vehicle application generating traffic
 */
class Application {
    private int type;
    private int payloadSize;
    private long interval;
    private long lastSentTime;
    
    public Application(int type, int payloadSize, long interval) {
        this.type = type;
        this.payloadSize = payloadSize;
        this.interval = interval;
        this.lastSentTime = 0;
    }
    
    public int getType() {
        return type;
    }
    
    public int getPayloadSize() {
        return payloadSize;
    }
    
    public long getInterval() {
        return interval;
    }
    
    public long getLastSentTime() {
        return lastSentTime;
    }
    
    public void setLastSentTime(long time) {
        this.lastSentTime = time;
    }
}

/**
 * Represents a message in the network
 */
class Message {
    private String id;
    private String source;
    private String destination;
    private int type;
    private int size;
    private long creationTime;
    
    public Message(String id, String source, String destination, int type, int size, long creationTime) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.type = type;
        this.size = size;
        this.creationTime = creationTime;
    }
    
    public String getId() {
        return id;
    }
    
    public String getSource() {
        return source;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public int getType() {
        return type;
    }
    
    public int getSize() {
        return size;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
}

/**
 * Represents a road in the environment
 */
class Road {
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private int lanes;
    private double speedLimit;
    private int environmentType;
    
    public Road(double startX, double startY, double endX, double endY, int lanes, double speedLimit, int environmentType) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.lanes = lanes;
        this.speedLimit = speedLimit;
        this.environmentType = environmentType;
    }
    
    public double getStartX() {
        return startX;
    }
    
    public double getStartY() {
        return startY;
    }
    
    public double getEndX() {
        return endX;
    }
    
    public double getEndY() {
        return endY;
    }
    
    public int getLanes() {
        return lanes;
    }
    
    public double getSpeedLimit() {
        return speedLimit;
    }
    
    public int getEnvironmentType() {
        return environmentType;
    }
    
    /**
     * Calculates distance from a point to this road
     * @param x X-coordinate
     * @param y Y-coordinate
     * @return Distance in meters
     */
    public double distanceFromPoint(double x, double y) {
        // Vector from start to end
        double rx = endX - startX;
        double ry = endY - startY;
        
        // Vector from start to point
        double px = x - startX;
        double py = y - startY;
        
        // Length of road segment
        double roadLength = Math.sqrt(rx * rx + ry * ry);
        
        // Normalize road vector
        rx /= roadLength;
        ry /= roadLength;
        
        // Project point vector onto road vector
        double projection = px * rx + py * ry;
        
        // Clamp projection to road segment
        projection = Math.max(0, Math.min(roadLength, projection));
        
        // Calculate closest point on road
        double closestX = startX + projection * rx;
        double closestY = startY + projection * ry;
        
        // Return distance to closest point
        return Math.sqrt(Math.pow(x - closestX, 2) + Math.pow(y - closestY, 2));
    }
}

/**
 * Represents the road network
 */
class RoadMap {
    private List<Road> roads;
    private double width;
    private double height;
    
    public RoadMap(double width, double height) {
        this.roads = new ArrayList<>();
        this.width = width;
        this.height = height;
    }
    
    public void addRoad(double startX, double startY, double endX, double endY, int lanes, double speedLimit, int environmentType) {
        roads.add(new Road(startX, startY, endX, endY, lanes, speedLimit, environmentType));
    }
    
    public Road getNearestRoad(double x, double y) {
        if (roads.isEmpty()) return null;
        
        Road nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Road road : roads) {
            double distance = road.distanceFromPoint(x, y);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = road;
            }
        }
        
        // Only return if the point is close enough to the road
        return minDistance <= 20.0 ? nearest : null; // 20m threshold
    }
    
    /**
     * Checks if there is line of sight between two points
     * @param x1 First point x-coordinate
     * @param y1 First point y-coordinate
     * @param x2 Second point x-coordinate
     * @param y2 Second point y-coordinate
     * @param environmentType The type of environment (urban, suburban, highway)
     * @return True if there is line of sight
     */
    public boolean hasLineOfSight(double x1, double y1, double x2, double y2, int environmentType) {
        double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        
        // Base probability adjusted by environment type
        double baseProbability;
        switch (environmentType) {
            case VANETOptimizationComparison.URBAN:
                baseProbability = 0.6; // Urban has more obstacles
                break;
            case VANETOptimizationComparison.SUBURBAN:
                baseProbability = 0.8; // Suburban has fewer obstacles
                break;
            case VANETOptimizationComparison.HIGHWAY:
                baseProbability = 0.95; // Highway has minimal obstacles
                break;
            default:
                baseProbability = 0.7; // Default value
        }
        
        // Reduce probability as distance increases
        return Math.random() < (baseProbability * (1.0 - (distance / 300.0)));
    }
}

/**
 * Utility class for Dijkstra's algorithm
 */
class NodeDistancePair implements Comparable<NodeDistancePair> {
    private String nodeId;
    private double distance;
    
    public NodeDistancePair(String nodeId, double distance) {
        this.nodeId = nodeId;
        this.distance = distance;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public double getDistance() {
        return distance;
    }
    
    @Override
    public int compareTo(NodeDistancePair other) {
        return Double.compare(this.distance, other.distance);
    }
}

/**
 * Training sample for the deep Q-network
 */
class TrainingSample {
    private double[] features;
    private double reward;
    
    public TrainingSample(double[] features, double reward) {
        this.features = features;
        this.reward = reward;
    }
    
    public double[] getFeatures() {
        return features;
    }
    
    public double getReward() {
        return reward;
    }
}

/**
 * Simple implementation of a Deep Q-Network for reinforcement learning
 */

class DeepQNetwork {
    private double learningRate;
    private double[][] weights1; // First hidden layer weights
    private double[] bias1;      // First hidden layer bias
    private double[][] weights2; // Second hidden layer weights
    private double[] bias2;      // Second hidden layer bias
    private double[] weights3;   // Output layer weights
    private double bias3;        // Output layer bias

    public DeepQNetwork(double learningRate) {
        this.learningRate = learningRate;

        // New architecture: 4 input -> 16 hidden -> 8 hidden -> 1 output
        weights1 = new double[4][16];
        bias1 = new double[16];
        weights2 = new double[16][8];
        bias2 = new double[8];
        weights3 = new double[8];
        bias3 = 0.5; // Initial bias

        Random random = new Random(42);

        // Initialize first layer weights and biases
        for (int i = 0; i < weights1.length; i++) {
            for (int j = 0; j < weights1[i].length; j++) {
                weights1[i][j] = (random.nextDouble() * 2 - 1) * 0.1;
            }
        }
        for (int i = 0; i < bias1.length; i++) {
            bias1[i] = (random.nextDouble() * 2 - 1) * 0.1;
        }

        // Initialize second layer weights and biases
        for (int i = 0; i < weights2.length; i++) {
            for (int j = 0; j < weights2[i].length; j++) {
                weights2[i][j] = (random.nextDouble() * 2 - 1) * 0.1;
            }
        }
        for (int i = 0; i < bias2.length; i++) {
            bias2[i] = (random.nextDouble() * 2 - 1) * 0.1;
        }

        // Initialize output layer weights
        for (int i = 0; i < weights3.length; i++) {
            weights3[i] = (random.nextDouble() * 2 - 1) * 0.1;
        }
        bias3 = (random.nextDouble() * 2 - 1) * 0.1;
    }

    /**
     * Sigmoid activation function
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    /**
     * Forward pass: Predicts Q-value for given features
     */
    public double predict(double[] features) {
        if (features.length != 4) {
            throw new IllegalArgumentException("Input size must be 4.");
        }

        // First hidden layer
        double[] hidden1 = new double[16];
        for (int i = 0; i < 16; i++) {
            double sum = bias1[i];
            for (int j = 0; j < features.length; j++) {
                sum += features[j] * weights1[j][i];
            }
            hidden1[i] = sigmoid(sum);
        }

        // Second hidden layer
        double[] hidden2 = new double[8];
        for (int i = 0; i < 8; i++) {
            double sum = bias2[i];
            for (int j = 0; j < 16; j++) {
                sum += hidden1[j] * weights2[j][i];
            }
            hidden2[i] = sigmoid(sum);
        }

        // Output layer
        double sum = bias3;
        for (int i = 0; i < 8; i++) {
            sum += hidden2[i] * weights3[i];
        }
        return sigmoid(sum);
    }

    /**
     * Trains the network on a batch of samples using SGD
     */
    public void trainOnBatch(List<TrainingSample> samples) {
        for (TrainingSample sample : samples) {
            // Forward pass
            double[] features = sample.getFeatures();
            double target = sample.getReward();

            // First hidden layer
            double[] hidden1 = new double[16];
            double[] hiddenRaw1 = new double[16];
            for (int i = 0; i < 16; i++) {
                hiddenRaw1[i] = bias1[i];
                for (int j = 0; j < features.length; j++) {
                    hiddenRaw1[i] += features[j] * weights1[j][i];
                }
                hidden1[i] = sigmoid(hiddenRaw1[i]);
            }

            // Second hidden layer
            double[] hidden2 = new double[8];
            double[] hiddenRaw2 = new double[8];
            for (int i = 0; i < 8; i++) {
                hiddenRaw2[i] = bias2[i];
                for (int j = 0; j < 16; j++) {
                    hiddenRaw2[i] += hidden1[j] * weights2[j][i];
                }
                hidden2[i] = sigmoid(hiddenRaw2[i]);
            }

            // Output layer
            double outputRaw = bias3;
            for (int i = 0; i < 8; i++) {
                outputRaw += hidden2[i] * weights3[i];
            }
            double output = sigmoid(outputRaw);

            // Backpropagation
            double error = target - output;
            double delta_output = error * output * (1 - output);

            // Update output layer weights
            bias3 += learningRate * delta_output;
            for (int i = 0; i < weights3.length; i++) {
                weights3[i] += learningRate * delta_output * hidden2[i];
            }

            // Update second hidden layer
            double[] delta_hidden2 = new double[8];
            for (int i = 0; i < 8; i++) {
                delta_hidden2[i] = delta_output * weights3[i] * hidden2[i] * (1 - hidden2[i]);
                bias2[i] += learningRate * delta_hidden2[i];
                for (int j = 0; j < 16; j++) {
                    weights2[j][i] += learningRate * delta_hidden2[i] * hidden1[j];
                }
            }

            // Update first hidden layer
            double[] delta_hidden1 = new double[16];
            for (int i = 0; i < 16; i++) {
                double sumError = 0.0;
                for (int j = 0; j < 8; j++) {
                    sumError += delta_hidden2[j] * weights2[i][j];
                }
                delta_hidden1[i] = sumError * hidden1[i] * (1 - hidden1[i]);
                bias1[i] += learningRate * delta_hidden1[i];
                for (int j = 0; j < features.length; j++) {
                    weights1[j][i] += learningRate * delta_hidden1[i] * features[j];
                }
            }
        }
    }
}


/**
 * Represents a congestion zone in the simulation
 */
class CongestionZone {
    private double x1;
    private double y1;
    private double x2;
    private double y2;
    private double networkLoad; // 0.0-1.0, where 1.0 means maximum congestion
    
    public CongestionZone(double x1, double y1, double x2, double y2, double networkLoad) {
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.networkLoad = networkLoad;
    }
    
    public boolean containsPoint(double x, double y) {
        return x >= x1 && x <= x2 && y >= y1 && y <= y2;
    }
    
    public double getNetworkLoad() {
        return networkLoad;
    }
}

/**
 * Represents a physical obstacle in the simulation
 */
class Obstacle {
    private double x1;
    private double y1;
    private double x2;
    private double y2;
    private double attenuation; // 0.0-1.0, where 1.0 means complete signal blockage
    
    public Obstacle(double x1, double y1, double x2, double y2, double attenuation) {
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.attenuation = attenuation;
    }
    
    public boolean intersectsLine(double lx1, double ly1, double lx2, double ly2) {
        // Check if line intersects any of the four sides of the obstacle
        return lineIntersectsSegment(lx1, ly1, lx2, ly2, x1, y1, x2, y1) || // Top edge
               lineIntersectsSegment(lx1, ly1, lx2, ly2, x2, y1, x2, y2) || // Right edge
               lineIntersectsSegment(lx1, ly1, lx2, ly2, x1, y2, x2, y2) || // Bottom edge
               lineIntersectsSegment(lx1, ly1, lx2, ly2, x1, y1, x1, y2);   // Left edge
    }
    
    private boolean lineIntersectsSegment(double lx1, double ly1, double lx2, double ly2,
                                          double sx1, double sy1, double sx2, double sy2) {
        // Calculate line equation parameters
        double a1 = ly2 - ly1;
        double b1 = lx1 - lx2;
        double c1 = lx2 * ly1 - lx1 * ly2;
        
        // Calculate segment endpoints relative to line
        double d1 = a1 * sx1 + b1 * sy1 + c1;
        double d2 = a1 * sx2 + b1 * sy2 + c1;
        
        // If endpoints are on same side of line, no intersection
        if (d1 * d2 > 0) {
            return false;
        }
        
        // Calculate segment equation parameters
        double a2 = sy2 - sy1;
        double b2 = sx1 - sx2;
        double c2 = sx2 * sy1 - sx1 * sy2;
        
        // Calculate line endpoints relative to segment
        double d3 = a2 * lx1 + b2 * ly1 + c2;
        double d4 = a2 * lx2 + b2 * ly2 + c2;
        
        // If line endpoints are on same side of segment, no intersection
        if (d3 * d4 > 0) {
            return false;
        }
        
        // Check if intersection is within the line segment bounds
        // Calculate intersection point
        double intersectX = (b1 * c2 - b2 * c1) / (a1 * b2 - a2 * b1);
        double intersectY = (a2 * c1 - a1 * c2) / (a1 * b2 - a2 * b1);
        
        // Check if intersection point is within the bounds of both segments
        return (Math.min(lx1, lx2) <= intersectX && intersectX <= Math.max(lx1, lx2) &&
                Math.min(ly1, ly2) <= intersectY && intersectY <= Math.max(ly1, ly2) &&
                Math.min(sx1, sx2) <= intersectX && intersectX <= Math.max(sx1, sx2) &&
                Math.min(sy1, sy2) <= intersectY && intersectY <= Math.max(sy1, sy2));
    }
    
    public double getAttenuation() {
        return attenuation;
    }
}