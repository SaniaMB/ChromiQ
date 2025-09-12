package io.github.saniamb.chromiq.unit;
import io.github.saniamb.chromiq.core.color.DominantColorExtractor;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.palette.PaletteManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class PaletteManagerTest {

    public static void main(String[] args) throws Exception {
        // === STEP 1: Load your image ===
        // Replace this with your actual image path
        String imagePath = "src/test/resources/test-images/gradient.jpeg";
        BufferedImage image = ImageIO.read(new File(imagePath));

        // === STEP 2: Extract dominant colors from the image ===
        DominantColorExtractor extractor = new DominantColorExtractor();
        List<DominantColorExtractor.DominantColor> dominantColors = extractor.extractDominantColors(image, 5);

        System.out.println("Extracted dominant colors:");
        for (DominantColorExtractor.DominantColor dc : dominantColors) {
            System.out.printf("- %s (%.2f%%, %d px)%n",
                    dc.getColor().getHex(), dc.getPercentage(), dc.getPixelCount());
        }

        // === STEP 3: Initialize PaletteManager ===
        PaletteManager manager = new PaletteManager(dominantColors);
        System.out.println("\nInitial Palette Summary:");
        System.out.println(manager.getPaletteSummary());
        manager.getPaletteColors().forEach(System.out::println);

        // === STEP 4: Test addPickedColor ===
        ColorEntry newColor = new ColorEntry(255, 0, 0); // bright red
        PaletteManager.AddColorResult addResult = manager.addPickedColor(newColor, true);
        System.out.println("\nAdd Color Result: " + addResult.getMessage());

        // === STEP 5: Test removeColor ===
        PaletteManager.RemoveColorResult removeResult = manager.removeColor(0);
        System.out.println("\nRemove Color Result: " + removeResult.getMessage());

        // === STEP 6: Test replaceColor ===
        ColorEntry newBlue = new ColorEntry(0, 0, 255); // bright blue
        PaletteManager.ReplaceColorResult replaceResult = manager.replaceColor(1, newBlue, false);
        System.out.println("\nReplace Color Result: " + replaceResult.getMessage());

        // === STEP 7: Final Palette ===
        System.out.println("\nFinal Palette Summary:");
        System.out.println(manager.getPaletteSummary());
        manager.getPaletteColors().forEach(System.out::println);
    }
}