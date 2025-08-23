package io.github.saniamb.chromiq.resources;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TestImageGenerator {

    public static void main(String[] args) {
        try {
            // Get the current working directory and print it for debugging
            String currentDir = System.getProperty("user.dir");
            System.out.println("🔍 Current working directory: " + currentDir);

            // Create the test resources directory if it doesn't exist
            File testResourcesDir = new File(currentDir, "src/test/resources/images");
            System.out.println("📁 Trying to create directory: " + testResourcesDir.getAbsolutePath());

            boolean created = testResourcesDir.mkdirs();
            if (created) {
                System.out.println("✅ Directory created successfully!");
            } else if (testResourcesDir.exists()) {
                System.out.println("✅ Directory already exists!");
            } else {
                System.out.println("❌ Failed to create directory!");
                return;
            }

            // Generate different test images
            createSmallTestImage(testResourcesDir);
            createLargeTestImage(testResourcesDir);
            createColoredTestImage(testResourcesDir);
            createTransparentTestImage(testResourcesDir);
            createJpgTestImage(testResourcesDir);

            System.out.println("✅ All test images created successfully!");
            System.out.println("📁 Check: src/test/resources/images/");
            System.out.println("🗑️  You can delete this TestImageGenerator.java file now");

        } catch (IOException e) {
            System.err.println("❌ Failed to create test images: " + e.getMessage());
        }
    }

    /**
     * Creates a simple 100x100 white image for basic testing
     */
    private static void createSmallTestImage(File outputDir) throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Fill with white background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 100, 100);

        // Add a simple colored border so we can see it's working
        g2d.setColor(Color.BLUE);
        g2d.drawRect(0, 0, 99, 99);

        g2d.dispose();

        File output = new File(outputDir, "test-small.png");
        ImageIO.write(image, "png", output);
        System.out.println("📄 Created: test-small.png (100x100)");
    }

    /**
     * Creates a large 4000x3000 image for testing resizing
     */
    private static void createLargeTestImage(File outputDir) throws IOException {
        BufferedImage image = new BufferedImage(4000, 3000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Fill with light gray
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, 4000, 3000);

        // Add some colored rectangles to make it interesting
        g2d.setColor(Color.RED);
        g2d.fillRect(500, 500, 1000, 800);

        g2d.setColor(Color.GREEN);
        g2d.fillRect(2000, 1000, 800, 600);

        g2d.setColor(Color.BLUE);
        g2d.fillRect(1000, 2000, 600, 400);

        g2d.dispose();

        File output = new File(outputDir, "test-large.png");
        ImageIO.write(image, "png", output);
        System.out.println("📄 Created: test-large.png (4000x3000)");
    }

    /**
     * Creates a colorful image for color extraction testing
     */
    private static void createColoredTestImage(File outputDir) throws IOException {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Create sections with different colors
        g2d.setColor(new Color(255, 0, 0));   // Pure red
        g2d.fillRect(0, 0, 100, 100);

        g2d.setColor(new Color(0, 255, 0));   // Pure green
        g2d.fillRect(100, 0, 100, 100);

        g2d.setColor(new Color(0, 0, 255));   // Pure blue
        g2d.fillRect(0, 100, 100, 100);

        g2d.setColor(new Color(255, 255, 0)); // Yellow
        g2d.fillRect(100, 100, 100, 100);

        g2d.dispose();

        File output = new File(outputDir, "test-colored.png");
        ImageIO.write(image, "png", output);
        System.out.println("📄 Created: test-colored.jpg (200x200, 4 colors)");
    }

    /**
     * Creates an image with transparency for alpha channel testing
     */
    private static void createTransparentTestImage(File outputDir) throws IOException {
        BufferedImage image = new BufferedImage(150, 150, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Leave background transparent

        // Add a semi-transparent circle
        g2d.setColor(new Color(255, 0, 0, 128)); // Red with 50% transparency
        g2d.fillOval(25, 25, 100, 100);

        // Add a solid border
        g2d.setColor(new Color(0, 0, 0, 255)); // Solid black
        g2d.drawOval(25, 25, 100, 100);

        g2d.dispose();

        File output = new File(outputDir, "test-transparent.png");
        ImageIO.write(image, "png", output);
        System.out.println("📄 Created: test-transparent.png (150x150, with alpha)");
    }

    private static void createJpgTestImage(File outputDir) throws IOException {
        BufferedImage image = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Fill background (JPEG cannot store alpha)
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 150, 150);

        // Draw colored shapes
        g2d.setColor(Color.RED);
        g2d.fillOval(25, 25, 100, 100);

        g2d.setColor(Color.BLACK);
        g2d.drawOval(25, 25, 100, 100);

        g2d.dispose();

        File output = new File(outputDir, "test-image.jpg");
        ImageIO.write(image, "jpg", output);
        System.out.println("📄 Created: test-image.jpg (150x150, RGB, no alpha)");
    }
}
