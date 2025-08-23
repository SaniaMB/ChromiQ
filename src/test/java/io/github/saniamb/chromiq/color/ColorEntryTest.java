package io.github.saniamb.chromiq.color;

import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ColorEntry class.
 */
public class ColorEntryTest {

    // --- RGB Tests ---

    @Test
    public void testRGBCreation() {
        ColorEntry color = new ColorEntry(255, 128, 0);

        assertEquals(255, color.getRed());
        assertEquals(128, color.getGreen());
        assertEquals(0, color.getBlue());
        assertEquals(1.0, color.getAlpha());
        assertNull(color.getName());
    }

    @Test
    public void testRGBWithNameAndAlpha() {
        ColorEntry color = new ColorEntry(100, 200, 50, "Lime", 0.5);

        assertEquals(100, color.getRed());
        assertEquals(200, color.getGreen());
        assertEquals(50, color.getBlue());
        assertEquals(0.5, color.getAlpha());
        assertEquals("Lime", color.getName());
        assertTrue(color.hasName());
    }

    @Test
    public void testSetRGBAndAlpha() {
        ColorEntry color = new ColorEntry(0, 0, 0);
        color.setRGB(10, 20, 30);
        color.setAlpha(0.75);
        color.setName("purple");

        assertEquals(10, color.getRed());
        assertEquals(20, color.getGreen());
        assertEquals(30, color.getBlue());
        assertEquals(0.75, color.getAlpha());
        assertEquals("purple", color.getName());

    }

    // --- HEX Tests ---

    @Test
    public void testHexCreation() {
        ColorEntry red = new ColorEntry("#FF0000");
        ColorEntry green = new ColorEntry("00FF00"); // no #
        ColorEntry blue = new ColorEntry("#00F");

        assertEquals(255, red.getRed());
        assertEquals(0, green.getRed());
        assertEquals(0, blue.getRed());
        assertEquals("#FF0000", red.getHex());
        assertEquals("#00FF00", green.getHex());
        assertEquals("#0000FF", blue.getHex());
    }

    @Test
    public void testSetHex() {
        ColorEntry color = new ColorEntry(0, 0, 0);
        color.setHex("#ABCDEF");

        assertEquals(171, color.getRed());
        assertEquals(205, color.getGreen());
        assertEquals(239, color.getBlue());
        assertEquals("#ABCDEF", color.getHex());
    }

    // --- HSL Tests ---

    @Test
    public void testHSLConversion() {
        ColorEntry red = new ColorEntry(255, 0, 0);
        double[] hsl = red.getHSL();

        assertEquals(0.0, hsl[0], 1.0); // Hue
        assertEquals(100.0, hsl[1], 1.0); // Saturation
        assertEquals(50.0, hsl[2], 1.0); // Lightness

        ColorEntry white = new ColorEntry(255, 255, 255);
        double[] whiteHSL = white.getHSL();
        assertEquals(0.0, whiteHSL[1], 1.0); // Saturation
        assertEquals(100.0, whiteHSL[2], 1.0); // Lightness
    }

    // --- LAB Tests ---

    @Test
    public void testLABConversion() {
        ColorEntry gray = new ColorEntry(128, 128, 128);
        double[] lab = gray.getLAB();

        assertEquals(3, lab.length);
        assertTrue(lab[0] >= 0 && lab[0] <= 100); // L
        assertTrue(lab[1] >= -127 && lab[1] <= 127); // A
        assertTrue(lab[2] >= -127 && lab[2] <= 127); // B
    }

    // --- Exception Tests ---

    @Test
    public void testInvalidRGB() {
        assertThrows(IllegalArgumentException.class, () -> new ColorEntry(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ColorEntry(0, 256, 0));
        assertThrows(IllegalArgumentException.class, () -> new ColorEntry(0, 0, -5));
    }

    @Test
    public void testInvalidHex() {
        assertThrows(IllegalArgumentException.class, () -> new ColorEntry(""));
        assertThrows(IllegalArgumentException.class, () -> new ColorEntry("#GG0000"));
        assertThrows(IllegalArgumentException.class, () -> new ColorEntry("#1234"));
    }

    @Test
    public void testInvalidAlpha() {
        ColorEntry color = new ColorEntry(0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> color.setAlpha(-0.1));
        assertThrows(IllegalArgumentException.class, () -> color.setAlpha(1.1));
    }

    // --- Equals and ToString Tests ---

    @Test
    public void testEqualsAndHashCode() {
        ColorEntry c1 = new ColorEntry(255, 0, 0, "Red", 1.0);
        ColorEntry c2 = new ColorEntry(255, 0, 0, "Red", 1.0);
        ColorEntry c3 = new ColorEntry(255, 0, 0);
        ColorEntry c4 = new ColorEntry(0, 255, 0, "Green");

        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertNotEquals(c1, c4);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testToString() {
        ColorEntry named = new ColorEntry(255, 0, 0, "Red", 0.5);
        ColorEntry unnamed = new ColorEntry(0, 255, 0);

        String strNamed = named.toString();
        String strUnnamed = unnamed.toString();

        assertTrue(strNamed.contains("Red"));
        assertTrue(strNamed.contains("RGBA(255,0,0,0.50)"));
        assertTrue(strNamed.contains("#FF0000"));

        assertTrue(strUnnamed.contains("RGBA(0,255,0,1.00)"));
        assertTrue(strUnnamed.contains("#00FF00"));
        assertFalse(strUnnamed.contains("Red"));

    }
}



