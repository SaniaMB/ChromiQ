package io.github.saniamb.chromiq.unit;

import io.github.saniamb.chromiq.core.color.ColorClusterer;
import io.github.saniamb.chromiq.core.color.ColorExtractor;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import java.util.*;

/**
 * Simple test to verify ColorClusterer works correctly.
 * This creates some test colors and sees if clustering groups them sensibly.
 */
public class ColorClustererTest {

    public static void main(String[] args) {
        System.out.println("🎨 Testing ColorClusterer...\n");

        // Create test colors that should form obvious groups
        List<ColorExtractor.ColorCount> testColors = createTestColors();

        System.out.println("📊 Input colors:");
        for (ColorExtractor.ColorCount cc : testColors) {
            System.out.println("  " + cc);
        }

        // Test clustering with k=3 (expect red, green, blue groups)
        ColorClusterer clusterer = new ColorClusterer();
        List<ColorClusterer.ColorCluster> clusters = clusterer.clusterColors(testColors, 3);

        System.out.println("\n🎯 Clustering Results (k=3):");
        for (int i = 0; i < clusters.size(); i++) {
            ColorClusterer.ColorCluster cluster = clusters.get(i);
            System.out.println(String.format("Cluster %d: %s", i + 1, cluster));

            // Show member colors
            for (ColorExtractor.ColorCount member : cluster.getMembers()) {
                System.out.println("    ↳ " + member.getColor().getHex() +
                        " (" + member.getCount() + " pixels)");
            }
            System.out.println();
        }

        // Test different k values
        System.out.println("🔄 Testing different k values:");
        for (int k = 2; k <= 5; k++) {
            List<ColorClusterer.ColorCluster> kClusters = clusterer.clusterColors(testColors, k);
            System.out.println(String.format("k=%d → %d clusters", k, kClusters.size()));
        }
    }

    /**
     * Creates test data with obvious color groups:
     * - Red family: bright red, dark red, pink
     * - Green family: bright green, dark green, lime
     * - Blue family: bright blue, dark blue, cyan
     */
    private static List<ColorExtractor.ColorCount> createTestColors() {
        List<ColorExtractor.ColorCount> colors = new ArrayList<>();

        // Red family (should cluster together)
        colors.add(createColorCount(255, 0, 0, 1000));     // Bright red - high pixel count
        colors.add(createColorCount(200, 0, 0, 500));      // Dark red
        colors.add(createColorCount(255, 100, 100, 300));  // Pink

        // Green family (should cluster together)
        colors.add(createColorCount(0, 255, 0, 800));      // Bright green
        colors.add(createColorCount(0, 150, 0, 400));      // Dark green
        colors.add(createColorCount(100, 255, 100, 200));  // Light green

        // Blue family (should cluster together)
        colors.add(createColorCount(0, 0, 255, 600));      // Bright blue
        colors.add(createColorCount(0, 0, 150, 350));      // Dark blue
        colors.add(createColorCount(100, 200, 255, 150));  // Light blue

        // Sort by pixel count (descending) like ColorExtractor does
        colors.sort((c1, c2) -> Integer.compare(c2.getCount(), c1.getCount()));

        return colors;
    }

    /**
     * Helper to create ColorCount objects for testing.
     */
    private static ColorExtractor.ColorCount createColorCount(int r, int g, int b, int pixelCount) {
        ColorEntry color = new ColorEntry(r, g, b);
        ColorExtractor.ColorCount colorCount = new ColorExtractor.ColorCount(color);

        // Manually set the count (normally done by ColorExtractor)
        for (int i = 1; i < pixelCount; i++) {
            colorCount.increment();
        }

        colorCount.setPercentage(pixelCount * 100); // Simplified percentage calculation
        return colorCount;
    }
}