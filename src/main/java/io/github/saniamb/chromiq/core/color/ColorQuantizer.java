package io.github.saniamb.chromiq.core.color;

import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.utils.Logger;
import java.util.*;

/**
 * ColorQuantizer - Smart Color Grouping Component
 * ==============================================
 *
 * Takes thousands of similar colors from ColorExtractor and groups them into
 * a manageable number of distinct, meaningful colors using Delta E color distance.
 *
 * Example: RGB(255,0,0), RGB(254,1,0), RGB(253,0,1) → One "Red" group
 *
 * This is essential for creating clean, usable color palettes from real photos.
 */
public class ColorQuantizer {

    /**
     * Delta E threshold for color similarity.
     * Delta E is the scientific standard for measuring color differences:
     * - 0-1: Colors are nearly identical
     * - 1-2: Barely perceptible difference
     * - 2-3: Noticeable but still very similar
     * - 3-5: Clear difference but related colors
     * - 5+: Obviously different colors
     */
    private static final double DEFAULT_DELTA_E_THRESHOLD = 3.0;

    /**
     * Maximum number of color groups to prevent performance issues.
     * Even complex images rarely need more than 100 distinct color groups.
     */
    private static final int MAX_COLOR_GROUPS = 500;

    /**
     * Represents a group of similar colors that have been quantized together.
     * The representative color is calculated as a weighted average of all colors in the group.
     */
    public static class ColorGroup implements Comparable<ColorGroup> {
        private final List<ColorExtractor.ColorCount> originalColors;
        private ColorEntry representativeColor;
        private int totalCount;
        private double totalPercentage;

        public ColorGroup(ColorExtractor.ColorCount initialColor) {
            this.originalColors = new ArrayList<>();
            this.originalColors.add(initialColor);
            this.representativeColor = initialColor.getColor();
            this.totalCount = initialColor.getCount();
            this.totalPercentage = initialColor.getPercentage();
        }

        /**
         * Adds a similar color to this group and recalculates the representative color.
         * The representative color becomes a weighted average based on pixel counts.
         */
        public void addColor(ColorExtractor.ColorCount colorCount) {
            originalColors.add(colorCount);
            totalCount += colorCount.getCount();
            totalPercentage += colorCount.getPercentage();

            // Recalculate representative color as weighted average
            updateRepresentativeColor();
        }

        /**
         * Calculates a weighted average color from all colors in this group.
         * Colors with more pixels have more influence on the final color.
         */
        /*
        private void updateRepresentativeColor() {
            if (originalColors.isEmpty()) return;

            long weightedRed = 0, weightedGreen = 0, weightedBlue = 0;
            long totalWeight = 0;

            for (ColorExtractor.ColorCount cc : originalColors) {
                ColorEntry color = cc.getColor();
                int weight = cc.getCount();

                weightedRed += (long) color.getRed() * weight;
                weightedGreen += (long) color.getGreen() * weight;
                weightedBlue += (long) color.getBlue() * weight;
                totalWeight += weight;
            }

            if (totalWeight > 0) {
                int avgRed = (int) (weightedRed / totalWeight);
                int avgGreen = (int) (weightedGreen / totalWeight);
                int avgBlue = (int) (weightedBlue / totalWeight);

                representativeColor = new ColorEntry(avgRed, avgGreen, avgBlue);
            }
        }
         */

        /**
         * Calculates a weighted average color from all colors in this group.
         * Colors with more pixels have more influence on the final color.
         *
         * Unlike the previous implementation (which averaged directly in RGB space),
         * this method averages in LAB color space. This ensures the representative
         * color is perceptually accurate and consistent with Delta E calculations,
         * since LAB is designed to be uniform with human vision.
         */
        private void updateRepresentativeColor() {
            if (originalColors.isEmpty()) return;

            double weightedL = 0.0, weightedA = 0.0, weightedB = 0.0;
            long totalWeight = 0;

            // Accumulate weighted LAB components
            for (ColorExtractor.ColorCount cc : originalColors) {
                ColorEntry color = cc.getColor();
                int weight = cc.getCount();
                double[] lab = color.getLAB(); // [L, A, B]

                weightedL += lab[0] * weight;
                weightedA += lab[1] * weight;
                weightedB += lab[2] * weight;
                totalWeight += weight;
            }

            if (totalWeight > 0) {
                // Compute weighted averages in LAB space
                double avgL = weightedL / totalWeight;
                double avgA = weightedA / totalWeight;
                double avgB = weightedB / totalWeight;

                // Convert averaged LAB back to RGB for representative ColorEntry
                representativeColor = ColorEntry.fromLAB(avgL, avgA, avgB);
            }
        }

/*🔹 Why This Change Helps

        Consistency with Delta E
        Your grouping is based on ΔE (LAB distance).
        Previously, the “average” was taken in RGB space → which doesn’t align with the LAB’s perceptual uniformity.
        Now, representative colors are computed in LAB as well, so group centers are truly meaningful, according to the same metric used to cluster them.
        More Perceptual Accuracy
        Averaging in RGB can “shift” colors unnaturally (e.g., averaging bright red and bright green in RGB gives a muddy brown, which the human eye doesn’t perceive as their midpoint).
        LAB averaging avoids this — the representative color looks more like the “middle” of its group to the human eye.
        Better Palette Quality
        Since palettes are ultimately shown to humans, this approach yields more natural-looking representative swatches.
        Especially useful when you extract palettes from photos with subtle gradients.
        Optimization Through Correctness
        Technically, the runtime is the same O(n) as before.
        The “optimization” here is semantic: your groups are now optimized for human perception, reducing misleading cluster centers.
        This makes downstream palette use (Chromiq visualizations, UI color picking, ML features) cleaner and more reliable. */

        // Getters
        public ColorEntry getRepresentativeColor() { return representativeColor; }
        public int getTotalCount() { return totalCount; }
        public double getTotalPercentage() { return totalPercentage; }
        public List<ColorExtractor.ColorCount> getOriginalColors() { return new ArrayList<>(originalColors); }
        public int getColorCount() { return originalColors.size(); }

        // Sort by total count (descending) for most important groups first
        @Override
        public int compareTo(ColorGroup other) {
            return Integer.compare(other.totalCount, this.totalCount);
        }

        @Override
        public String toString() {
            return String.format("ColorGroup{%s: %d pixels (%.2f%%) from %d colors}",
                    representativeColor.getHex(), totalCount, totalPercentage, originalColors.size());
        }
    }

    /**
     * Main quantization method: Groups similar colors together using Delta E distance.
     *
     * @param extractedColors List of colors from ColorExtractor (should be pre-sorted by frequency)
     * @param deltaEThreshold Maximum color difference to group together (default: 3.0)
     * @return List of ColorGroups, sorted by importance (total pixel count)
     */
    /*public List<ColorGroup> quantizeColors(List<ColorExtractor.ColorCount> extractedColors,
                                           double deltaEThreshold) {

        if (extractedColors == null || extractedColors.isEmpty()) {
            Logger.info("No colors to quantize - returning empty list");
            return new ArrayList<>();
        }

        Logger.info(String.format("Starting color quantization: %d input colors, Delta E threshold: %.1f",
                extractedColors.size(), deltaEThreshold));

        long startTime = System.currentTimeMillis();
        List<ColorGroup> colorGroups = new ArrayList<>();

        // Process colors in order of frequency (most common first)
        for (ColorExtractor.ColorCount colorCount : extractedColors) {
            boolean addedToExistingGroup = false;

            // Try to find an existing group this color belongs to
            for (ColorGroup group : colorGroups) {
                double distance = calculateDeltaE(colorCount.getColor(), group.getRepresentativeColor());

                if (distance <= deltaEThreshold) {
                    group.addColor(colorCount);
                    addedToExistingGroup = true;
                    break; // Add to first matching group only
                }
            }

            // If no suitable group found, create a new one
            if (!addedToExistingGroup) {
                colorGroups.add(new ColorGroup(colorCount));
            }

            // Safety limit to prevent performance issues
            if (colorGroups.size() >= MAX_COLOR_GROUPS) {
                Logger.info("Reached maximum color groups limit: " + MAX_COLOR_GROUPS);
                break;
            }
        }

        // Sort groups by importance (total pixel count)
        Collections.sort(colorGroups);

        long processingTime = System.currentTimeMillis() - startTime;

        Logger.info(String.format("Color quantization complete: %d groups created in %dms",
                colorGroups.size(), processingTime));

        // Log quantization effectiveness
        int originalColors = extractedColors.size();
        int finalGroups = colorGroups.size();
        double compressionRatio = (originalColors > 0) ? (double) finalGroups / originalColors : 0;

        Logger.info(String.format("Quantization ratio: %d → %d colors (%.1f%% of original)",
                originalColors, finalGroups, compressionRatio * 100));

        return colorGroups;
    }*/
    /**
     * Main quantization method: Groups similar colors together using Delta E distance.
     * UPDATED VERSION - handles complex images better
     */
    public List<ColorGroup> quantizeColors(List<ColorExtractor.ColorCount> extractedColors,
                                           double deltaEThreshold) {

        if (extractedColors == null || extractedColors.isEmpty()) {
            Logger.info("No colors to quantize - returning empty list");
            return new ArrayList<>();
        }

        Logger.info(String.format("Starting color quantization: %d input colors, Delta E threshold: %.1f",
                extractedColors.size(), deltaEThreshold));

        long startTime = System.currentTimeMillis();
        List<ColorGroup> colorGroups = new ArrayList<>();

        // Process colors in order of frequency (most common first)
        int processedColors = 0;
        for (ColorExtractor.ColorCount colorCount : extractedColors) {
            boolean addedToExistingGroup = false;

            // Try to find an existing group this color belongs to
            for (ColorGroup group : colorGroups) {
                double distance = calculateDeltaE(colorCount.getColor(), group.getRepresentativeColor());

                if (distance <= deltaEThreshold) {
                    group.addColor(colorCount);
                    addedToExistingGroup = true;
                    break; // Add to first matching group only
                }
            }

            // If no suitable group found, create a new one
            if (!addedToExistingGroup) {
                colorGroups.add(new ColorGroup(colorCount));
            }

            processedColors++;

            // Safety limit to prevent performance issues - but log what we're missing
            if (colorGroups.size() >= MAX_COLOR_GROUPS) {
                int remainingColors = extractedColors.size() - processedColors;
                double remainingCoverage = extractedColors.subList(processedColors, extractedColors.size())
                        .stream()
                        .mapToDouble(ColorExtractor.ColorCount::getPercentage)
                        .sum();

                Logger.info(String.format("Reached maximum color groups limit: %d (processed %d/%d colors, %.2f%% coverage remaining)",
                        MAX_COLOR_GROUPS, processedColors, extractedColors.size(), remainingCoverage));

                // If we're missing significant coverage, warn the user
                if (remainingCoverage > 10.0) {
                    Logger.info("WARNING: Significant color coverage (" + String.format("%.1f%%", remainingCoverage) +
                            ") was not processed. Consider using a higher Delta E threshold or processing fewer initial colors.");
                }
                break;
            }
        }

        // Sort groups by importance (total pixel count)
        Collections.sort(colorGroups);

        long processingTime = System.currentTimeMillis() - startTime;

        Logger.info(String.format("Color quantization complete: %d groups created in %dms",
                colorGroups.size(), processingTime));

        // Log quantization effectiveness
        int originalColors = extractedColors.size();
        int finalGroups = colorGroups.size();
        double compressionRatio = (originalColors > 0) ? (double) finalGroups / originalColors : 0;

        Logger.info(String.format("Quantization ratio: %d → %d colors (%.1f%% of original)",
                originalColors, finalGroups, compressionRatio * 100));

        return colorGroups;
    }

    /**
     * Quantize colors using the default Delta E threshold.
     */
    public List<ColorGroup> quantizeColors(List<ColorExtractor.ColorCount> extractedColors) {
        return quantizeColors(extractedColors, DEFAULT_DELTA_E_THRESHOLD);
    }

    /**
     * Calculates Delta E (CIE76) color difference using LAB color space.
     * This measures how different two colors appear to human eyes.
     *
     * Formula: ΔE = √[(ΔL)² + (Δa)² + (Δb)²]
     *
     * @param color1 First color
     * @param color2 Second color
     * @return Delta E distance (0 = identical, higher = more different)
     */
    private double calculateDeltaE(ColorEntry color1, ColorEntry color2) {
        // Get LAB values for both colors
        double[] lab1 = color1.getLAB();  // [L, A, B]
        double[] lab2 = color2.getLAB();

        // Calculate differences in each LAB component
        double deltaL = lab1[0] - lab2[0];  // Lightness difference
        double deltaA = lab1[1] - lab2[1];  // Green-Red axis difference
        double deltaB = lab1[2] - lab2[2];  // Blue-Yellow axis difference

        // Calculate Euclidean distance in LAB space
        return Math.sqrt(deltaL * deltaL + deltaA * deltaA + deltaB * deltaB);
    }

    /**
     * Extracts just the representative colors from quantized groups.
     * Useful for palette generation.
     */
    public List<ColorEntry> getRepresentativeColors(List<ColorGroup> colorGroups) {
        return colorGroups.stream()
                .map(ColorGroup::getRepresentativeColor)
                .toList();
    }

    /**
     * Provides detailed statistics about the quantization process.
     */
    public String getQuantizationReport(List<ColorExtractor.ColorCount> originalColors,
                                        List<ColorGroup> quantizedGroups) {

        if (originalColors.isEmpty()) {
            return "No colors to analyze";
        }

        int totalOriginalColors = originalColors.size();
        int totalGroups = quantizedGroups.size();
        double compressionRatio = (double) totalGroups / totalOriginalColors;

        // Find largest and smallest groups
        ColorGroup largestGroup = quantizedGroups.stream()
                .max(Comparator.comparingDouble(ColorGroup::getTotalPercentage))
                .orElse(null);

        ColorGroup smallestGroup = quantizedGroups.stream()
                .min(Comparator.comparingDouble(ColorGroup::getTotalPercentage))
                .orElse(null);

        StringBuilder report = new StringBuilder();
        report.append("ChromiQ Color Quantization Report\n");
        report.append("=================================\n");
        report.append(String.format("Original Colors: %,d\n", totalOriginalColors));
        report.append(String.format("Color Groups: %,d\n", totalGroups));
        report.append(String.format("Compression Ratio: %.1f%% (%.1fx reduction)\n",
                compressionRatio * 100, 1.0 / compressionRatio));
        report.append(String.format("Delta E Threshold: %.1f\n\n", DEFAULT_DELTA_E_THRESHOLD));

        if (largestGroup != null && smallestGroup != null) {
            report.append("Group Statistics:\n");
            report.append(String.format("• Largest group: %s (%.2f%%, %d original colors)\n",
                    largestGroup.getRepresentativeColor().getHex(),
                    largestGroup.getTotalPercentage(),
                    largestGroup.getColorCount()));

            report.append(String.format("• Smallest group: %s (%.2f%%, %d original colors)\n",
                    smallestGroup.getRepresentativeColor().getHex(),
                    smallestGroup.getTotalPercentage(),
                    smallestGroup.getColorCount()));
        }

        return report.toString();
    }
}