package io.github.saniamb.chromiq.core.color;

import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.utils.Logger;

import java.util.*;

/**
 * ColorClusterer - LAB-based K-means Color Clustering
 * ==================================================
 * <p>
 * Uses weighted K-means clustering in LAB color space to group similar colors.
 * This is more sophisticated than greedy Delta E grouping because:
 * <p>
 * 1. Finds globally optimal cluster centers (not just locally good ones)
 * 2. Uses pixel counts as weights (dominant colors influence clusters more)
 * 3. Works in LAB space (perceptually uniform color distances)
 * 4. Iteratively refines clusters until convergence
 */

public class ColorClusterer {

    /**
     * Maximum iterations for k-means convergence.
     * Usually converges in 5-10 iterations, but we set a safety limit.
     */
    private static final int MAX_ITERATIONS = 50;

    /**
     * Convergence threshold: if cluster centers move less than this distance
     * in LAB space between iterations, we consider the algorithm converged.
     */
    private static final double CONVERGENCE_THRESHOLD = 0.1;

    /**
     * Represents a cluster of similar colors with a weighted center.
     */
    public static class ColorCluster {
        private ColorEntry centerColor;                   // Representative color for this cluster
        private List<ColorExtractor.ColorCount> members;    // All colors assigned to this cluster
        private int totalPixelCount;                        // Total pixels across all members
        private double totalPercentage;                     // Total percentage across all members
        private double[] labCenter;                         // LAB coordinates of cluster center

        public ColorCluster(ColorEntry initialCenter) {
            this.centerColor = initialCenter;
            this.members = new ArrayList<>();
            this.totalPercentage = 0.0;
            this.totalPixelCount = 0;
            this.labCenter = initialCenter.getLAB();
        }

        /**
         * Adds a color to this cluster and updates statistics.
         */
        public void addMember(ColorExtractor.ColorCount colorCount) {
            members.add(colorCount);
            totalPixelCount += colorCount.getCount();
            totalPercentage += colorCount.getPercentage();
        }


        /**
         * Clears all members (used during k-means iterations).
         */
        public void clearMembers() {
            members.clear();
            totalPixelCount = 0;
            totalPercentage = 0.0;
        }

        /**
         * Recalculates the cluster center as a weighted average of all members.
         * This is the core of k-means: update centers based on current assignments.
         */
        public boolean updateCenter() {
            if (members.isEmpty()) return false;

            double weightedL = 0.0, weightedA = 0.0, weightedB = 0.0;
            long totalWeight = 0;

            for (ColorExtractor.ColorCount colorCount : members) {
                double[] lab = colorCount.getColor().getLAB();
                int weight = colorCount.getCount(); //Pixel count as weight

                weightedL += lab[0] * weight;
                weightedA += lab[1] * weight;
                weightedB += lab[2] * weight;
                totalWeight += weight;
            }

            if (totalWeight == 0) return false;

            // New cluster center in LAB space
            double[] newLabCenter = new double[]{
                    weightedL / totalWeight,
                    weightedA / totalWeight,
                    weightedB / totalWeight
            };

            // Check if center moved significantly (for convergence)
            double centerMovement = calculateLabDistance(labCenter, newLabCenter);

            // Update center
            labCenter = newLabCenter;
            centerColor = ColorEntry.fromLAB(labCenter[0], labCenter[1], labCenter[2]);

            return centerMovement > CONVERGENCE_THRESHOLD;
        }

        // Getters
        public ColorEntry getCenterColor() {
            return centerColor;
        }

        public List<ColorExtractor.ColorCount> getMembers() {
            return new ArrayList<>(members);
        }

        public int getTotalPixelCount() {
            return totalPixelCount;
        }

        public double getTotalPercentage() {
            return totalPercentage;
        }

        public int getClusterSize() {
            return members.size();
        }

        public double[] getLabCenter() {
            return Arrays.copyOf(labCenter, 3);
        }

        @Override
        public String toString() {
            return String.format("ColorCluster{center=%s, members=%d, pixels=%d (%.2f%%)}",
                    centerColor.getHex(), members.size(), totalPixelCount, totalPercentage);
        }
    }


    /**
     * Clusters colors using weighted k-means in LAB space.
     *
     * @param extractedColors Colors from ColorExtractor (with pixel counts)
     * @param k Number of clusters to create
     * @return List of clusters sorted by total pixel count (dominant first)
     */
    public List<ColorCluster> clusterColors(List<ColorExtractor.ColorCount> extractedColors, int k) {
        if (extractedColors == null || extractedColors.isEmpty()) {
            Logger.info("No colors to cluster - returning empty list");
            return new ArrayList<>();
        }

        if (k <= 0 || k > extractedColors.size()) {
            throw new IllegalArgumentException("k must be between 1 and " + extractedColors.size());
        }

        Logger.info(String.format("Starting LAB k-means clustering: %d colors → %d clusters",
                extractedColors.size(), k));

        long startTime = System.currentTimeMillis();

        // Step 1: Initialize k cluster centers
        List<ColorCluster> clusters = initializeClusters(extractedColors, k);

        // Step 2: Run k-means iterations
        int iterations = runKMeansIterations(extractedColors, clusters);

        // Step 3: Sort clusters by importance (total pixel count)
        clusters.sort((c1, c2) -> Integer.compare(c2.getTotalPixelCount(), c1.getTotalPixelCount()));

        long processingTime = System.currentTimeMillis() - startTime;

        Logger.info(String.format("LAB k-means clustering complete: %d iterations, %dms",
                iterations, processingTime));

        return clusters;
    }

    /**
     * Initialize k cluster centers using k-means++ algorithm.
     * This gives better starting points than random selection.
     */
    private List<ColorCluster> initializeClusters(List<ColorExtractor.ColorCount> colors, int k) {
        List<ColorCluster> clusters = new ArrayList<>();
        List<ColorExtractor.ColorCount> remainingColors = new ArrayList<>(colors);

        // First center: pick the most dominant color
        ColorExtractor.ColorCount firstColor = colors.get(0); // Already sorted by frequency
        clusters.add(new ColorCluster(firstColor.getColor()));
        remainingColors.remove(firstColor);

        // Remaining centers: pick colors far from existing centers (k-means++)
        Random random = new Random(42); // Fixed seed for reproducible results

        for (int i = 1; i < k && !remainingColors.isEmpty(); i++) {
            ColorExtractor.ColorCount farthestColor = findFarthestColor(remainingColors, clusters, random);
            clusters.add(new ColorCluster(farthestColor.getColor()));
            remainingColors.remove(farthestColor);
        }

        Logger.info("Initialized " + clusters.size() + " cluster centers using k-means++");
        return clusters;
    }

    /**
     * Finds the color that is farthest from existing cluster centers.
     * Uses weighted random selection based on distance (k-means++ strategy).
     */
    private ColorExtractor.ColorCount findFarthestColor(List<ColorExtractor.ColorCount> candidates,
                                                        List<ColorCluster> existingClusters,
                                                        Random random) {
        // Calculate distance-based weights for each candidate
        List<Double> weights = new ArrayList<>();
        double totalWeight = 0.0;

        for (ColorExtractor.ColorCount candidate : candidates) {
            // Find minimum distance to any existing cluster center
            double minDistance = Double.MAX_VALUE;
            double[] candidateLab = candidate.getColor().getLAB();

            for (ColorCluster cluster : existingClusters) {
                double distance = calculateLabDistance(candidateLab, cluster.getLabCenter());
                minDistance = Math.min(minDistance, distance);
            }

            // Weight = distance² (k-means++ standard)
            double weight = minDistance * minDistance;
            weights.add(weight);
            totalWeight += weight;
        }

        // Weighted random selection
        double randomValue = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0.0;

        for (int i = 0; i < candidates.size(); i++) {
            cumulativeWeight += weights.get(i);
            if (randomValue <= cumulativeWeight) {
                return candidates.get(i);
            }
        }

        // Fallback (shouldn't happen)
        return candidates.get(candidates.size() - 1);
    }

    /**
     * Runs k-means iterations until convergence.
     */
    private int runKMeansIterations(List<ColorExtractor.ColorCount> colors, List<ColorCluster> clusters) {
        int iteration = 0;
        boolean centersChanged = true;

        while (centersChanged && iteration < MAX_ITERATIONS) {
            iteration++;
            centersChanged = false;

            // Clear previous assignments
            for (ColorCluster cluster : clusters) {
                cluster.clearMembers();
            }

            // Assign each color to nearest cluster center
            for (ColorExtractor.ColorCount colorCount : colors) {
                ColorCluster nearestCluster = findNearestCluster(colorCount.getColor(), clusters);
                nearestCluster.addMember(colorCount);
            }

            // Update cluster centers based on new assignments
            for (ColorCluster cluster : clusters) {
                boolean centerMoved = cluster.updateCenter();
                if (centerMoved) {
                    centersChanged = true;
                }
            }

            Logger.info(String.format("K-means iteration %d: centers %s",
                    iteration, centersChanged ? "moved" : "converged"));
        }

        return iteration;
    }

    /**
     * Finds the cluster with center nearest to the given color in LAB space.
     */
    private ColorCluster findNearestCluster(ColorEntry color, List<ColorCluster> clusters) {
        double[] colorLab = color.getLAB();
        ColorCluster nearestCluster = null;
        double minDistance = Double.MAX_VALUE;

        for (ColorCluster cluster : clusters) {
            double distance = calculateLabDistance(colorLab, cluster.getLabCenter());
            if (distance < minDistance) {
                minDistance = distance;
                nearestCluster = cluster;
            }
        }

        return nearestCluster;
    }

    /**
     * Calculates Euclidean distance between two points in LAB color space.
     * This approximates Delta E (perceptual color difference).
     */
    private static double calculateLabDistance(double[] lab1, double[] lab2) {
        double deltaL = lab1[0] - lab2[0];
        double deltaA = lab1[1] - lab2[1];
        double deltaB = lab1[2] - lab2[2];

        return Math.sqrt(deltaL * deltaL + deltaA * deltaA + deltaB * deltaB);
    }
}

