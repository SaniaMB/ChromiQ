package io.github.saniamb.chromiq.integration;


import io.github.saniamb.chromiq.core.color.ColorExtractor;
import io.github.saniamb.chromiq.core.color.ColorQuantizer;
import io.github.saniamb.chromiq.core.input.ImageInputHandler;
import io.github.saniamb.chromiq.core.utils.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ColorQuantizer using real image files.
 *
 * Tests the complete pipeline: ImageInputHandler → ColorExtractor → ColorQuantizer
 *
 * Place your test images in: src/test/resources/images/
 * Expected images:
 * - solid_red.png (or similar solid color image)
 * - transparent.png (image with transparency)
 * - gradient.png (color gradient image)
 * - photo.jpg (real photograph)
 */
public class ColorQuantizerIntegrationTest {

    private ImageInputHandler imageHandler;
    private ColorExtractor colorExtractor;
    private ColorQuantizer colorQuantizer;

    // Test image paths (relative to src/test/resources/)
    private static final String IMAGES_DIR = "/test-images/";
    private static final String SOLID_IMAGE = IMAGES_DIR + "solid-blue.png";
    private static final String TRANSPARENT_IMAGE = IMAGES_DIR + "transparent.jpg";
    private static final String GRADIENT_IMAGE = IMAGES_DIR + "gradient.jpeg";
    private static final String PHOTO_IMAGE = IMAGES_DIR + "photo.jpeg";

    @BeforeEach
    void setUp() {
        imageHandler = new ImageInputHandler();
        colorExtractor = new ColorExtractor();
        colorQuantizer = new ColorQuantizer();

        Logger.info("=== Starting ColorQuantizer Integration Tests ===");
    }

    /**
     * Helper method to run the complete color processing pipeline on an image.
     * This simulates what a real ChromiQ user would experience.
     */
    private PipelineResult processImage(String imagePath, String testName) throws IOException {
        Logger.info("\n--- Testing: " + testName + " ---");
        Logger.info("Image: " + imagePath);

        long totalStartTime = System.currentTimeMillis();

        // Step 1: Load image
        BufferedImage image = imageHandler.loadFromStream(
                getClass().getResourceAsStream(imagePath)
        );
        assertNotNull(image, "Failed to load image: " + imagePath);
        Logger.info("✅ Image loaded: " + imageHandler.getImageInfo(image));

        // Step 2: Extract colors
        List<ColorExtractor.ColorCount> extractedColors = colorExtractor.extractColors(image);
        assertNotNull(extractedColors, "Color extraction failed");
        Logger.info("✅ Colors extracted: " + extractedColors.size() + " unique colors");

        // Step 3: Quantize colors
        List<ColorQuantizer.ColorGroup> colorGroups = colorQuantizer.quantizeColors(extractedColors);
        assertNotNull(colorGroups, "Color quantization failed");
        Logger.info("✅ Colors quantized: " + colorGroups.size() + " color groups");

        long totalTime = System.currentTimeMillis() - totalStartTime;
        Logger.info("⏱️ Total pipeline time: " + totalTime + "ms");

        return new PipelineResult(image, extractedColors, colorGroups, totalTime);
    }

    /**
     * Helper class to hold results from the complete pipeline
     */
    private record PipelineResult(
            BufferedImage image,
            List<ColorExtractor.ColorCount> extractedColors,
            List<ColorQuantizer.ColorGroup> colorGroups,
            long processingTimeMs
    ) {
        public void printDetailedReport() {
            Logger.info("\n📊 DETAILED PIPELINE REPORT:");
            Logger.info("Image size: " + image.getWidth() + "x" + image.getHeight());
            Logger.info("Total pixels: " + (image.getWidth() * image.getHeight()));
            Logger.info("Unique colors extracted: " + extractedColors.size());
            Logger.info("Color groups after quantization: " + colorGroups.size());

            if (!extractedColors.isEmpty()) {
                double compressionRatio = (double) colorGroups.size() / extractedColors.size();
                Logger.info("Compression ratio: " + String.format("%.1f%%", compressionRatio * 100));
            }

            Logger.info("Processing time: " + processingTimeMs + "ms");

            // Show top 5 color groups
            Logger.info("\n🎨 TOP 5 COLOR GROUPS:");
            for (int i = 0; i < Math.min(5, colorGroups.size()); i++) {
                ColorQuantizer.ColorGroup group = colorGroups.get(i);
                Logger.info(String.format("%d. %s - %.2f%% (%d pixels, %d original colors)",
                        i + 1,
                        group.getRepresentativeColor().getHex(),
                        group.getTotalPercentage(),
                        group.getTotalCount(),
                        group.getColorCount()
                ));
            }
        }
    }

    @Test
    @DisplayName("Solid Color Image - Should produce minimal groups")
    void testSolidColorImage() throws IOException {
        PipelineResult result = processImage(SOLID_IMAGE, "Solid Color Image");

        // Assertions for solid color image
        assertTrue(result.colorGroups().size() <= 5,
                "Solid color image should produce very few color groups, got: " + result.colorGroups().size());

        // The dominant group should contain most pixels
        if (!result.colorGroups().isEmpty()) {
            ColorQuantizer.ColorGroup dominantGroup = result.colorGroups().get(0);
            assertTrue(dominantGroup.getTotalPercentage() > 80.0,
                    "Dominant color should represent >80% of pixels in solid image, got: " +
                            dominantGroup.getTotalPercentage() + "%");
        }

        result.printDetailedReport();
    }

    @Test
    @DisplayName("Transparent Image - Should handle alpha properly")
    void testTransparentImage() throws IOException {
        PipelineResult result = processImage("/images/test-transparent.png", "Transparent Image");

        // Transparent images might have fewer visible colors
        assertFalse(result.extractedColors().isEmpty(),
                "Should extract some visible colors from transparent image");

        // Quantization should still work
        assertFalse(result.colorGroups().isEmpty(),
                "Should create color groups from visible pixels");

        result.printDetailedReport();
    }

    @Test
    @DisplayName("Gradient Image - Should show quantization effectiveness")
    void testGradientImage() throws IOException {
        PipelineResult result = processImage(GRADIENT_IMAGE, "Gradient Image");

        // Gradients typically have many similar colors - perfect for quantization
        assertTrue(result.extractedColors().size() > 50,
                "Gradient should have many extracted colors, got: " + result.extractedColors().size());

        // Quantization should significantly reduce the number
        assertTrue(result.colorGroups().size() < result.extractedColors().size() / 2,
                "Quantization should significantly reduce colors from " + result.extractedColors().size() +
                        " to " + result.colorGroups().size());

        result.printDetailedReport();
    }

    @Test
    @DisplayName("Real Photo - Should demonstrate practical quantization")
    void testRealPhoto() throws IOException {
        PipelineResult result = processImage(PHOTO_IMAGE, "Real Photograph");

        // Real photos typically have thousands of unique colors
        assertTrue(result.extractedColors().size() > 100,
                "Real photo should have many colors, got: " + result.extractedColors().size());

        // After quantization, should be manageable for palette generation
        assertTrue(result.colorGroups().size() <= 100,
                "Quantized photo should have ≤100 color groups, got: " + result.colorGroups().size());

        // Performance check - should process reasonably quickly
        assertTrue(result.processingTimeMs() < 5000,
                "Photo processing should complete in <5 seconds, took: " + result.processingTimeMs() + "ms");

        result.printDetailedReport();
    }

    @Test
    @DisplayName("Performance Test - Large image handling")
    void testPerformanceWithLargeImage() throws IOException {
        // This will test ImageInputHandler's resizing + full pipeline
        PipelineResult result = processImage(PHOTO_IMAGE, "Performance Test");

        // Verify image was processed efficiently
        int totalPixels = result.image().getWidth() * result.image().getHeight();

        // ImageInputHandler should limit size to prevent memory issues
        assertTrue(totalPixels <= 1920 * 1080,
                "Image should be resized to reasonable dimensions. Total pixels: " + totalPixels);

        // Processing time should be reasonable
        double pixelsPerMs = totalPixels / (double) result.processingTimeMs();
        Logger.info("Performance: " + String.format("%.1f", pixelsPerMs) + " pixels/ms");

        assertTrue(pixelsPerMs > 100,
                "Should process at least 100 pixels/ms, got: " + pixelsPerMs);

        result.printDetailedReport();
    }

    @Test
    @DisplayName("Pipeline Robustness - Test multiple images in sequence")
    void testPipelineRobustness() throws IOException {
        Logger.info("\n=== PIPELINE ROBUSTNESS TEST ===");

        String[] testImages = {SOLID_IMAGE, TRANSPARENT_IMAGE, GRADIENT_IMAGE, PHOTO_IMAGE};
        String[] testNames = {"Solid", "Transparent", "Gradient", "Photo"};

        for (int i = 0; i < testImages.length; i++) {
            try {
                PipelineResult result = processImage(testImages[i], testNames[i] + " (Batch)");

                // Basic sanity checks for each image
                assertNotNull(result.image(), "Image loading failed for: " + testNames[i]);
                assertFalse(result.extractedColors().isEmpty(), "No colors extracted from: " + testNames[i]);
                assertFalse(result.colorGroups().isEmpty(), "No color groups created for: " + testNames[i]);

                Logger.info("✅ " + testNames[i] + " processed successfully");

            } catch (Exception e) {
                fail("Pipeline failed on " + testNames[i] + ": " + e.getMessage());
            }
        }

        Logger.info("🎉 All images processed successfully in batch!");
    }
}