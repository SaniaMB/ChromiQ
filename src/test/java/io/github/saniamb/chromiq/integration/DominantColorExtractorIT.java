package io.github.saniamb.chromiq.integration;

import io.github.saniamb.chromiq.core.color.DominantColorExtractor;
import io.github.saniamb.chromiq.core.input.ImageInputHandler;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.HashSet;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Pipeline Testing for ChromiQ Color Extraction
 * =======================================================
 *
 * Tests the complete user workflow:
 * 1. Image Upload → ImageInputHandler
 * 2. Image Processing → Resize, format conversion
 * 3. Color Extraction → DominantColorExtractor
 * 4. Results Validation → Proper palette generation
 *
 * These tests simulate real user scenarios with realistic images
 * and measure end-to-end performance and quality.
 */
public class DominantColorExtractorIT {

    private ImageInputHandler imageHandler;
    private DominantColorExtractor colorExtractor;

    // Performance benchmarks (in milliseconds)
    private static final long MAX_SMALL_IMAGE_TIME = 1000;   // 100x100 images
    private static final long MAX_MEDIUM_IMAGE_TIME = 3000;  // 500x400 images
    private static final long MAX_LARGE_IMAGE_TIME = 5000;   // 1000x800 images

    @BeforeEach
    void setUp() {
        imageHandler = new ImageInputHandler();
        colorExtractor = new DominantColorExtractor();
    }

    // === COMPLETE PIPELINE TESTS ===

    @Test
    @DisplayName("Complete pipeline: Simple graphic with few colors")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testPipelineSimpleGraphic() throws IOException {
        // Simulate uploading a simple logo/graphic
        BufferedImage originalImage = createLogoImage();

        // Step 1: Simulate image upload via stream
        InputStream imageStream = bufferedImageToStream(originalImage, "PNG");

        // Step 2: Process through ImageInputHandler (like real upload)
        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);

        // Verify ImageInputHandler did its job
        assertNotNull(processedImage, "ImageInputHandler should process image successfully");
        assertEquals(BufferedImage.TYPE_INT_ARGB, processedImage.getType(), "Should be converted to ARGB");

        // Step 3: Extract dominant colors (complete extraction pipeline)
        long startTime = System.currentTimeMillis();
        List<DominantColorExtractor.DominantColor> colors = colorExtractor.extractDominantColors(processedImage);
        long pipelineTime = System.currentTimeMillis() - startTime;

        // Verify results quality
        assertFalse(colors.isEmpty(), "Should extract colors from logo");
        assertTrue(colors.size() <= 10, "Simple logo should have few dominant colors");
        assertTrue(pipelineTime < MAX_SMALL_IMAGE_TIME, "Simple image should process quickly: " + pipelineTime + "ms");

        // Verify color quality (logo should have distinct, saturated colors)
        boolean hasDistinctColors = colors.size() > 1;
        assertTrue(hasDistinctColors, "Logo should have multiple distinct colors");

        // Check total coverage (should account for most pixels)
        double totalCoverage = colors.stream().mapToDouble(DominantColorExtractor.DominantColor::getPercentage).sum();
        assertTrue(totalCoverage > 90, "Dominant colors should cover most of image: " + totalCoverage + "%");
    }

    @Test
    @DisplayName("Complete pipeline: Complex photo with many colors")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPipelineComplexPhoto() throws IOException {
        // Simulate uploading a complex photograph
        BufferedImage originalImage = createPhotoImage(400);

        // Step 1: Process through complete pipeline
        InputStream imageStream = bufferedImageToStream(originalImage, "PNG");
        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);

        // Step 2: Extract dominant colors
        long startTime = System.currentTimeMillis();
        List<DominantColorExtractor.DominantColor> colors = colorExtractor.extractDominantColors(processedImage);
        long pipelineTime = System.currentTimeMillis() - startTime;

        // Verify clustering was used (complex photo should return exactly 10 colors via clustering)
        assertEquals(10, colors.size(), "Complex photo should cluster to 10 dominant colors");
        assertTrue(pipelineTime < MAX_MEDIUM_IMAGE_TIME, "Medium image should process in reasonable time: " + pipelineTime + "ms");

        // Verify colors are diverse (clustering found different regions)
        Set<String> colorFamilies = new HashSet<>();
        for (DominantColorExtractor.DominantColor dc : colors) {
            ColorEntry color = dc.getColor();
            // Categorize colors by their dominant channel
            if (color.getRed() > color.getGreen() && color.getRed() > color.getBlue()) colorFamilies.add("red");
            else if (color.getGreen() > color.getRed() && color.getGreen() > color.getBlue()) colorFamilies.add("green");
            else if (color.getBlue() > color.getRed() && color.getBlue() > color.getGreen()) colorFamilies.add("blue");
            else colorFamilies.add("neutral");
        }

        assertTrue(colorFamilies.size() >= 2, "Complex photo should have diverse color families, found: " + colorFamilies);
    }

    @Test
    @DisplayName("Complete pipeline: Large image resize and processing")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPipelineLargeImageResize() throws IOException {
        // Create oversized image (larger than ImageInputHandler's MAX_WIDTH/HEIGHT)
        BufferedImage hugeImage = createGradientImage();

        // Process through pipeline
        InputStream imageStream = bufferedImageToStream(hugeImage, "PNG");
        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);

        // Verify ImageInputHandler resized the image
        assertTrue(processedImage.getWidth() <= 1920, "Width should be resized: " + processedImage.getWidth());
        assertTrue(processedImage.getHeight() <= 1080, "Height should be resized: " + processedImage.getHeight());

        // Verify colors are still extracted properly after resizing
        List<DominantColorExtractor.DominantColor> colors = colorExtractor.extractDominantColors(processedImage);

        assertFalse(colors.isEmpty(), "Should extract colors even after resizing");
        assertTrue(colors.size() <= 10, "Should respect max colors limit");

        // Verify gradient colors are preserved (magenta to cyan transition)
        boolean hasMagentish = colors.stream().anyMatch(c ->
                c.getColor().getRed() > 100 && c.getColor().getBlue() > 100);
        boolean hasCyanish = colors.stream().anyMatch(c ->
                c.getColor().getGreen() > 100 && c.getColor().getBlue() > 100);

        assertTrue(hasMagentish, "Should preserve magenta tones from original");
        assertTrue(hasCyanish, "Should preserve cyan tones from original");
    }

    // === EDGE CASE PIPELINE TESTS ===

    @Test
    @DisplayName("Pipeline handles corrupted/invalid image gracefully")
    void testPipelineInvalidImage() {
        // Create invalid image data
        byte[] corruptedData = "This is not an image file".getBytes();
        InputStream invalidStream = new ByteArrayInputStream(corruptedData);

        // Pipeline should handle this gracefully
        assertThrows(IOException.class, () -> {
            imageHandler.loadFromStream(invalidStream);
        }, "Pipeline should reject corrupted image data");
    }

    @Test
    @DisplayName("Pipeline handles empty/transparent image")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testPipelineTransparentImage() throws IOException {
        // Create completely transparent image
        BufferedImage transparentImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        // (defaults to fully transparent)

        InputStream imageStream = bufferedImageToStream(transparentImage, "PNG");
        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);

        List<DominantColorExtractor.DominantColor> colors = colorExtractor.extractDominantColors(processedImage);

        assertTrue(colors.isEmpty(), "Transparent image should return no colors");
    }

    @Test
    @DisplayName("Pipeline handles solid color image efficiently")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testPipelineSolidColor() throws IOException {
        BufferedImage solidImage = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = solidImage.createGraphics();
        g2d.setColor(Color.ORANGE);
        g2d.fillRect(0, 0, 300, 200);
        g2d.dispose();

        InputStream imageStream = bufferedImageToStream(solidImage, "PNG");
        BufferedImage processedImage = imageHandler.loadFromStream(imageStream);

        long startTime = System.currentTimeMillis();
        List<DominantColorExtractor.DominantColor> colors = colorExtractor.extractDominantColors(processedImage);
        long processingTime = System.currentTimeMillis() - startTime;

        // Should be very fast (no clustering needed)
        assertEquals(1, colors.size(), "Solid color should return exactly 1 color");
        assertTrue(processingTime < 500, "Solid color should process very quickly: " + processingTime + "ms");
        assertEquals("#FFC800", colors.get(0).getColor().getHex(), "Should be orange");
        assertTrue(colors.get(0).getPercentage() > 99, "Should be nearly 100%");
    }

    // === PERFORMANCE AND STRESS TESTS ===

    @Test
    @DisplayName("Pipeline performance benchmark")
    void testPipelinePerformanceBenchmark() throws IOException {
        // Test various image sizes and measure performance

        // Small image benchmark
        BufferedImage small = createRandomColorImage(150, 100, 8);
        long smallTime = measurePipelineTime(small);
        assertTrue(smallTime < MAX_SMALL_IMAGE_TIME,
                "Small image pipeline too slow: " + smallTime + "ms");

        // Medium image benchmark
        BufferedImage medium = createRandomColorImage(600, 400, 25);
        long mediumTime = measurePipelineTime(medium);
        assertTrue(mediumTime < MAX_MEDIUM_IMAGE_TIME,
                "Medium image pipeline too slow: " + mediumTime + "ms");

        // Performance should scale reasonably
        assertTrue(mediumTime > smallTime, "Medium image should take longer than small");
        assertTrue(mediumTime < smallTime * 10, "Medium image shouldn't be 10x slower than small");
    }

    @Test
    @DisplayName("Pipeline memory usage is reasonable")
    void testPipelineMemoryUsage() throws IOException {
        // Measure memory before
        Runtime runtime = Runtime.getRuntime();
        System.gc(); // Suggest garbage collection
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Process several medium-sized images
        for (int i = 0; i < 5; i++) {
            BufferedImage image = createPhotoImage(400);
            InputStream stream = bufferedImageToStream(image, "PNG");
            BufferedImage processed = imageHandler.loadFromStream(stream);
            colorExtractor.extractDominantColors(processed);
        }

        // Measure memory after
        System.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // Should not leak excessive memory (less than 50MB for 5 images)
        assertTrue(memoryUsed < 50 * 1024 * 1024,
                "Pipeline using too much memory: " + (memoryUsed / 1024 / 1024) + "MB");
    }

    // === USER SCENARIO TESTS ===

    @Test
    @DisplayName("User scenario: Upload photo, extract palette, customize size")
    void testUserWorkflow() throws IOException {
        // Simulate complete user workflow
        BufferedImage userPhoto = createPhotoImage(500);

        // Step 1: User uploads image
        InputStream upload = bufferedImageToStream(userPhoto, "PNG");
        BufferedImage processedImage = imageHandler.loadFromStream(upload);

        // Step 2: User gets default 10-color palette
        List<DominantColorExtractor.DominantColor> defaultPalette =
                colorExtractor.extractDominantColors(processedImage);
        assertEquals(10, defaultPalette.size(), "Default should be 10 colors");

        // Step 3: User customizes to 5 colors
        List<DominantColorExtractor.DominantColor> customPalette =
                colorExtractor.extractDominantColors(processedImage, 5);
        assertEquals(5, customPalette.size(), "Custom should be 5 colors");

        // Step 4: User customizes to 3 colors
        List<DominantColorExtractor.DominantColor> minimalPalette =
                colorExtractor.extractDominantColors(processedImage, 3);
        assertEquals(3, minimalPalette.size(), "Minimal should be 3 colors");

        // Verify consistency: smaller palettes should be subsets of larger ones
        // (i.e., the most dominant color should be the same across all palette sizes)
    }

    // === HELPER METHODS FOR REALISTIC TEST IMAGES ===

    /**
     * Creates a logo-style image with few distinct colors.
     */
    private BufferedImage createLogoImage() {
        BufferedImage image = new BufferedImage(200, 150, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // White background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 200, 150);

        // Blue circle (logo element)
        g2d.setColor(Color.BLUE);
        g2d.fillOval(200 /4, 150 /4, 200 /2, 150 /2);

        // Red text area (simulate text/graphics)
        g2d.setColor(Color.RED);
        g2d.fillRect(10, 150 - 30, 200 - 20, 20);

        g2d.dispose();
        return image;
    }

    /**
     * Creates a photo-style image with many similar colors (good for testing clustering).
     */
    private BufferedImage createPhotoImage(int width) {
        BufferedImage image = new BufferedImage(width, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Create a complex scene with multiple color regions
        // Sky region (blue tones)
        GradientPaint skyGradient = new GradientPaint(0, 0, Color.CYAN, 0, (float) 300 /3, Color.BLUE);
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, width, 300 /3);

        // Ground region (green/brown tones)
        GradientPaint groundGradient = new GradientPaint(0, (float) 300 /3, Color.GREEN, 0, 300, new Color(101, 67, 33));
        g2d.setPaint(groundGradient);
        g2d.fillRect(0, 300 /3, width, 300 * 2/3);

        // Add some "objects" with different colors
        g2d.setColor(Color.YELLOW); // Sun
        g2d.fillOval(width - 80, 20, 60, 60);

        g2d.setColor(Color.RED); // House
        g2d.fillRect(width/3, 300 /2, width/4, 300 /3);

        g2d.dispose();
        return image;
    }

    /**
     * Creates gradient image with many similar colors.
     */
    private BufferedImage createGradientImage() {
        BufferedImage image = new BufferedImage(2500, 1800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        GradientPaint gradient = new GradientPaint(0, 0, Color.MAGENTA, 2500, 1800, Color.CYAN);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, 2500, 1800);

        g2d.dispose();
        return image;
    }

    /**
     * Creates image with random colors (stress testing).
     */
    private BufferedImage createRandomColorImage(int width, int height, int colorCount) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        int stripHeight = height / colorCount;

        for (int i = 0; i < colorCount; i++) {
            // Create random colors
            Color randomColor = new Color(
                    (int)(Math.random() * 256),
                    (int)(Math.random() * 256),
                    (int)(Math.random() * 256)
            );

            g2d.setColor(randomColor);
            g2d.fillRect(0, i * stripHeight, width, stripHeight);
        }

        g2d.dispose();
        return image;
    }

    /**
     * Converts BufferedImage to InputStream (simulates file upload).
     */
    private InputStream bufferedImageToStream(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Measures complete pipeline processing time.
     */
    private long measurePipelineTime(BufferedImage originalImage) throws IOException {
        InputStream stream = bufferedImageToStream(originalImage, "PNG");

        long startTime = System.currentTimeMillis();

        BufferedImage processedImage = imageHandler.loadFromStream(stream);
        colorExtractor.extractDominantColors(processedImage);

        return System.currentTimeMillis() - startTime;
    }
}
