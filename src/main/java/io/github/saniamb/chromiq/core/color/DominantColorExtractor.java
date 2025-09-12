package io.github.saniamb.chromiq.core.color;

import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.utils.Logger;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * DominantColorExtractor - Complete Color Processing Pipeline
 * =========================================================
 *
 * Orchestrates the entire color extraction process by combining:
 * 1. ColorExtractor (finds all colors in image)
 * 2. ColorClusterer (groups similar colors using k-means)
 * 3. Smart logic (handles edge cases like solid colors)
 *
 * Key Features:
 * - Default returns up to 10 dominant colors
 * - Automatically adapts to images with fewer colors
 * - Handles edge cases (solid colors, transparent images)
 * - Provides clean API for UI components
 *
 * Usage:
 * List<DominantColor> colors = extractor.extractDominantColors(image);
 * List<DominantColor> fewColors = extractor.extractDominantColors(image, 5);
 */
public class DominantColorExtractor {

    // Default number of colors to extract (as per your requirement)
    private static final int DEFAULT_MAX_COLORS = 10;

    // Minimum number of colors that makes clustering worthwhile
    private static final int MIN_COLORS_FOR_CLUSTERING = 15;

    // Our worker components
    private static ColorExtractor colorExtractor = null;
    private static ColorClusterer colorClusterer = null;

    /**
     * Represents a dominant color with its importance in the image.
     * This is our clean output format for the UI.
     */
    public static class DominantColor implements Comparable<DominantColor> {
        private final ColorEntry color;
        private final double percentage;
        private final int pixelCount;

        public DominantColor(ColorEntry color, double percentage, int pixelCount) {
            this.color = color;
            this.percentage = percentage;
            this.pixelCount = pixelCount;
        }

        // Getters
        public ColorEntry getColor() { return color; }
        public double getPercentage() { return percentage; }
        public int getPixelCount() { return pixelCount; }

        // Sort by percentage (most dominant first)
        @Override
        public int compareTo(DominantColor other) {
            return Double.compare(other.percentage, this.percentage);
        }

        @Override
        public String toString() {
            return String.format("DominantColor{%s: %.2f%% (%,d pixels)}",
                    color.getHex(), percentage, pixelCount);
        }
    }

    /**
     * Constructor - creates the extractor with default components.
     */
    public DominantColorExtractor() {
        colorExtractor = new ColorExtractor();
        colorClusterer = new ColorClusterer();
    }

    /**
     * Main extraction method - returns up to 10 dominant colors.
     *
     * @param image BufferedImage to analyze (should be ARGB from ImageInputHandler)
     * @return List of dominant colors, sorted by importance
     * @throws IllegalArgumentException if image is null
     */
    public static List<DominantColor> extractDominantColors(BufferedImage image) {
        return extractDominantColors(image, DEFAULT_MAX_COLORS);
    }

    /**
     * Extraction with custom color count.
     *
     * @param image BufferedImage to analyze
     * @param maxColors Maximum number of colors to return (1-10)
     * @return List of dominant colors, sorted by importance
     * @throws IllegalArgumentException if image is null or maxColors is invalid
     */
    public static List<DominantColor> extractDominantColors(BufferedImage image, int maxColors) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        if (maxColors < 1 || maxColors > DEFAULT_MAX_COLORS) {
            throw new IllegalArgumentException("maxColors must be between 1 and " + DEFAULT_MAX_COLORS);
        }

        Logger.info(String.format("Starting dominant color extraction: %dx%d image, max colors: %d",
                image.getWidth(), image.getHeight(), maxColors));

        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Extract all unique colors from the image
            List<ColorExtractor.ColorCount> allColors = colorExtractor.extractColors(image);

            if (allColors.isEmpty()) {
                Logger.info("No visible colors found in image (all transparent)");
                return new ArrayList<>();
            }

            Logger.info(String.format("Found %d unique colors in image", allColors.size()));

            // Step 2: Decide whether to cluster or return all colors
            List<DominantColor> dominantColors;

            if (shouldUseAllColors(allColors, maxColors)) {
                // Image has few colors - return all without clustering
                dominantColors = convertToNaturalPalette(allColors, maxColors);
                Logger.info(String.format("Using natural palette: returning %d colors without clustering",
                        dominantColors.size()));
            } else {
                // Image has many colors - use clustering to find dominant ones
                dominantColors = clusterAndExtract(allColors, maxColors);
                Logger.info(String.format("Used k-means clustering: extracted %d dominant colors",
                        dominantColors.size()));
            }

            // Step 3: Sort by importance (most dominant first) and return
            Collections.sort(dominantColors);

            long processingTime = System.currentTimeMillis() - startTime;
            Logger.info(String.format("Dominant color extraction complete: %d colors in %dms",
                    dominantColors.size(), processingTime));

            return dominantColors;

        } catch (Exception e) {
            Logger.info("Error during color extraction: " + e.getMessage());
            throw new RuntimeException("Failed to extract dominant colors", e);
        }
    }

    /**
     * Determines if we should return all colors without clustering.
     * This happens when the image naturally has few distinct colors.
     */
    private static boolean shouldUseAllColors(List<ColorExtractor.ColorCount> allColors, int maxColors) {
        // If image has fewer colors than requested, no point in clustering
        if (allColors.size() <= maxColors) {
            return true;
        }

        // If image doesn't have enough colors to make clustering worthwhile
        if (allColors.size() < MIN_COLORS_FOR_CLUSTERING) {
            return true;
        }

        // Otherwise, use clustering
        return false;
    }

    /**
     * Converts raw color counts to DominantColor format without clustering.
     * Used for images that naturally have few colors.
     */
    private static List<DominantColor> convertToNaturalPalette(List<ColorExtractor.ColorCount> colorCounts, int maxColors) {
        List<DominantColor> result = new ArrayList<>();

        // Take up to maxColors, they're already sorted by frequency
        int colorsToTake = Math.min(maxColors, colorCounts.size());

        for (int i = 0; i < colorsToTake; i++) {
            ColorExtractor.ColorCount cc = colorCounts.get(i);
            result.add(new DominantColor(cc.getColor(), cc.getPercentage(), cc.getCount()));
        }

        return result;
    }

    /**
     * Uses k-means clustering to find dominant colors in complex images.
     */
    private static List<DominantColor> clusterAndExtract(List<ColorExtractor.ColorCount> allColors, int maxColors) {
        // Use k-means clustering to group similar colors
        List<ColorClusterer.ColorCluster> clusters = colorClusterer.clusterColors(allColors, maxColors);

        List<DominantColor> result = new ArrayList<>();

        for (ColorClusterer.ColorCluster cluster : clusters) {
            // Convert cluster to our clean DominantColor format
            DominantColor dominantColor = new DominantColor(
                    cluster.getCenterColor(),           // Representative color of the cluster
                    cluster.getTotalPercentage(),       // Combined percentage of all colors in cluster
                    cluster.getTotalPixelCount()        // Combined pixel count of all colors in cluster
            );
            result.add(dominantColor);
        }

        return result;
    }

    /**
     * Provides a summary report of the extraction process.
     * Useful for debugging and understanding results.
     */
    public String getExtractionReport(BufferedImage image, List<DominantColor> dominantColors) {
        if (dominantColors.isEmpty()) {
            return "No dominant colors extracted from image";
        }

        StringBuilder report = new StringBuilder();
        report.append("ChromiQ Dominant Color Extraction Report\n");
        report.append("=====================================\n");
        report.append(String.format("Image: %dx%d pixels\n", image.getWidth(), image.getHeight()));
        report.append(String.format("Dominant Colors Found: %d\n\n", dominantColors.size()));

        for (int i = 0; i < dominantColors.size(); i++) {
            DominantColor dc = dominantColors.get(i);
            report.append(String.format("%2d. %s - %,d pixels (%.2f%%)\n",
                    i + 1, dc.getColor().getHex(), dc.getPixelCount(), dc.getPercentage()));
        }

        // Calculate coverage
        double totalCoverage = dominantColors.stream()
                .mapToDouble(DominantColor::getPercentage)
                .sum();

        report.append(String.format("\nTotal Coverage: %.1f%% of visible pixels\n", totalCoverage));

        return report.toString();
    }
}