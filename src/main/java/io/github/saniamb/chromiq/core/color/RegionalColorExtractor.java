package io.github.saniamb.chromiq.core.color;

import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.utils.Logger;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * RegionalColorExtractor - Interactive Color Picking from Image Regions
 * ====================================================================
 *
 * Extracts the most representative color from a small region around a user's
 * click point on an image. This enables precise, interactive color selection
 * for palette customization.
 *
 * Key Features:
 * 1. Extracts colors from small regions (default 20x20 pixels)
 * 2. Handles edge cases when clicking near image borders
 * 3. Returns the most dominant color from that region
 * 4. Uses existing ColorExtractor logic for consistency
 *
 * Usage:
 * RegionalColorExtractor extractor = new RegionalColorExtractor();
 * ColorEntry pickedColor = extractor.extractFromRegion(image, clickX, clickY);
 */
public class RegionalColorExtractor {

    // Default region size: 20x20 pixels (400 total pixels)
    // This is large enough to capture local color variety but small enough
    // to stay focused on the user's intended selection
    private static final int DEFAULT_REGION_SIZE = 20;

    // Minimum region size to prevent issues with very small regions
    private static final int MIN_REGION_SIZE = 5;

    // Maximum region size to keep processing fast
    private static final int MAX_REGION_SIZE = 50;

    // Our worker components - reuse the existing tested logic
    private final ColorExtractor colorExtractor;
    private final ColorQuantizer colorQuantizer;

    /**
     * Constructor - creates extractor with default components
     */
    public RegionalColorExtractor() {
        this.colorExtractor = new ColorExtractor();
        this.colorQuantizer = new ColorQuantizer();
    }

    /**
     * Extracts the most representative color from a region around click coordinates.
     *
     * @param image The source image (should be ARGB format)
     * @param clickX X coordinate of user click (0 to image.getWidth()-1)
     * @param clickY Y coordinate of user click (0 to image.getHeight()-1)
     * @return Most representative color from that region
     * @throws IllegalArgumentException if image is null or coordinates are invalid
     */
    public ColorEntry extractFromRegion(BufferedImage image, int clickX, int clickY) {
        return extractFromRegion(image, clickX, clickY, DEFAULT_REGION_SIZE);
    }

    /**
     * Extracts the EXACT color from a single pixel (like Adobe Color, Coolors.co).
     * This gives you the precise color at that coordinate, just like an eyedropper tool.
     *
     * @param image The source image (should be ARGB format)
     * @param clickX X coordinate of user click
     * @param clickY Y coordinate of user click
     * @return Exact color of that single pixel
     * @throws IllegalArgumentException if image is null or coordinates are invalid
     */
    public ColorEntry extractExactPixel(BufferedImage image, int clickX, int clickY) {
        // Basic validation (reuse existing method)
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        if (clickX < 0 || clickX >= image.getWidth()) {
            throw new IllegalArgumentException(String.format(
                    "clickX (%d) must be between 0 and %d", clickX, image.getWidth() - 1));
        }

        if (clickY < 0 || clickY >= image.getHeight()) {
            throw new IllegalArgumentException(String.format(
                    "clickY (%d) must be between 0 and %d", clickY, image.getHeight() - 1));
        }

        Logger.info(String.format("Extracting exact pixel color at (%d,%d)", clickX, clickY));

        try {
            // Get the exact ARGB value from that single pixel
            int argb = image.getRGB(clickX, clickY);

            // Extract RGB components
            int alpha = (argb >>> 24) & 0xFF;
            int red = (argb >>> 16) & 0xFF;
            int green = (argb >>> 8) & 0xFF;
            int blue = argb & 0xFF;

            // Convert alpha from 0-255 to 0.0-1.0
            double normalizedAlpha = alpha / 255.0;

            ColorEntry exactColor = new ColorEntry(red, green, blue, "Exact Pixel", normalizedAlpha);

            Logger.info(String.format("Exact pixel extraction complete: %s", exactColor.getHex()));

            return exactColor;

        } catch (Exception e) {
            Logger.info("Error during exact pixel extraction: " + e.getMessage());
            throw new RuntimeException("Failed to extract exact pixel color", e);
        }
    }

    /**
     * Extracts color from region with custom region size.
     *
     * @param image The source image
     * @param clickX X coordinate of user click
     * @param clickY Y coordinate of user click
     * @param regionSize Size of the region to analyze (regionSize x regionSize pixels)
     * @return Most representative color from that region
     * @throws IllegalArgumentException if parameters are invalid
     */
    public ColorEntry extractFromRegion(BufferedImage image, int clickX, int clickY, int regionSize) {
        // Input validation
        validateInputs(image, clickX, clickY, regionSize);

        Logger.info(String.format("Extracting color from region at (%d,%d) with size %dx%d",
                clickX, clickY, regionSize, regionSize));

        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Calculate the region boundaries, handling image edges
            RegionBounds bounds = calculateRegionBounds(image, clickX, clickY, regionSize);

            Logger.info(String.format("Region bounds: x=%d-%d, y=%d-%d (actual size: %dx%d)",
                    bounds.startX, bounds.endX, bounds.startY, bounds.endY,
                    bounds.actualWidth, bounds.actualHeight));

            // Step 2: Extract the sub-image for this region
            BufferedImage regionImage = extractRegionImage(image, bounds);

            // Step 3: Find the most representative color from this region
            ColorEntry representativeColor = findRepresentativeColor(regionImage);

            long processingTime = System.currentTimeMillis() - startTime;

            Logger.info(String.format("Regional color extraction complete: %s in %dms",
                    representativeColor.getHex(), processingTime));

            return representativeColor;

        } catch (Exception e) {
            Logger.info("Error during regional color extraction: " + e.getMessage());
            throw new RuntimeException("Failed to extract color from region", e);
        }
    }

    /**
     * Validates input parameters for color extraction.
     */
    private void validateInputs(BufferedImage image, int clickX, int clickY, int regionSize) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        if (clickX < 0 || clickX >= image.getWidth()) {
            throw new IllegalArgumentException(String.format(
                    "clickX (%d) must be between 0 and %d", clickX, image.getWidth() - 1));
        }

        if (clickY < 0 || clickY >= image.getHeight()) {
            throw new IllegalArgumentException(String.format(
                    "clickY (%d) must be between 0 and %d", clickY, image.getHeight() - 1));
        }

        if (regionSize < MIN_REGION_SIZE || regionSize > MAX_REGION_SIZE) {
            throw new IllegalArgumentException(String.format(
                    "regionSize (%d) must be between %d and %d",
                    regionSize, MIN_REGION_SIZE, MAX_REGION_SIZE));
        }
    }

    /**
     * Helper class to store region boundary information
     */
    private static class RegionBounds {
        final int startX, endX, startY, endY;
        final int actualWidth, actualHeight;

        RegionBounds(int startX, int endX, int startY, int endY) {
            this.startX = startX;
            this.endX = endX;
            this.startY = startY;
            this.endY = endY;
            this.actualWidth = endX - startX;
            this.actualHeight = endY - startY;
        }
    }

    /**
     * Calculates region boundaries, ensuring we don't go outside the image.
     * Centers the region around the click point when possible.
     */
    private RegionBounds calculateRegionBounds(BufferedImage image, int clickX, int clickY, int regionSize) {
        int halfSize = regionSize / 2;

        // Calculate ideal region bounds (centered on click)
        int idealStartX = clickX - halfSize;
        int idealEndX = clickX + halfSize;
        int idealStartY = clickY - halfSize;
        int idealEndY = clickY + halfSize;

        // Clamp to image boundaries
        int actualStartX = Math.max(0, idealStartX);
        int actualEndX = Math.min(image.getWidth(), idealEndX);
        int actualStartY = Math.max(0, idealStartY);
        int actualEndY = Math.min(image.getHeight(), idealEndY);

        // Ensure we have at least a 1x1 region
        if (actualEndX <= actualStartX) actualEndX = actualStartX + 1;
        if (actualEndY <= actualStartY) actualEndY = actualStartY + 1;

        return new RegionBounds(actualStartX, actualEndX, actualStartY, actualEndY);
    }

    /**
     * Extracts a sub-image from the specified region bounds.
     */
    private BufferedImage extractRegionImage(BufferedImage sourceImage, RegionBounds bounds) {
        return sourceImage.getSubimage(
                bounds.startX,
                bounds.startY,
                bounds.actualWidth,
                bounds.actualHeight
        );
    }

    /**
     * Finds the most representative color from a region image.
     * Uses the same logic as your dominant color extraction but optimized for small regions.
     */
    private ColorEntry findRepresentativeColor(BufferedImage regionImage) {
        // Extract all colors from this small region
        List<ColorExtractor.ColorCount> regionColors = colorExtractor.extractColors(regionImage);

        if (regionColors.isEmpty()) {
            // Fallback: return white if no colors found (shouldn't happen but safety first)
            Logger.info("No colors found in region, returning white as fallback");
            return new ColorEntry(255, 255, 255, "Fallback White");
        }

        // For small regions, often the most common color is the best choice
        if (regionColors.size() <= 3) {
            // Very simple region - just return the most common color
            ColorExtractor.ColorCount mostCommon = regionColors.get(0); // Already sorted by frequency
            Logger.info(String.format("Simple region with %d colors, using most common: %s",
                    regionColors.size(), mostCommon.getColor().getHex()));
            return mostCommon.getColor();
        }

        // More complex region - use quantization to find the best representative
        List<ColorQuantizer.ColorGroup> quantizedGroups = colorQuantizer.quantizeColors(regionColors, 1);

        if (!quantizedGroups.isEmpty()) {
            ColorEntry representative = quantizedGroups.get(0).getRepresentativeColor();
            Logger.info(String.format("Complex region with %d colors, quantized to: %s",
                    regionColors.size(), representative.getHex()));
            return representative;
        }

        // Final fallback
        return regionColors.get(0).getColor();
    }
}