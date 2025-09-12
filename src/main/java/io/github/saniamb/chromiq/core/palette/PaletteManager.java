package io.github.saniamb.chromiq.core.palette;

import io.github.saniamb.chromiq.core.color.DominantColorExtractor;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.utils.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PaletteManager - Interactive Palette Customization
 * =================================================
 *
 * Manages a user's color palette, allowing them to:
 * 1. Start with a dominant color palette from an image
 * 2. Remove colors they don't want
 * 3. Add new colors picked from specific image regions
 * 4. Maintain palette organization and avoid duplicates
 *
 * Key Features:
 * - Supports both exact pixel and region-based color picking
 * - Prevents duplicate colors (with similarity threshold)
 * - Maintains color importance/popularity order
 * - Provides palette statistics and information
 *
 * Usage:
 * PaletteManager manager = new PaletteManager(dominantColors);
 * manager.removeColor(2); // Remove color at index 2
 * manager.addPickedColor(newColor, true); // Add new color (exact mode)
 */
public class PaletteManager {

    /**
     * Represents a color in the managed palette with its source and metadata
     */
    public static class PaletteColor {
        private ColorEntry color;
        private ColorSource source;
        private double originalPercentage; // From dominant extraction
        private int originalPixelCount;
        private boolean isUserPicked;      // True if user manually added this

        public enum ColorSource {
            DOMINANT_EXTRACTION,    // From original DominantColorExtractor
            USER_EXACT_PICK,       // User picked with exact pixel mode
            USER_REGION_PICK       // User picked with region averaging mode
        }

        // Constructor for dominant colors (from DominantColorExtractor)
        public PaletteColor(ColorEntry color, double percentage, int pixelCount) {
            this.color = color;
            this.source = ColorSource.DOMINANT_EXTRACTION;
            this.originalPercentage = percentage;
            this.originalPixelCount = pixelCount;
            this.isUserPicked = false;
        }

        // Constructor for user-picked colors
        public PaletteColor(ColorEntry color, boolean isExactPick) {
            this.color = color;
            this.source = isExactPick ? ColorSource.USER_EXACT_PICK : ColorSource.USER_REGION_PICK;
            this.originalPercentage = 0.0; // User picks don't have image percentages
            this.originalPixelCount = 0;
            this.isUserPicked = true;
        }

        // Getters
        public ColorEntry getColor() { return color; }
        public ColorSource getSource() { return source; }
        public double getOriginalPercentage() { return originalPercentage; }
        public int getOriginalPixelCount() { return originalPixelCount; }
        public boolean isUserPicked() { return isUserPicked; }

        public String getSourceDescription() {
            return switch (source) {
                case DOMINANT_EXTRACTION -> String.format("Dominant (%.1f%%)", originalPercentage);
                case USER_EXACT_PICK -> "User Pick (Exact)";
                case USER_REGION_PICK -> "User Pick (Region)";
            };
        }

        @Override
        public String toString() {
            return String.format("PaletteColor{%s - %s}", color.getHex(), getSourceDescription());
        }
    }

    // Configuration
    private static final int MAX_PALETTE_SIZE = 10;
    private static final double COLOR_SIMILARITY_THRESHOLD = 10.0; // Delta E threshold for "similar" colors

    // Palette state
    private List<PaletteColor> paletteColors;
    private String imageName; // Optional name/path of source image

    /**
     * Creates a PaletteManager with dominant colors from DominantColorExtractor
     */
    public PaletteManager(List<DominantColorExtractor.DominantColor> dominantColors) {
        this.paletteColors = new ArrayList<>();
        this.imageName = "Unnamed Image";

        // Convert DominantColors to PaletteColors
        for (DominantColorExtractor.DominantColor dc : dominantColors) {
            PaletteColor paletteColor = new PaletteColor(
                    dc.getColor(),
                    dc.getPercentage(),
                    dc.getPixelCount()
            );
            paletteColors.add(paletteColor);
        }

        Logger.info(String.format("PaletteManager initialized with %d dominant colors", paletteColors.size()));
    }

    /**
     * Creates an empty PaletteManager (for manual palette building)
     */
    public PaletteManager() {
        this.paletteColors = new ArrayList<>();
        this.imageName = "Custom Palette";
        Logger.info("PaletteManager initialized as empty custom palette");
    }

    // --- Core Palette Operations ---

    /**
     * Adds a user-picked color to the palette.
     *
     * @param pickedColor The color picked by the user
     * @param isExactPick True if picked with exact pixel mode, false for region averaging
     * @return Result object indicating success/failure and any messages
     */
    public AddColorResult addPickedColor(ColorEntry pickedColor, boolean isExactPick) {
        if (pickedColor == null) {
            return new AddColorResult(false, "Cannot add null color to palette");
        }

        // Check if palette is full
        if (paletteColors.size() >= MAX_PALETTE_SIZE) {
            return new AddColorResult(false,
                    String.format("Palette is full (%d colors). Remove a color first.", MAX_PALETTE_SIZE));
        }

        // Check for similar colors already in palette
        PaletteColor similarColor = findSimilarColor(pickedColor);
        if (similarColor != null) {
            return new AddColorResult(false,
                    String.format("Similar color already exists: %s (%s)",
                            similarColor.getColor().getHex(), similarColor.getSourceDescription()));
        }

        // Add the new color
        PaletteColor newPaletteColor = new PaletteColor(pickedColor, isExactPick);
        paletteColors.add(newPaletteColor);

        String pickMode = isExactPick ? "exact pixel" : "region average";
        Logger.info(String.format("Added user-picked color %s (%s) to palette",
                pickedColor.getHex(), pickMode));

        return new AddColorResult(true,
                String.format("Added %s to palette (%s mode)", pickedColor.getHex(), pickMode));
    }

    /**
     * Removes a color from the palette by index.
     *
     * @param index Index of color to remove (0-based)
     * @return Result object indicating success/failure and any messages
     */
    public RemoveColorResult removeColor(int index) {
        if (index < 0 || index >= paletteColors.size()) {
            return new RemoveColorResult(false,
                    String.format("Invalid index %d. Valid range: 0-%d", index, paletteColors.size() - 1));
        }

        PaletteColor removedColor = paletteColors.remove(index);

        Logger.info(String.format("Removed color %s from palette (was at index %d)",
                removedColor.getColor().getHex(), index));

        return new RemoveColorResult(true, removedColor,
                String.format("Removed %s from palette", removedColor.getColor().getHex()));
    }

    /**
     * Replaces a color at specific index with a new user-picked color.
     * This is a convenience method that combines remove + add.
     */
    public ReplaceColorResult replaceColor(int index, ColorEntry newColor, boolean isExactPick) {
        if (index < 0 || index >= paletteColors.size()) {
            return new ReplaceColorResult(false, null, null,
                    String.format("Invalid index %d. Valid range: 0-%d", index, paletteColors.size() - 1));
        }

        // Check for similar colors (excluding the one we're replacing)
        PaletteColor similarColor = findSimilarColorExcluding(newColor, index);
        if (similarColor != null) {
            return new ReplaceColorResult(false, null, null,
                    String.format("Similar color already exists: %s", similarColor.getColor().getHex()));
        }

        // Perform the replacement
        PaletteColor oldColor = paletteColors.get(index);
        PaletteColor newPaletteColor = new PaletteColor(newColor, isExactPick);
        paletteColors.set(index, newPaletteColor);

        String pickMode = isExactPick ? "exact pixel" : "region average";
        Logger.info(String.format("Replaced color %s with %s at index %d (%s mode)",
                oldColor.getColor().getHex(), newColor.getHex(), index, pickMode));

        return new ReplaceColorResult(true, oldColor, newPaletteColor,
                String.format("Replaced %s with %s", oldColor.getColor().getHex(), newColor.getHex()));
    }

    // --- Helper Methods ---

    /**
     * Finds a color in the palette that's similar to the given color.
     * Uses Delta E color difference calculation.
     */
    private PaletteColor findSimilarColor(ColorEntry candidateColor) {
        for (PaletteColor paletteColor : paletteColors) {
            if (areColorsSimilar(candidateColor, paletteColor.getColor())) {
                return paletteColor;
            }
        }
        return null;
    }

    /**
     * Finds similar color but excludes a specific index (used for replacements)
     */
    private PaletteColor findSimilarColorExcluding(ColorEntry candidateColor, int excludeIndex) {
        for (int i = 0; i < paletteColors.size(); i++) {
            if (i == excludeIndex) continue; // Skip the index we're replacing

            PaletteColor paletteColor = paletteColors.get(i);
            if (areColorsSimilar(candidateColor, paletteColor.getColor())) {
                return paletteColor;
            }
        }
        return null;
    }

    /**
     * Checks if two colors are similar using Delta E color difference.
     */
    private boolean areColorsSimilar(ColorEntry color1, ColorEntry color2) {
        double[] lab1 = color1.getLAB();
        double[] lab2 = color2.getLAB();

        double deltaL = lab1[0] - lab2[0];
        double deltaA = lab1[1] - lab2[1];
        double deltaB = lab1[2] - lab2[2];

        double deltaE = Math.sqrt(deltaL * deltaL + deltaA * deltaA + deltaB * deltaB);

        return deltaE <= COLOR_SIMILARITY_THRESHOLD;
    }

    // --- Getters and Information Methods ---

    /**
     * Gets current palette colors as a read-only list
     */
    public List<PaletteColor> getPaletteColors() {
        return new ArrayList<>(paletteColors);
    }

    /**
     * Gets just the ColorEntry objects for simpler usage
     */
    public List<ColorEntry> getColorEntries() {
        return paletteColors.stream()
                .map(PaletteColor::getColor)
                .collect(Collectors.toList());
    }

    public int getPaletteSize() { return paletteColors.size(); }
    public int getMaxPaletteSize() { return MAX_PALETTE_SIZE; }
    public boolean isPaletteFull() { return paletteColors.size() >= MAX_PALETTE_SIZE; }
    public boolean isPaletteEmpty() { return paletteColors.isEmpty(); }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    /**
     * Gets a summary of the current palette
     */
    public String getPaletteSummary() {
        if (paletteColors.isEmpty()) {
            return "Empty palette";
        }

        long dominantCount = paletteColors.stream()
                .filter(pc -> pc.getSource() == PaletteColor.ColorSource.DOMINANT_EXTRACTION)
                .count();
        long userPickCount = paletteColors.stream()
                .filter(PaletteColor::isUserPicked)
                .count();

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Palette: %d/%d colors", paletteColors.size(), MAX_PALETTE_SIZE));
        summary.append(String.format(" (%d dominant, %d user-picked)", dominantCount, userPickCount));

        return summary.toString();
    }

    // --- Result Classes ---

    public static class AddColorResult {
        private final boolean success;
        private final String message;

        public AddColorResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class RemoveColorResult {
        private final boolean success;
        private final PaletteColor removedColor;
        private final String message;

        public RemoveColorResult(boolean success, PaletteColor removedColor, String message) {
            this.success = success;
            this.removedColor = removedColor;
            this.message = message;
        }

        // Overload for convenience (no removedColor available)
        public RemoveColorResult(boolean success, String message) {
            this(success, null, message);
        }

        public boolean isSuccess() { return success; }
        public PaletteColor getRemovedColor() { return removedColor; }
        public String getMessage() { return message; }
    }

    public static class ReplaceColorResult {
        private final boolean success;
        private final PaletteColor oldColor;
        private final PaletteColor newColor;
        private final String message;

        public ReplaceColorResult(boolean success, PaletteColor oldColor, PaletteColor newColor, String message) {
            this.success = success;
            this.oldColor = oldColor;
            this.newColor = newColor;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public PaletteColor getOldColor() { return oldColor; }
        public PaletteColor getNewColor() { return newColor; }
        public String getMessage() { return message; }
    }
}
