package io.github.saniamb.chromiq.color;

import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.color.ColorExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Comprehensive tests for ColorExtractor class.
 *
 * Tests cover:
 * - Basic color extraction functionality
 * - Edge cases (null images, empty images)
 * - Different image scenarios (solid colors, gradients, transparency)
 * - Performance with various image sizes
 * - Statistical accuracy
 */
public class ColorExtractorTest {

    private ColorExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ColorExtractor();
    }

    // ======================
    // BASIC FUNCTIONALITY TESTS
    // ======================

    @Test
    @DisplayName("Should throw exception when image is null")
    void testNullImage() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extractor.extractColors(null)
        );
        assertEquals("Image cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should extract single color from solid color image")
    void testSolidColorExtraction() {
        // Create a 10x10 solid red image
        BufferedImage image = createSolidColorImage(10, 10, Color.RED);

        List<ColorExtractor.ColorCount> colors = extractor.extractColors(image);

        // Should find exactly one color
        assertEquals(1, colors.size(), "Should find exactly one color in solid image");

        ColorExtractor.ColorCount redColor = colors.get(0);
        assertEquals(100, redColor.getCount(), "Should count all 100 pixels");
        assertEquals(100.0, redColor.getPercentage(), 0.01, "Should be 100% of pixels");

        // Verify it's actually red
        ColorEntry color = redColor.getColor();
        assertEquals(255, color.getRed());
        assertEquals(0, color.getGreen());
        assertEquals(0, color.getBlue());
    }

    @Test
    @DisplayName("Should handle multi-color image correctly")
    void testMultiColorExtraction() {
        // Create a 2x2 image with 4 different colors
        BufferedImage image = createQuadrantImage();

        List<ColorExtractor.ColorCount> colors = extractor.extractColors(image);

        // 🔍 Debug print
        System.out.println("Extracted colors:");
        for (ColorExtractor.ColorCount cc : colors) {
            ColorEntry ce = cc.getColor();
            System.out.printf(
                    "R=%d G=%d B=%d A=%.2f Count=%d Percentage=%.2f%n",
                    ce.getRed(), ce.getGreen(), ce.getBlue(), ce.getAlpha(),
                    cc.getCount(), cc.getPercentage()
            );
        }

        // Should find exactly 4 colors
        assertEquals(4, colors.size(), "Should find 4 unique colors");

        // Each color should appear exactly once (25% each)
        for (ColorExtractor.ColorCount colorCount : colors) {
            assertEquals(1, colorCount.getCount(), "Each color should appear once");
            assertEquals(25.0, colorCount.getPercentage(), 0.01, "Each color should be 25%");
        }
    }



    @Test
    @DisplayName("Should handle transparent pixels correctly")
    void testTransparencyHandling() {
        BufferedImage image = createTransparentImage();

        List<ColorExtractor.ColorCount> colors = extractor.extractColors(image);

        // Should only count opaque pixels, ignore transparent ones
        assertTrue(colors.size() > 0, "Should find some colors despite transparency");

        // Total visible pixels should be less than total pixels
        int totalVisiblePixels = colors.stream().mapToInt(ColorExtractor.ColorCount::getCount).sum();
        int totalPixels = image.getWidth() * image.getHeight();
        assertTrue(totalVisiblePixels < totalPixels, "Should have fewer visible pixels due to transparency");
    }

    // ======================
    // PERFORMANCE TESTS
    // ======================

    @Test
    @DisplayName("Should handle large images efficiently")
    void testLargeImagePerformance() {
        // Create a larger image (500x500 = 250,000 pixels)
        BufferedImage largeImage = createGradientImage(500, 500);

        long startTime = System.currentTimeMillis();
        List<ColorExtractor.ColorCount> colors = extractor.extractColors(largeImage);
        long endTime = System.currentTimeMillis();

        // Should complete within reasonable time (adjust the threshold as needed)
        long processingTime = endTime - startTime;
        assertTrue(processingTime < 5000, "Should process large image within 5 seconds, took: " + processingTime + "ms");

        // Should find many colors in gradient
        assertTrue(colors.size() > 100, "Gradient should produce many unique colors");
    }

    // ======================
    // UTILITY METHOD TESTS
    // ======================

    @Test
    @DisplayName("Should return correct unique colors list")
    void testExtractUniqueColors() {
        BufferedImage image = createQuadrantImage();

        List<ColorEntry> uniqueColors = extractor.extractUniqueColors(image);

        assertEquals(4, uniqueColors.size(), "Should return 4 unique colors");

        // All colors should be different
        for (int i = 0; i < uniqueColors.size(); i++) {
            for (int j = i + 1; j < uniqueColors.size(); j++) {
                assertNotEquals(uniqueColors.get(i), uniqueColors.get(j),
                        "All colors should be unique");
            }
        }
    }

    @Test
    @DisplayName("Should limit colors correctly in extractTopColors")
    void testExtractTopColors() {
        BufferedImage image = createGradientImage(50, 50); // Many colors

        List<ColorExtractor.ColorCount> topColors = extractor.extractTopColors(image, 10);

        assertEquals(10, topColors.size(), "Should return exactly 10 colors");

        // Should be sorted by frequency (most common first)
        for (int i = 0; i < topColors.size() - 1; i++) {
            assertTrue(topColors.get(i).getCount() >= topColors.get(i + 1).getCount(),
                    "Colors should be sorted by frequency (descending)");
        }
    }

    @Test
    @DisplayName("Should generate meaningful color statistics")
    void testColorStatistics() {
        BufferedImage image = createSolidColorImage(10, 10, Color.BLUE);

        String stats = extractor.getColorStatistics(image);

        assertNotNull(stats, "Statistics should not be null");
        assertTrue(stats.contains("ChromiQ Color Analysis Report"), "Should contain report header");
        assertTrue(stats.contains("100 total"), "Should mention total pixel count");
        assertTrue(stats.contains("Unique Colors: 1"), "Should mention unique color count");
        assertTrue(stats.contains("#0000FF"), "Should mention the blue color hex");
    }

    // ======================
    // HELPER METHODS FOR TEST IMAGE CREATION
    // ======================

    /**
     * Creates a solid color image for testing.
     *
     * @param width Image width
     * @param height Image height
     * @param color The solid color to fill
     * @return BufferedImage filled with the specified color
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
     * Creates a 2x2 image with 4 different colored quadrants.
     * Top-left: Red, Top-right: Green, Bottom-left: Blue, Bottom-right: Yellow
     */
    private BufferedImage createQuadrantImage() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);

        // Set individual pixels
        image.setRGB(0, 0, Color.RED.getRGB());     // Top-left
        image.setRGB(1, 0, Color.GREEN.getRGB());   // Top-right
        image.setRGB(0, 1, Color.BLUE.getRGB());    // Bottom-left
        image.setRGB(1, 1, Color.YELLOW.getRGB());  // Bottom-right

        return image;
    }

    /**
     * Creates an image with some transparent pixels.
     */
    private BufferedImage createTransparentImage() {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Fill with solid color first
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, 4, 4);

        // Make some pixels transparent (alpha = 0)
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(2, 2, 2, 2); // Bottom-right quarter transparent

        g2d.dispose();
        return image;
    }

    /**
     * Creates a gradient image for testing with many colors.
     */
    private BufferedImage createGradientImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Create gradient from black to white based on position
                int intensity = (int) (255.0 * x / width);
                int colorValue = (255 << 24) | (intensity << 16) | (intensity << 8) | intensity;
                image.setRGB(x, y, colorValue);
            }
        }

        return image;
    }
}