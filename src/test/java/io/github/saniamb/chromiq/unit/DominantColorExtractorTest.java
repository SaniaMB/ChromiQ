package io.github.saniamb.chromiq.unit;

import io.github.saniamb.chromiq.core.color.DominantColorExtractor;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DominantColorExtractor.
 *
 * Tests the complete color extraction pipeline including:
 * - Integration of ColorExtractor + ColorClusterer
 * - Smart clustering decisions
 * - Edge case handling
 * - Performance characteristics
 */
public class DominantColorExtractorTest {

    private DominantColorExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new DominantColorExtractor();
    }

    // === BASIC FUNCTIONALITY TESTS ===

    @Test
    @DisplayName("Should extract dominant colors from simple image")
    void testBasicExtraction() {
        // Create a simple 3-color image: 50% red, 30% blue, 20% green
        BufferedImage image = createTestImage(100, 100, new Color[]{
                Color.RED,   // 5000 pixels
                Color.BLUE,  // 3000 pixels
                Color.GREEN  // 2000 pixels
        }, new int[]{50, 30, 20});

        List<DominantColorExtractor.DominantColor> colors = extractor.extractDominantColors(image);

        // Should return all 3 colors since we have fewer than 10
        assertEquals(3, colors.size(), "Should return all 3 colors from simple image");

        // Should be sorted by dominance (red first, then blue, then green)
        assertEquals("#FF0000", colors.get(0).getColor().getHex(), "Most dominant should be red");
        assertEquals("#0000FF", colors.get(1).getColor().getHex(), "Second should be blue");
        assertEquals("#00FF00", colors.get(2).getColor().getHex(), "Third should be green");

        // Check percentages are approximately correct
        assertTrue(colors.get(0).getPercentage() > 45, "Red should be ~50%");
        assertTrue(colors.get(1).getPercentage() > 25, "Blue should be ~30%");
        assertTrue(colors.get(2).getPercentage() > 15, "Green should be ~20%");
    }

    @Test
    @DisplayName("Should respect maxColors parameter")
    void testMaxColorsLimit() {
        // Create image with 6 distinct colors
        BufferedImage image = createRainbowImage(60, 60, 6);

        // Request only 3 colors
        List<DominantColorExtractor.DominantColor> colors = extractor.extractDominantColors(image, 3);

        assertEquals(3, colors.size(), "Should return exactly 3 colors when requested");

        // Test different limits
        assertEquals(1, extractor.extractDominantColors(image, 1).size());
        assertEquals(5, extractor.extractDominantColors(image, 5).size());
    }

    // === EDGE CASE TESTS ===

    @Test
    @DisplayName("Should handle solid color image")
    void testSolidColorImage() {
        BufferedImage image = createSolidColorImage(100, 100, Color.BLUE);

        List<DominantColorExtractor.DominantColor> colors = extractor.extractDominantColors(image);

        assertEquals(1, colors.size(), "Solid color image should return 1 color");
        assertEquals("#0000FF", colors.get(0).getColor().getHex(), "Should be blue");
        assertTrue(colors.get(0).getPercentage() > 99, "Should be nearly 100%");
    }

    @Test
    @DisplayName("Should handle completely transparent image")
    void testTransparentImage() {
        BufferedImage image = createTransparentImage(50, 50);

        List<DominantColorExtractor.DominantColor> colors = extractor.extractDominantColors(image);

        assertEquals(0, colors.size(), "Transparent image should return no colors");
    }

    @Test
    @DisplayName("Should handle image with transparent regions")
    void testPartiallyTransparentImage() {
        // Create image: 50% red, 50% transparent
        BufferedImage image = createPartiallyTransparentImage(100, 100, Color.RED, 0.5f);

        List<DominantColorExtractor.DominantColor> colors = extractor.extractDominantColors(image);

        assertEquals(1, colors.size(), "Should find 1 color in partially transparent image");
        assertEquals("#FF0000", colors.get(0).getColor().getHex(), "Should be red");
        assertTrue(colors.get(0).getPercentage() > 99, "Should be ~100% of visible pixels");
    }

    // === SMART LOGIC TESTS ===

    @Test
    @DisplayName("Should use natural palette for images with few colors")
    void testNaturalPaletteLogic() {
        // Create image with exactly 8 distinct colors (below clustering threshold)
        BufferedImage image = createRainbowImage(80, 80, 8);

        List<DominantColorExtractor.DominantColor> colors = extractor.extractDominantColors(image, 10);

        // Should return all 8 colors without clustering
        assertEquals(8, colors.size(), "Should return all 8 colors without clustering");

        // Colors should be distinct (no clustering artifacts)
        Set<String> hexColors = new HashSet<>();
        for (DominantColorExtractor.DominantColor dc : colors) {
            hexColors.add(dc.getColor().getHex());
        }
        assertEquals(8, hexColors.size(), "All colors should be distinct");
    }

    @Test
    @DisplayName("Should use clustering for images with many colors")
    void testClusteringLogic() {
        // Create complex gradient image with many similar colors
        BufferedImage image = createGradientImage(100, 100, Color.RED, Color.BLUE);

        List<DominantColorExtractor.DominantColor> colors = extractor.extractDominantColors(image, 5);

        assertEquals(5, colors.size(), "Should return exactly 5 clustered colors");

        // Verify colors are reasonably spaced (clustering worked)
        // In a red-to-blue gradient, we expect colors across the spectrum
        boolean hasReddish = colors.stream().anyMatch(c -> c.getColor().getRed() > 150);
        boolean hasBluish = colors.stream().anyMatch(c -> c.getColor().getBlue() > 150);

        assertTrue(hasReddish, "Should have reddish colors from gradient");
        assertTrue(hasBluish, "Should have bluish colors from gradient");
    }

    // === VALIDATION TESTS ===

    @Test
    @DisplayName("Should throw exception for null image")
    void testNullImageValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            extractor.extractDominantColors(null);
        }, "Should throw exception for null image");
    }

    @Test
    @DisplayName("Should throw exception for invalid maxColors")
    void testInvalidMaxColorsValidation() {
        BufferedImage image = createSolidColorImage(10, 10, Color.RED);

        assertThrows(IllegalArgumentException.class, () -> {
            extractor.extractDominantColors(image, 0);
        }, "Should throw exception for maxColors = 0");

        assertThrows(IllegalArgumentException.class, () -> {
            extractor.extractDominantColors(image, 11);
        }, "Should throw exception for maxColors > 10");

        assertThrows(IllegalArgumentException.class, () -> {
            extractor.extractDominantColors(image, -5);
        }, "Should throw exception for negative maxColors");
    }

    // === PERFORMANCE TESTS ===

    @Test
    @DisplayName("Should process large image in reasonable time")
    void testPerformance() {
        // Create a reasonably large complex image
        BufferedImage image = createGradientImage(500, 400, Color.RED, Color.YELLOW);

        long startTime = System.currentTimeMillis();
        List<DominantColorExtractor.DominantColor> colors = extractor.extractDominantColors(image);
        long processingTime = System.currentTimeMillis() - startTime;

        assertFalse(colors.isEmpty(), "Should extract colors from large image");
        assertTrue(processingTime < 5000, "Should process 500x400 image in under 5 seconds, took: " + processingTime + "ms");
    }

    // === INTEGRATION TESTS ===

    @Test
    @DisplayName("Should produce consistent results across multiple runs")
    void testConsistency() {
        BufferedImage image = createTestImage(50, 50, new Color[]{Color.RED, Color.BLUE}, new int[]{70, 30});

        // Run extraction multiple times
        List<DominantColorExtractor.DominantColor> run1 = extractor.extractDominantColors(image);
        List<DominantColorExtractor.DominantColor> run2 = extractor.extractDominantColors(image);
        List<DominantColorExtractor.DominantColor> run3 = extractor.extractDominantColors(image);

        // Results should be identical
        assertEquals(run1.size(), run2.size(), "Run 1 and 2 should have same color count");
        assertEquals(run1.size(), run3.size(), "Run 1 and 3 should have same color count");

        for (int i = 0; i < run1.size(); i++) {
            assertEquals(run1.get(i).getColor().getHex(), run2.get(i).getColor().getHex(),
                    "Color " + i + " should be same in runs 1 and 2");
            assertEquals(run1.get(i).getColor().getHex(), run3.get(i).getColor().getHex(),
                    "Color " + i + " should be same in runs 1 and 3");
        }
    }

    @Test
    @DisplayName("Should generate meaningful extraction report")
    void testExtractionReport() {
        BufferedImage image = createTestImage(100, 100, new Color[]{Color.RED, Color.GREEN, Color.BLUE},
                new int[]{50, 30, 20});

        List<DominantColorExtractor.DominantColor> colors = extractor.extractDominantColors(image);
        String report = extractor.getExtractionReport(image, colors);

        assertNotNull(report, "Report should not be null");
        assertTrue(report.contains("100x100"), "Report should mention image dimensions");
        assertTrue(report.contains("#FF0000"), "Report should contain red color");
        assertTrue(report.contains("Dominant Colors Found: 3"), "Report should show color count");
    }

    // === HELPER METHODS FOR CREATING TEST IMAGES ===

    /**
     * Creates test image with specific colors and their distributions.
     */
    private BufferedImage createTestImage(int width, int height, Color[] colors, int[] percentages) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        int totalPixels = width * height;
        int currentPixel = 0;

        for (int i = 0; i < colors.length; i++) {
            int pixelsForThisColor = (totalPixels * percentages[i]) / 100;

            g2d.setColor(colors[i]);

            // Fill pixels for this color
            for (int p = 0; p < pixelsForThisColor && currentPixel < totalPixels; p++) {
                int x = currentPixel % width;
                int y = currentPixel / width;
                g2d.fillRect(x, y, 1, 1);
                currentPixel++;
            }
        }

        g2d.dispose();
        return image;
    }

    /**
     * Creates a solid color image.
     */
    private BufferedImage createSolidColorImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return image;
    }

    /**
     * Creates completely transparent image.
     */
    private BufferedImage createTransparentImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // Leave it transparent (default alpha = 0)
        return image;
    }

    /**
     * Creates image that's partially transparent.
     */
    private BufferedImage createPartiallyTransparentImage(int width, int height, Color color, float transparency) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Fill half with color, half transparent
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, (int)(height * transparency));

        g2d.dispose();
        return image;
    }

    /**
     * Creates rainbow image with N distinct colors.
     */
    private BufferedImage createRainbowImage(int width, int height, int colorCount) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        int stripWidth = width / colorCount;

        for (int i = 0; i < colorCount; i++) {
            // Create evenly spaced hue values
            float hue = (float) i / colorCount;
            Color color = Color.getHSBColor(hue, 1.0f, 1.0f);

            g2d.setColor(color);
            g2d.fillRect(i * stripWidth, 0, stripWidth, height);
        }

        g2d.dispose();
        return image;
    }

    /**
     * Creates gradient image (many similar colors - good for testing clustering).
     */
    private BufferedImage createGradientImage(int width, int height, Color startColor, Color endColor) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        for (int x = 0; x < width; x++) {
            // Calculate blend ratio
            float ratio = (float) x / (width - 1);

            // Interpolate between start and end colors
            int r = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
            int g = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
            int b = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);

            Color blendedColor = new Color(r, g, b);
            g2d.setColor(blendedColor);
            g2d.fillRect(x, 0, 1, height);
        }

        g2d.dispose();
        return image;
    }
}