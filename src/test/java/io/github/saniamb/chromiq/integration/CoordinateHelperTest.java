package io.github.saniamb.chromiq.integration;

import io.github.saniamb.chromiq.core.color.RegionalColorExtractor;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.input.ImageInputHandler;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Scanner;

/**
 * Easy Regional Color Extractor Test with Suggested Coordinates
 * ============================================================
 *
 * This version automatically suggests interesting coordinates to test,
 * so you don't need to figure out coordinates yourself!
 *
 * Features:
 * 1. Automatically suggests test points (corners, center, quarters)
 * 2. Tests a grid of points across the image
 * 3. Shows you a "color map" of different regions
 * 4. Still lets you enter custom coordinates if you want
 */
public class CoordinateHelperTest {

    private static final RegionalColorExtractor regionalExtractor = new RegionalColorExtractor();
    private static final ImageInputHandler imageHandler = new ImageInputHandler();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("🎨 ChromiQ Regional Color Extractor - Easy Coordinate Test");
        System.out.println("=========================================================");
        System.out.println();

        while (true) {
            try {
                // Step 1: Get image path from user
                String imagePath = getImagePath();
                if (imagePath.equals("quit")) {
                    System.out.println("👋 Thanks for testing! Goodbye.");
                    break;
                }

                // Step 2: Load image
                BufferedImage image = loadImageSafely(imagePath);
                if (image == null) continue;

                displayImageInfo(image, imagePath);

                // Step 3: Choose testing mode
                chooseTestingMode(image);

            } catch (Exception e) {
                System.err.println("❌ Unexpected error: " + e.getMessage());
                System.out.println("Let's try again...\n");
            }
        }

        scanner.close();
    }

    private static String getImagePath() {
        System.out.println("📁 Enter the path to your image file:");
        System.out.println("   (or 'quit' to exit)");
        System.out.print("Path: ");
        return scanner.nextLine().trim();
    }

    private static BufferedImage loadImageSafely(String imagePath) {
        try {
            System.out.println("📤 Loading image...");
            BufferedImage image = imageHandler.loadFromFile(imagePath);
            System.out.println("✅ Image loaded successfully!");
            return image;
        } catch (IOException e) {
            System.err.println("❌ Failed to load image: " + e.getMessage());
            return null;
        }
    }

    private static void displayImageInfo(BufferedImage image, String path) {
        System.out.println();
        System.out.println("🖼️  IMAGE INFO: " + image.getWidth() + "x" + image.getHeight() + " pixels");
        System.out.println();
    }

    /**
     * Lets user choose between automatic suggested coordinates or manual entry
     */
    private static void chooseTestingMode(BufferedImage image) {
        while (true) {
            System.out.println("🎯 TESTING OPTIONS:");
            System.out.println("1. Auto-test key points (recommended - no coordinate needed!)");
            System.out.println("2. Test a color grid (shows colors across whole image)");
            System.out.println("3. Enter my own coordinates");
            System.out.println("4. Done with this image");
            System.out.print("Choose (1-4): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    autoTestKeyPoints(image,true);
                    break;
                case "2":
                    testColorGrid(image);
                    break;
                case "3":
                    manualCoordinateTest(image);
                    break;
                case "4":
                    return; // Exit this image
                default:
                    System.out.println("Please enter 1, 2, 3, or 4");
            }
            System.out.println();
        }
    }

    /**
     * Tests important points without user needing to know coordinates
     */
    private static void autoTestKeyPoints(BufferedImage image, boolean useExactPixels) {
        String method = useExactPixels ? "EXACT PIXELS" : "REGION AVERAGING";
        System.out.println("\n🎯 AUTO-TESTING KEY POINTS (" + method + "):");

        if (useExactPixels) {
            System.out.println("Using exact pixel colors (like Adobe Color, Coolors.co)...\n");
        } else {
            System.out.println("Using region averaging (20x20 pixels around each point)...\n");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Define test points with descriptions
        TestPoint[] testPoints = {
                new TestPoint("Top-left corner", 10, 10),
                new TestPoint("Top-right corner", width - 10, 10),
                new TestPoint("Bottom-left corner", 10, height - 10),
                new TestPoint("Bottom-right corner", width - 10, height - 10),
                new TestPoint("Center", width / 2, height / 2),
                new TestPoint("Left center", width / 4, height / 2),
                new TestPoint("Right center", 3 * width / 4, height / 2),
                new TestPoint("Top center", width / 2, height / 4),
                new TestPoint("Bottom center", width / 2, 3 * height / 4)
        };

        for (TestPoint point : testPoints) {
            if (useExactPixels) {
                extractAndDisplayExactPixel(image, point.x, point.y, point.description);
            } else {
                extractAndDisplayColorCompact(image, point.x, point.y, point.description);
            }
        }

        System.out.println("✅ Key points testing complete!");
    }

    /**
     * Compares both extraction methods side by side
     */
    private static void compareExtractionMethods(BufferedImage image) {
        System.out.println("\n🔄 COMPARING EXTRACTION METHODS:");
        System.out.println("Testing the same coordinates with both approaches...\n");

        int width = image.getWidth();
        int height = image.getHeight();

        // Test a few key points with both methods
        TestPoint[] testPoints = {
                new TestPoint("Center", width / 2, height / 2),
                new TestPoint("Top-left", 50, 50),
                new TestPoint("Bottom-right", width - 50, height - 50)
        };

        for (TestPoint point : testPoints) {
            System.out.println("📍 " + point.description + " (" + point.x + ", " + point.y + "):");

            // Extract with both methods
            try {
                ColorEntry exactColor = regionalExtractor.extractExactPixel(image, point.x, point.y);
                ColorEntry regionColor = regionalExtractor.extractFromRegion(image, point.x, point.y);

                System.out.println("   Exact pixel:    " + exactColor.getHex() + " RGB(" +
                        exactColor.getRed() + "," + exactColor.getGreen() + "," + exactColor.getBlue() + ")");
                System.out.println("   Region average: " + regionColor.getHex() + " RGB(" +
                        regionColor.getRed() + "," + regionColor.getGreen() + "," + regionColor.getBlue() + ")");

                // Calculate how different they are
                int redDiff = Math.abs(exactColor.getRed() - regionColor.getRed());
                int greenDiff = Math.abs(exactColor.getGreen() - regionColor.getGreen());
                int blueDiff = Math.abs(exactColor.getBlue() - regionColor.getBlue());
                int totalDiff = redDiff + greenDiff + blueDiff;

                if (totalDiff < 30) {
                    System.out.println("   Difference: ✅ Very similar colors");
                } else if (totalDiff < 100) {
                    System.out.println("   Difference: ⚠️ Somewhat different");
                } else {
                    System.out.println("   Difference: ❌ Quite different (complex area)");
                }

                System.out.println();

            } catch (Exception e) {
                System.out.println("   Error: " + e.getMessage());
            }
        }

        System.out.println("✅ Comparison complete!");
    }

    /**
     * Exact pixel extraction display
     */
    private static void extractAndDisplayExactPixel(BufferedImage image, int x, int y, String description) {
        try {
            ColorEntry color = regionalExtractor.extractExactPixel(image, x, y);
            System.out.println(String.format("%-20s (%4d,%4d): %s RGB(%3d,%3d,%3d) [EXACT]",
                    description, x, y, color.getHex(),
                    color.getRed(), color.getGreen(), color.getBlue()));
        } catch (Exception e) {
            System.out.println(description + " - Error: " + e.getMessage());
        }
    }

    /**
     * Tests a grid of colors from across the image
     */
    private static void testColorGrid(BufferedImage image) {
        System.out.println("\n🌈 COLOR GRID TEST:");
        System.out.println("Sampling colors from a 3x3 grid across your image...\n");

        int width = image.getWidth();
        int height = image.getHeight();

        // Create 3x3 grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = (col + 1) * width / 4;  // Divide into quarters, sample centers
                int y = (row + 1) * height / 4;

                String position = getGridPosition(row, col);
                extractAndDisplayColorCompact(image, x, y, position + " of image");
            }
        }

        System.out.println("✅ Grid testing complete!");
    }

    /**
     * Manual coordinate entry for advanced users
     */
    private static void manualCoordinateTest(BufferedImage image) {
        System.out.println("\n📍 MANUAL COORDINATE ENTRY:");
        System.out.println("Image size: " + image.getWidth() + " x " + image.getHeight() + " pixels");
        System.out.println("Valid coordinates: x (0-" + (image.getWidth()-1) + "), y (0-" + (image.getHeight()-1) + ")");
        System.out.println();

        while (true) {
            System.out.print("Enter coordinates (x y) or 'back' to return to menu: ");
            String input = scanner.nextLine().trim();

            if (input.equals("back")) break;

            try {
                String[] parts = input.split("\\s+");
                if (parts.length != 2) {
                    System.out.println("Please enter exactly 2 numbers: x y");
                    continue;
                }

                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);

                if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
                    System.out.println("Coordinates out of bounds!");
                    continue;
                }

                extractAndDisplayColorDetailed(image, x, y);

            } catch (NumberFormatException e) {
                System.out.println("Please enter valid numbers");
            }
        }
    }

    /**
     * Compact color display for grid/auto tests
     */
    private static void extractAndDisplayColorCompact(BufferedImage image, int x, int y, String description) {
        try {
            ColorEntry color = regionalExtractor.extractFromRegion(image, x, y);
            System.out.println(String.format("%-20s (%4d,%4d): %s RGB(%3d,%3d,%3d)",
                    description, x, y, color.getHex(),
                    color.getRed(), color.getGreen(), color.getBlue()));
        } catch (Exception e) {
            System.out.println(description + " - Error: " + e.getMessage());
        }
    }

    /**
     * Detailed color display for manual tests
     */
    private static void extractAndDisplayColorDetailed(BufferedImage image, int x, int y) {
        try {
            ColorEntry color = regionalExtractor.extractFromRegion(image, x, y);
            double[] hsl = color.getHSL();

            System.out.println(String.format("Coordinates (%d, %d):", x, y));
            System.out.println(String.format("  HEX: %s", color.getHex()));
            System.out.println(String.format("  RGB: (%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue()));
            System.out.println(String.format("  HSL: (%.0f°, %.0f%%, %.0f%%)", hsl[0], hsl[1], hsl[2]));
            System.out.println();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Helper classes and methods
     */
    private static class TestPoint {
        final String description;
        final int x, y;

        TestPoint(String description, int x, int y) {
            this.description = description;
            this.x = x;
            this.y = y;
        }
    }

    private static String getGridPosition(int row, int col) {
        String[] rowNames = {"Top", "Middle", "Bottom"};
        String[] colNames = {"left", "center", "right"};
        return rowNames[row] + " " + colNames[col];
    }
}