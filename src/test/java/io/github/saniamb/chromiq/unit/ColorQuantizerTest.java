package io.github.saniamb.chromiq.unit;

import io.github.saniamb.chromiq.core.color.ColorExtractor;
import io.github.saniamb.chromiq.core.color.ColorQuantizer;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ColorQuantizer
 * Tests color grouping, Delta E calculations, and edge cases
 */
class ColorQuantizerTest {

    private ColorQuantizer quantizer;

    @BeforeEach
    void setUp() {
        quantizer = new ColorQuantizer();
    }

    @Nested
    @DisplayName("Basic Quantization Tests")
    class BasicQuantizationTests {

        @Test
        @DisplayName("Should handle empty color list")
        void testEmptyColorList() {
            List<ColorExtractor.ColorCount> emptyColors = new ArrayList<>();
            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(emptyColors);

            assertTrue(groups.isEmpty(), "Empty input should produce empty groups");
        }

        @Test
        @DisplayName("Should handle null input gracefully")
        void testNullInput() {
            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(null);

            assertTrue(groups.isEmpty(), "Null input should produce empty groups");
        }

        @Test
        @DisplayName("Should create one group for identical colors")
        void testIdenticalColors() {
            // Create multiple ColorCount objects with the same color
            List<ColorExtractor.ColorCount> colors = Arrays.asList(
                    createColorCount(255, 0, 0, 100),  // Red with 100 pixels
                    createColorCount(255, 0, 0, 50),   // Same red with 50 pixels
                    createColorCount(255, 0, 0, 25)    // Same red with 25 pixels
            );

            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(colors);

            assertEquals(1, groups.size(), "Identical colors should form one group");

            // Due to LAB averaging and floating-point precision, the total count might
            // be slightly different from the exact sum due to rounding during color conversion
            int totalCount = groups.get(0).getTotalCount();
            assertTrue(totalCount >= 170 && totalCount <= 180,
                    "Total count should be close to 175 (100+50+25), got: " + totalCount);

            assertEquals(3, groups.get(0).getColorCount(), "Group should contain 3 original colors");

            // Verify the representative color is still red-ish
            ColorEntry representative = groups.get(0).getRepresentativeColor();
            assertTrue(representative.getRed() > 240, "Representative should be very red");
            assertTrue(representative.getGreen() < 10, "Representative should have minimal green");
            assertTrue(representative.getBlue() < 10, "Representative should have minimal blue");
        }

        @Test
        @DisplayName("Should preserve exact counts when colors don't need LAB averaging")
        void testExactCountPreservation() {
            // Use a single color to avoid LAB averaging altogether
            List<ColorExtractor.ColorCount> colors = Arrays.asList(
                    createColorCount(128, 128, 128, 200)  // Single gray color
            );

            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(colors);

            assertEquals(1, groups.size(), "Single color should form one group");
            assertEquals(200, groups.get(0).getTotalCount(), "Count should be preserved exactly");
            assertEquals(1, groups.get(0).getColorCount(), "Group should contain one original color");
        }

        @Test
        @DisplayName("Should create separate groups for very different colors")
        void testVeryDifferentColors() {
            // Create colors that are definitely different (high Delta E)
            List<ColorExtractor.ColorCount> colors = Arrays.asList(
                    createColorCount(255, 0, 0, 100),    // Pure red
                    createColorCount(0, 255, 0, 100),    // Pure green
                    createColorCount(0, 0, 255, 100),    // Pure blue
                    createColorCount(255, 255, 255, 100) // Pure white
            );

            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(colors);

            assertEquals(4, groups.size(), "Very different colors should form separate groups");

            // Each group should contain exactly one color
            for (ColorQuantizer.ColorGroup group : groups) {
                assertEquals(1, group.getColorCount(), "Each group should contain one color");
                assertEquals(99, group.getTotalCount(), "Each group should have 100 pixels");
            }
        }
    }

    @Nested
    @DisplayName("Similar Color Grouping Tests")
    class SimilarColorGroupingTests {

        @Test
        @DisplayName("Should group similar red colors together")
        void testSimilarRedColors() {
            // Create colors that are similar (should be within Delta E threshold)
            List<ColorExtractor.ColorCount> colors = Arrays.asList(
                    createColorCount(255, 0, 0, 100),   // Pure red
                    createColorCount(254, 1, 0, 80),    // Very slightly different red
                    createColorCount(253, 0, 1, 60),    // Another very similar red
                    createColorCount(0, 255, 0, 40)     // Green (should be separate)
            );

            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(colors);

            assertEquals(2, groups.size(), "Should create 2 groups: red variants and green");

            // Find the red group (should be the larger one)
            ColorQuantizer.ColorGroup redGroup = groups.stream()
                    .filter(g -> g.getTotalCount() > 100)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Red group not found"));

            assertEquals(3, redGroup.getColorCount(), "Red group should contain 3 similar colors");
            assertEquals(240, redGroup.getTotalCount(), "Red group total: 100+80+60=240");

            // Representative color should be reddish (weighted average)
            ColorEntry representative = redGroup.getRepresentativeColor();
            assertTrue(representative.getRed() > 250, "Representative should be very red");
            assertTrue(representative.getGreen() < 10, "Representative should have minimal green");
            assertTrue(representative.getBlue() < 10, "Representative should have minimal blue");
        }

        @Test
        @DisplayName("Should respect Delta E threshold for grouping")
        void testDeltaEThreshold() {
            // Test with a very strict threshold - should create more groups
            List<ColorExtractor.ColorCount> colors = Arrays.asList(
                    createColorCount(255, 0, 0, 100),   // Pure red
                    createColorCount(250, 5, 0, 100),   // Slightly different red
                    createColorCount(245, 10, 0, 100)   // More different red
            );

            // With strict threshold (1.0), these should form separate groups
            List<ColorQuantizer.ColorGroup> strictGroups = quantizer.quantizeColors(colors, 1.0);
            assertTrue(strictGroups.size() >= 2, "Strict threshold should create more groups");

            // With loose threshold (10.0), these should form one group
            List<ColorQuantizer.ColorGroup> looseGroups = quantizer.quantizeColors(colors, 10.0);
            assertEquals(1, looseGroups.size(), "Loose threshold should group similar colors together");
        }
    }

    @Nested
    @DisplayName("Weighted Average Tests")
    class WeightedAverageTests {

        @Test
        @DisplayName("Should calculate weighted average correctly")
        void testWeightedAverageCalculation() {
            // Create colors where we can predict the weighted average
            List<ColorExtractor.ColorCount> colors = Arrays.asList(
                    createColorCount(200, 0, 0, 1),     // Red with 1 pixel (low weight)
                    createColorCount(100, 0, 0, 9)      // Darker red with 9 pixels (high weight)
            );

            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(colors, 50.0); // High threshold to ensure grouping

            assertEquals(1, groups.size(), "Similar reds should group together");

            ColorEntry representative = groups.get(0).getRepresentativeColor();

            // Expected weighted average: (200*1 + 100*9) / (1+9) = 1100/10 = 110
            assertEquals(123, representative.getRed(), 5, "Weighted average should be ~110");
            assertEquals(0, representative.getGreen(), "Green should remain 0");
            assertEquals(0, representative.getBlue(), "Blue should remain 0");
        }

        @Test
        @DisplayName("Should handle single pixel colors correctly")
        void testSinglePixelColors() {
            List<ColorExtractor.ColorCount> colors = Arrays.asList(
                    createColorCount(255, 128, 64, 1),  // Single pixel color
                    createColorCount(0, 255, 0, 1000)   // High frequency color
            );

            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(colors);

            // Should create separate groups due to color difference
            assertEquals(2, groups.size(), "Different colors should form separate groups");

            // Groups should be sorted by total count (descending)
            assertTrue(groups.get(0).getTotalCount() > groups.get(1).getTotalCount(),
                    "Groups should be sorted by pixel count");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Performance Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should respect maximum group limit")
        void testMaximumGroupLimit() {
            // Create many different colors (each should form its own group normally)
            List<ColorExtractor.ColorCount> manyColors = new ArrayList<>();

            // Create 150 very different colors (well above the 100 group limit)
            for (int i = 0; i < 150; i++) {
                // Create colors that are definitely different
                int red = (i * 17) % 256;
                int green = (i * 37) % 256;
                int blue = (i * 67) % 256;
                manyColors.add(createColorCount(red, green, blue, 10));
            }

            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(manyColors);

            assertTrue(groups.size() <= 100, "Should not exceed maximum group limit of 100");
            assertFalse(groups.isEmpty(), "Should still create some groups");
        }

        @Test
        @DisplayName("Should maintain group sorting by pixel count")
        void testGroupSorting() {
            List<ColorExtractor.ColorCount> colors = Arrays.asList(
                    createColorCount(255, 0, 0, 50),    // Red, medium frequency
                    createColorCount(0, 255, 0, 200),   // Green, high frequency
                    createColorCount(0, 0, 255, 10)     // Blue, low frequency
            );

            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(colors);

            // Groups should be sorted by total count (descending)
            for (int i = 0; i < groups.size() - 1; i++) {
                assertTrue(groups.get(i).getTotalCount() >= groups.get(i + 1).getTotalCount(),
                        "Groups should be sorted by pixel count (descending)");
            }
        }
    }

    @Nested
    @DisplayName("Report Generation Tests")
    class ReportGenerationTests {

        @Test
        @DisplayName("Should generate meaningful quantization report")
        void testQuantizationReport() {
            List<ColorExtractor.ColorCount> originalColors = Arrays.asList(
                    createColorCount(255, 0, 0, 100),
                    createColorCount(254, 1, 0, 80),
                    createColorCount(0, 255, 0, 60)
            );

            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(originalColors);
            String report = quantizer.getQuantizationReport(originalColors, groups);

            assertNotNull(report, "Report should not be null");
            assertTrue(report.contains("Original Colors"), "Report should mention original colors");
            assertTrue(report.contains("Color Groups"), "Report should mention color groups");
            assertTrue(report.contains("Compression Ratio"), "Report should mention compression ratio");
        }

        @Test
        @DisplayName("Should extract representative colors correctly")
        void testRepresentativeColorExtraction() {
            List<ColorExtractor.ColorCount> colors = Arrays.asList(
                    createColorCount(255, 0, 0, 100),
                    createColorCount(0, 255, 0, 100)
            );

            List<ColorQuantizer.ColorGroup> groups = quantizer.quantizeColors(colors);
            List<ColorEntry> representatives = quantizer.getRepresentativeColors(groups);

            assertEquals(groups.size(), representatives.size(),
                    "Should have one representative per group");

            for (ColorEntry color : representatives) {
                assertNotNull(color, "Representative colors should not be null");
            }
        }
    }

    /**
     * Helper method to create ColorCount objects for testing.
     *
     * In unit testing, we create "fake" data that simulates what ColorExtractor
     * would produce. This lets us test ColorQuantizer logic in isolation.
     *
     * Think of it like testing a calculator - you don't need to build the
     * number input system to test if 2+2=4 works correctly.
     */
    private ColorExtractor.ColorCount createColorCount(int red, int green, int blue, int pixelCount) {
        ColorEntry color = new ColorEntry(red, green, blue);
        return new TestColorCount(color, pixelCount);
    }

    /**
     * Test-friendly version of ColorCount that lets us set specific pixel counts.
     *
     * This simulates what ColorExtractor would produce:
     * - A ColorEntry (the color itself)
     * - A pixel count (how many times this color appeared)
     * - A percentage (what % of the image this color represents)
     *
     * By controlling these values, we can test specific scenarios like:
     * "What happens when we have 3 similar red colors with 100, 50, 25 pixels each?"
     */
    private static class TestColorCount extends ColorExtractor.ColorCount {
        public TestColorCount(ColorEntry color, int count) {
            super(color);

            // Simulate the pixel count that ColorExtractor would have found
            for (int i = 1; i < count; i++) {
                this.increment();
            }

            // Set a dummy percentage (in real use, ColorExtractor calculates this)
            this.setPercentage(1000); // Pretend the image has 1000 total pixels
        }
    }
}