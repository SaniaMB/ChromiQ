package io.github.saniamb.chromiq.integration;

import io.github.saniamb.chromiq.core.color.DominantColorExtractor;
import io.github.saniamb.chromiq.core.color.RegionalColorExtractor;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.input.ImageInputHandler;
import io.github.saniamb.chromiq.core.palette.PaletteManager;
import io.github.saniamb.chromiq.core.utils.Logger;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * PaletteIntegrationTest - Complete Workflow Integration Test
 * =========================================================
 *
 * Tests the complete ChromiQ workflow with a real image:
 * 1. Load and process image
 * 2. Extract dominant colors
 * 3. Create initial palette
 * 4. Simulate user clicking on regions to add colors
 * 5. Test palette management (add, remove, replace)
 *
 * This simulates the real user experience end-to-end.
 */
public class PaletteManagerIntegrationTest {

    public static void main(String[] args) {
        System.out.println("=== ChromiQ Palette Integration Test ===\n");

        try {
            // Step 1: Load a real image
            runCompleteWorkflowTest();

            System.out.println("\n✅ Integration test completed successfully!");

        } catch (Exception e) {
            System.err.println("❌ Integration test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs the complete ChromiQ workflow with a real image
     */
    private static void runCompleteWorkflowTest() throws IOException {
        System.out.println("🔄 Starting complete workflow test...\n");

        // === STEP 1: Load and Process Image ===
        System.out.println("📁 Step 1: Loading image...");

        // TODO: Replace this with YOUR actual image path
        String imagePath = "src/test/resources/test-images/gradient.jpeg"; // <-- CHANGE THIS TO YOUR IMAGE PATH

        ImageInputHandler imageHandler = new ImageInputHandler();
        BufferedImage image = imageHandler.loadFromFile(imagePath);

        System.out.println("✅ Image loaded: " + imageHandler.getImageInfo(image));
        System.out.println();

        // === STEP 2: Extract Dominant Colors ===
        System.out.println("🎨 Step 2: Extracting dominant colors...");

        DominantColorExtractor dominantExtractor = new DominantColorExtractor();
        List<DominantColorExtractor.DominantColor> dominantColors =
                DominantColorExtractor.extractDominantColors(image); // Start with 5 colors

        System.out.println("✅ Found " + dominantColors.size() + " dominant colors:");
        for (int i = 0; i < dominantColors.size(); i++) {
            DominantColorExtractor.DominantColor dc = dominantColors.get(i);
            System.out.printf("   %d. %s - %.2f%% (%,d pixels)\n",
                    i + 1, dc.getColor().getHex(), dc.getPercentage(), dc.getPixelCount());
        }
        System.out.println();

        // === STEP 3: Create Initial Palette ===
        System.out.println("📋 Step 3: Creating initial palette...");

        PaletteManager paletteManager = new PaletteManager(dominantColors);
        paletteManager.setImageName(imagePath);

        System.out.println("✅ " + paletteManager.getPaletteSummary());
        displayCurrentPalette(paletteManager);
        System.out.println();

        // === STEP 4: Simulate User Interactions ===
        System.out.println("👆 Step 4: Simulating user color picking...");

        RegionalColorExtractor regionalExtractor = new RegionalColorExtractor();

        // Simulate user clicking on different parts of the image
        testUserInteractions(image, paletteManager, regionalExtractor);

        // === STEP 5: Test Palette Management ===
        System.out.println("🔧 Step 5: Testing palette management...");

        testPaletteManagement(paletteManager);

        // === STEP 6: Final Results ===
        System.out.println("📊 Step 6: Final palette results:");
        displayFinalResults(paletteManager);
    }

    /**
     * Simulates various user interactions with the image
     */
    private static void testUserInteractions(BufferedImage image,
                                             PaletteManager paletteManager,
                                             RegionalColorExtractor regionalExtractor) {

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        // Test clicking in different regions of the image
        testRegionClick("Top-left corner", paletteManager, regionalExtractor, image,
                imageWidth / 4, imageHeight / 4, true);

        testRegionClick("Center", paletteManager, regionalExtractor, image,
                imageWidth / 2, imageHeight / 2, false);

        testRegionClick("Bottom-right", paletteManager, regionalExtractor, image,
                (imageWidth * 3) / 4, (imageHeight * 3) / 4, true);

        // Test exact pixel picking
        testExactPixelClick("Exact center pixel", paletteManager, regionalExtractor, image,
                imageWidth / 2, imageHeight / 2);
    }

    /**
     * Tests clicking on a region and adding the color to palette
     */
    private static void testRegionClick(String description,
                                        PaletteManager paletteManager,
                                        RegionalColorExtractor regionalExtractor,
                                        BufferedImage image,
                                        int x, int y, boolean isExactPick) {

        System.out.printf("   👆 Simulating click on %s at (%d, %d)...\n", description, x, y);

        try {
            ColorEntry pickedColor;
            if (isExactPick) {
                pickedColor = regionalExtractor.extractExactPixel(image, x, y);
                System.out.printf("      🎯 Exact pixel color: %s\n", pickedColor.getHex());
            } else {
                pickedColor = regionalExtractor.extractFromRegion(image, x, y);
                System.out.printf("      🏞️  Region average color: %s\n", pickedColor.getHex());
            }

            // Try to add to palette
            PaletteManager.AddColorResult result = paletteManager.addPickedColor(pickedColor, isExactPick);

            if (result.isSuccess()) {
                System.out.printf("      ✅ %s\n", result.getMessage());
            } else {
                System.out.printf("      ⚠️  %s\n", result.getMessage());
            }

        } catch (Exception e) {
            System.out.printf("      ❌ Error: %s\n", e.getMessage());
        }

        System.out.println();
    }

    /**
     * Tests exact pixel picking
     */
    private static void testExactPixelClick(String description,
                                            PaletteManager paletteManager,
                                            RegionalColorExtractor regionalExtractor,
                                            BufferedImage image,
                                            int x, int y) {

        System.out.printf("   🎯 %s at (%d, %d)...\n", description, x, y);

        try {
            ColorEntry exactColor = regionalExtractor.extractExactPixel(image, x, y);
            System.out.printf("      Exact color: %s (RGBA: %d,%d,%d,%.2f)\n",
                    exactColor.getHex(), exactColor.getRed(), exactColor.getGreen(),
                    exactColor.getBlue(), exactColor.getAlpha());

            // Try to add as exact pick
            PaletteManager.AddColorResult result = paletteManager.addPickedColor(exactColor, true);
            System.out.printf("      %s %s\n",
                    result.isSuccess() ? "✅" : "⚠️", result.getMessage());

        } catch (Exception e) {
            System.out.printf("      ❌ Error: %s\n", e.getMessage());
        }

        System.out.println();
    }

    /**
     * Tests palette management operations (remove, replace)
     */
    private static void testPaletteManagement(PaletteManager paletteManager) {

        System.out.println("   🗑️  Testing color removal...");
        if (paletteManager.getPaletteSize() > 2) {
            // Remove the color at index 1 (second color)
            PaletteManager.RemoveColorResult removeResult = paletteManager.removeColor(1);
            System.out.printf("      %s %s\n",
                    removeResult.isSuccess() ? "✅" : "❌", removeResult.getMessage());
        } else {
            System.out.println("      ⚠️  Not enough colors to test removal");
        }

        System.out.println("\n   🔄 Testing color replacement...");
        if (paletteManager.getPaletteSize() > 0) {
            // Create a test color for replacement
            ColorEntry testColor = new ColorEntry(255, 0, 128, "Test Pink"); // Bright pink

            PaletteManager.ReplaceColorResult replaceResult =
                    paletteManager.replaceColor(0, testColor, true);
            System.out.printf("      %s %s\n",
                    replaceResult.isSuccess() ? "✅" : "❌", replaceResult.getMessage());
        }

        System.out.println();
    }

    /**
     * Displays the current palette in a readable format
     */
    private static void displayCurrentPalette(PaletteManager paletteManager) {
        List<PaletteManager.PaletteColor> colors = paletteManager.getPaletteColors();

        System.out.println("Current Palette:");
        for (int i = 0; i < colors.size(); i++) {
            PaletteManager.PaletteColor pc = colors.get(i);
            System.out.printf("   %d. %s - %s\n",
                    i + 1, pc.getColor().getHex(), pc.getSourceDescription());
        }
    }

    /**
     * Shows final results and statistics
     */
    private static void displayFinalResults(PaletteManager paletteManager) {
        System.out.println("✅ " + paletteManager.getPaletteSummary());
        System.out.println();

        displayCurrentPalette(paletteManager);

        System.out.println("\nPalette Export (HEX values):");
        List<ColorEntry> finalColors = paletteManager.getColorEntries();
        for (int i = 0; i < finalColors.size(); i++) {
            System.out.printf("Color %d: %s\n", i + 1, finalColors.get(i).getHex());
        }

        System.out.println("\nPalette Export (RGB values):");
        for (int i = 0; i < finalColors.size(); i++) {
            ColorEntry color = finalColors.get(i);
            System.out.printf("Color %d: rgb(%d, %d, %d)\n",
                    i + 1, color.getRed(), color.getGreen(), color.getBlue());
        }
    }
}