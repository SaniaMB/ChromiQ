package io.github.saniamb.chromiq.core.color;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import java.awt.image.BufferedImage;
import java.util.*;
import io.github.saniamb.chromiq.core.utils.Logger;

/**
 * ColorExtractor - Core Color Processing Component
 * ===============================================
 * Optimized version:
 * - Uses bulk pixel read for faster performance
 * - Avoids redundant map lookups
 * - Preserves memory efficiency and style
 */
public class ColorExtractor {

    // Transparency threshold: pixels with alpha below this are considered transparent
    private static final int ALPHA_THRESHOLD =  128;   // 0.5

    /**
     * Represents a color with its occurrence count and percentage in an image.
     * This is the primary output format for color extraction results.
     */
    public static class ColorCount implements Comparable<ColorCount>{

        private final ColorEntry color;
        private int count;
        private double percentage; // Calculated when needed

        protected ColorCount(ColorEntry color) {
            this.color = color;
            this.count = 0;
            this.percentage = 0.0;
        }

        public void increment() { this.count++; }

        public void setPercentage(int totalPixels){
            if (totalPixels <= 0) {
                this.percentage = 0.0;
            } else {
                this.percentage = (count * 100.0) / totalPixels;
            }
        }

        // Getters
        public ColorEntry getColor() { return color; }
        public int getCount() { return count; }
        public double getPercentage() { return percentage; }

        // For sorting by count (descending)
        @Override
        public int compareTo(ColorCount other) {
            return Integer.compare(other.count, this.count); // Reverse order for descending
        }

        @Override
        public String toString(){
            return String.format("ColorCount{%s: %d pixels (%.2f%%)}", color != null ? color.getHex() : "null", count, percentage);
        }

        @Override
        public boolean equals(Object obj){
            if(this == obj) return true;
            if(!(obj instanceof ColorCount that)) return false;
            return count == that.count && Objects.equals(color, that.color);
        }

        @Override
        public int hashCode() {
            return Objects.hash(color, count);
        }
    }

    /**
     * Extracts all unique colors from the given image with full analysis.
     * Optimizations:
     * - Reads all pixels in bulk into an int[] array (faster than nested loops with getRGB)
     * - Uses computeIfAbsent to reduce map lookups
     * - Skips transparency checks early for efficiency
     *
     * @param image The BufferedImage to analyze (should be ARGB from ImageInputHandler)
     * @return List of ColorCount objects, sorted by frequency (most common first)
     * @throws IllegalArgumentException if image is null
     */
    public List<ColorCount> extractColors(BufferedImage image){
       if(image == null){
           throw new IllegalArgumentException("Image cannot be null");
       }

       Logger.info("Starting color extraction for " + image.getWidth() + "x" + image.getHeight() + " image");
       long startTime = System.currentTimeMillis();

       int width = image.getWidth();
       int height = image.getHeight();

        //Bulk read of pixels to avoid nested loop of width and height
        int[] pixels = image.getRGB(0,0,width,height,null,0,width);

        Map<Integer, ColorCount> colorMap= new HashMap<>();
        int processedPixels = 0;
        int transparentPixels = 0;

        for(int argb : pixels){
            processedPixels++;

            int alpha = (argb >>> 24) & 0xFF;

            if(alpha < ALPHA_THRESHOLD){
                transparentPixels++;
                continue;
            }

            int red = (argb >>> 16) & 0xFF;
            int green = (argb >>> 8) & 0xFF;
            int blue  =  argb & 0xFF;

            // RGB key without alpha
            int rgbKey = (red << 16) | (green << 8) | blue;

            // Avoid double lookup using computeIfAbsent (Double looking -> get, put)
            colorMap.computeIfAbsent(rgbKey,k ->{
                double normalizedAlpha = alpha /255.0;
                return new ColorCount(new ColorEntry(red, green, blue, null, normalizedAlpha));
            }).increment();
        }

        // Convert to list and calculate percentages
        int visiblePixels = processedPixels - transparentPixels;
        List<ColorCount> colorCounts = new ArrayList<>(colorMap.values());

        for(ColorCount cc : colorCounts){
            cc.setPercentage(visiblePixels);
        }

        Collections.sort(colorCounts);

        long processingTime = System.currentTimeMillis() - startTime;

        Logger.info(String.format("Color extraction complete: %d unique colors found in %dms",
                colorCounts.size(), processingTime));

        Logger.info(String.format("Processed %d pixels (%d visible, %d transparent)",
                processedPixels, visiblePixels, transparentPixels));

        return colorCounts;
    }

    // --- The rest of the methods (extractUniqueColors, extractTopColors, getColorStatistics, getTopColorsSummary)
    // remain unchanged since they’re already efficient and clean ---

    public List<ColorEntry> extractUniqueColors(BufferedImage image){
        List<ColorCount> colorCounts = extractColors(image);
        return colorCounts.stream().map(ColorCount::getColor).toList();
    }

    // Might miss some key colors in the image if only a limited amount of color is extracted, Hence this method
    // won't be too handy after adding quantization and dominant color extraction steps in the Color Extraction Pipeline

     public List<ColorCount> extractTopColors(BufferedImage image, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        return extractColors(image).stream()
                .limit(limit)
                .toList();
    }

    public String getColorStatistics(BufferedImage image) {
        List<ColorCount> colors = extractColors(image);
        int totalPixels = image.getWidth() * image.getHeight();

        if (colors.isEmpty()) {
            return "No visible colors found in image (all transparent)";
        }

        ColorCount mostCommon = colors.get(0);
        ColorCount leastCommon = colors.get(colors.size() - 1);

        int visiblePixels = colors.stream().mapToInt(ColorCount::getCount).sum();
        int transparentPixels = totalPixels - visiblePixels;

        return String.format(
                """
                        ChromiQ Color Analysis Report
                        ============================
                        Image Size: %dx%d pixels (%,d total)
                        Visible Pixels: %,d (%.1f%%)
                        Transparent Pixels: %,d (%.1f%%)
                        Unique Colors: %,d
                        
                        Color Distribution:
                        • Most common: %s (%,d pixels, %.2f%%)
                        • Least common: %s (%d pixel%s, %.2f%%)
                        • Average pixels per color: %.1f""",

                image.getWidth(), image.getHeight(), totalPixels,
                visiblePixels, (visiblePixels * 100.0) / totalPixels,
                transparentPixels, (transparentPixels * 100.0) / totalPixels,
                colors.size(),
                mostCommon.getColor().getHex(), mostCommon.getCount(), mostCommon.getPercentage(),
                leastCommon.getColor().getHex(), leastCommon.getCount(),
                leastCommon.getCount() == 1 ? "" : "s", leastCommon.getPercentage(),
                (double) visiblePixels / colors.size()
        );
    }

    // Might become useless later
    public String getTopColorsSummary(BufferedImage image, int topCount) {
        List<ColorCount> colors = extractTopColors(image, topCount);

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Top %d Colors:\n", Math.min(topCount, colors.size())));

        for (int i = 0; i < colors.size(); i++) {
            ColorCount cc = colors.get(i);
            summary.append(String.format("%2d. %s - %,d pixels (%.2f%%)\n",
                    i + 1, cc.getColor().getHex(), cc.getCount(), cc.getPercentage()));
        }

        return summary.toString();
    }
}
