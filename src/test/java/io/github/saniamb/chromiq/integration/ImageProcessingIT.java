package io.github.saniamb.chromiq.integration;

import io.github.saniamb.chromiq.core.input.ImageInputHandler;
import io.github.saniamb.chromiq.core.color.ColorExtractor;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.color.ColorExtractor.ColorCount;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Integration Tests for ImageInputHandler + ColorExtractor Pipeline
 *
 * Tests the complete flow:
 * Real Image File → ImageInputHandler → BufferedImage → ColorExtractor → Color Analysis
 *
 * This verifies that our core pipeline works with actual image data,
 * not just synthetic test cases.
 */
public class ImageProcessingIT {

    private ImageInputHandler imageHandler;
    private ColorExtractor colorExtractor;

    @BeforeEach
    void setUp() {
        imageHandler = new ImageInputHandler();
        colorExtractor = new ColorExtractor();
    }

    @Test
    @DisplayName("Integration: Load solid color image and extract its color")
    void testSolidColorImagePipeline() throws IOException {
        // Step 1: Load image through ImageInputHandler
        InputStream imageStream = getClass().getResourceAsStream("/test-images/solid-blue.png");
        assertNotNull(imageStream, "Test image 'solid-blue.png' not found in test resources");

        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);

        // Verify ImageInputHandler did its job
        assertNotNull(processedImage, "ImageInputHandler should return a valid BufferedImage");
        assertEquals(BufferedImage.TYPE_INT_ARGB, processedImage.getType(),
                "ImageInputHandler should convert image to ARGB format");

        // Step 2: Extract colors using ColorExtractor
        List<ColorCount> colors = colorExtractor.extractColors(processedImage);

        // Verify ColorExtractor results
        assertFalse(colors.isEmpty(), "ColorExtractor should find at least one color");

        // For a solid blue image, we expect very few unique colors (ideally 1)
        assertTrue(colors.size() <= 5,
                "Solid color image should have very few unique colors, found: " + colors.size());

        // The most common color should be some shade of red
        ColorCount dominantColor = colors.get(0);
        ColorEntry blueColor = dominantColor.getColor();

        // Red component should be significantly higher than green and blue
        assertTrue(blueColor.getBlue() > blueColor.getGreen() + 50,
                "Dominant color should be blueish: " + blueColor.getHex());
        assertTrue(blueColor.getBlue() > blueColor.getRed() + 50,
                "Dominant color should be blueish: " + blueColor.getHex());

        System.out.println("✅ Solid Color Test Results:");
        System.out.println("   Image size: " + processedImage.getWidth() + "x" + processedImage.getHeight());
        System.out.println("   Unique colors found: " + colors.size());
        System.out.println("   Dominant color: " + dominantColor);
    }

    @Test
    @DisplayName("Integration: Load and process a complex real-world image")
    void testComplexImagePipeline() throws IOException {
        // This test will use a more complex image with multiple colors
        InputStream imageStream = getClass().getResourceAsStream("/test-images/photo.jpeg");

        if (imageStream == null) {
            System.out.println("⚠️ Skipping complex image test - photo.jpeg not found");
            return; // Skip test if image not available
        }

        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);
        List<ColorCount> colors = colorExtractor.extractColors(processedImage);

        // Complex images should have many colors
        assertTrue(colors.size() > 200,
                "Complex image should have many unique colors, found: " + colors.size());

        // Verify the top colors have reasonable percentages
        ColorCount topColor = colors.get(0);
        assertTrue(topColor.getPercentage() > 0.1,
                "Top color should represent at least 0.1% of pixels");
        assertTrue(topColor.getPercentage() < 50.0,
                "Top color shouldn't dominate more than 50% of pixels in a complex image");

        System.out.println("✅ Complex Image Test Results:");
        System.out.println("   Image size: " + processedImage.getWidth() + "x" + processedImage.getHeight());
        System.out.println("   Unique colors found: " + colors.size());
        System.out.println("   Top 3 colors:");
        for (int i = 0; i < Math.min(3, colors.size()); i++) {
            System.out.println("     " + (i+1) + ". " + colors.get(i));
        }
    }

    @Test
    @DisplayName("Integration: Verify image resizing works correctly")
    void testImageResizingIntegration() throws IOException {
        // We'll test this with any available image
        InputStream imageStream = getClass().getResourceAsStream("/test-images/solid-blue.png");
        assumeImageExists(imageStream, "solid-blue.png");

        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);

        // Verify image is within size limits (ImageInputHandler should resize if needed)
        assertTrue(processedImage.getWidth() <= 1920,
                "Image width should be <= 1920 after processing");
        assertTrue(processedImage.getHeight() <= 1080,
                "Image height should be <= 1080 after processing");

        // Color extraction should still work on resized image
        List<ColorCount> colors = colorExtractor.extractColors(processedImage);
        assertFalse(colors.isEmpty(), "Color extraction should work on resized images");

        System.out.println("✅ Resize Integration Test:");
        System.out.println("   Final image size: " + processedImage.getWidth() + "x" + processedImage.getHeight());
        System.out.println("   Colors extracted successfully: " + colors.size());
    }

    @Test
    @DisplayName("Integration: Test complete pipeline performance")
    void testPipelinePerformance() throws IOException {
        InputStream imageStream = getClass().getResourceAsStream("/test-images/solid-blue.png");
        assumeImageExists(imageStream, "solid-blue.png");

        long startTime = System.currentTimeMillis();

        // Complete pipeline
        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);
        List<ColorCount> colors = colorExtractor.extractColors(processedImage);
        String statistics = colorExtractor.getColorStatistics(processedImage);

        long totalTime = System.currentTimeMillis() - startTime;

        // The Pipeline should complete reasonably quickly (under 1 second for small images)
        assertTrue(totalTime < 1000,
                "Complete pipeline should execute quickly, took: " + totalTime + "ms");

        System.out.println("✅ Performance Test:");
        System.out.println("   Total pipeline time: " + totalTime + "ms");
        System.out.println("   Colors found: " + colors.size());
        System.out.println("\n" + statistics);
    }

    @Test
    @DisplayName("Integration: Load and process a transparent logo image")
    void testTransparentImagePipeline() throws IOException {
        // Step 1: Load image
        InputStream imageStream = getClass().getResourceAsStream("/images/test-transparent.png");
        assertNotNull(imageStream, "Test image 'transparent.png' not found in test resources");

        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);

        // Verify transparency is preserved
        assertEquals(BufferedImage.TYPE_INT_ARGB, processedImage.getType(),
                "Transparent image should be loaded as ARGB to keep alpha channel");

        // Step 2: Extract colors
        List<ColorCount> colors = colorExtractor.extractColors(processedImage);

        // Transparent logos usually contain multiple colors + transparent background
        assertFalse(colors.isEmpty(), "ColorExtractor should find colors in transparent logo");

        // Ensure we have a decent variety of colors (not just background)
        assertTrue(colors.size() > 1,
                "This transparent image should have minimal colors, found: " + colors.size());

        // Step 3: Check transparency
        boolean hasTransparentPixels = false;
        for (int y = 0; y < processedImage.getHeight() && !hasTransparentPixels; y++) {
            for (int x = 0; x < processedImage.getWidth(); x++) {
                int alpha = (processedImage.getRGB(x, y) >> 24) & 0xff;
                if (alpha < 255) {
                    hasTransparentPixels = true;
                    break;
                }
            }
        }
        assertTrue(hasTransparentPixels, "Image should contain some transparent pixels");

        // Step 4: Dominant color sanity checks
        ColorCount topColor = colors.get(0);
        assertTrue(topColor.getPercentage() > 0.1,
                "Top color should represent at least 0.1% of pixels");
        assertTrue(topColor.getPercentage() > 50.0,
                "Transparent background should not dominate excessively");

        System.out.println("✅ Transparent Logo Test Results:");
        System.out.println("   Image size: " + processedImage.getWidth() + "x" + processedImage.getHeight());
        System.out.println("   Unique colors found: " + colors.size());
        System.out.println("   Has transparency: " + hasTransparentPixels);
        System.out.println("   Top 3 colors:");
        for (int i = 0; i < Math.min(3, colors.size()); i++) {
            System.out.println("     " + (i+1) + ". " + colors.get(i));
        }
    }

    @Test
    @DisplayName("Integration: Load and process a multi-color gradient image")
    void testMultiColorGradientPipeline() throws IOException {
        // Load the gradient image from test resources
        InputStream imageStream = getClass().getResourceAsStream("/test-images/gradient.jpeg");

        if (imageStream == null) {
            System.out.println("⚠️ Skipping gradient image test - multi-gradient.png not found");
            return; // Skip test if image not available
        }

        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);
        List<ColorCount> colors = colorExtractor.extractColors(processedImage);

        // Multi-color gradients should yield a good variety of unique colors
        assertTrue(colors.size() > 50,
                "Multi-color gradient should have many unique colors, found: " + colors.size());

        // No single color should dominate — distribution should be spread out
        ColorCount topColor = colors.get(0);
        assertTrue(topColor.getPercentage() < 20.0,
                "No single color in a gradient should dominate more than 20%, got: " + topColor.getPercentage());

        // The gradient should include at least a handful of dominant color regions
        int topN = Math.min(20, colors.size());
        double coverage = 0;
        for (int i = 0; i < topN; i++) {
            coverage += colors.get(i).getPercentage();
        }
        assertTrue(coverage > 0.05,
                "Top " + topN + " colors should together cover a noticeable portion of the gradient, got: " + coverage + "%");

        System.out.println("✅ Multi-Color Gradient Test Results:");
        System.out.println("   Image size: " + processedImage.getWidth() + "x" + processedImage.getHeight());
        System.out.println("   Unique colors found: " + colors.size());
        System.out.println("   Top " + topN + " colors:");
        for (int i = 0; i < topN; i++) {
            System.out.println("     " + (i+1) + ". " + colors.get(i));
        }
    }

    @Test
    @DisplayName("Integration: Load and process an image of an icon with minimal colors")
    void testIcon() throws IOException {

        InputStream imageStream = getClass().getResourceAsStream("/test-images/small-icon.png");
        assumeImageExists(imageStream, "small-icon.png");
        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);

        // Verify ImageInputHandler did its job
        assertNotNull(processedImage, "ImageInputHandler should return a valid BufferedImage");
        assertEquals(BufferedImage.TYPE_INT_ARGB, processedImage.getType(),
                "ImageInputHandler should convert image to ARGB format");

        // Step 2: Extract colors using ColorExtractor
        List<ColorCount> colors = colorExtractor.extractColors(processedImage);

        // Verify ColorExtractor results
        assertFalse(colors.isEmpty(), "ColorExtractor should find at least one color");

        // The most common color should be some shade of red
        ColorCount dominantColor = colors.get(0);
        ColorEntry blueColor = dominantColor.getColor();

        System.out.println("✅ Solid Color Test Results:");
        System.out.println("   Image size: " + processedImage.getWidth() + "x" + processedImage.getHeight());
        System.out.println("   Unique colors found: " + colors.size());
        System.out.println("   Dominant color: " + dominantColor);

        for (int i = 0; i < Math.min(20, colors.size()); i++) {
            ColorCount cc = colors.get(i);
            System.out.println("     " + (i+1) + ". " + cc.getColor());
        }
    }

    // Helper method to skip tests gracefully if test images aren't available
    private void assumeImageExists(InputStream imageStream, String imageName) {
        if (imageStream == null) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                    "Skipping test - " + imageName + " not found in test resources");
        }
    }
}