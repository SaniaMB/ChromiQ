package io.github.saniamb.chromiq.core.color.models;
import java.util.Objects;

/**
 * Represents a single color with RGB (and optional alpha) values and optional name.
 * Automatically calculates HEX, HSL, and LAB representations from RGB values.
 * RGB values are the source of truth - other formats are calculated on demand.
 */
public class ColorEntry {

    // --- RGB values (0-255 range) ---
    private int red;
    private int green;
    private int blue;

    // --- Optional alpha channel (0.0-1.0) ---
    private double alpha = 1.0;

    // --- Optional name for the color ---
    private String name;

    // --- Cached representations for performance ---
    private String cachedHex;
    private double[] cachedHSL;
    private double[] cachedLAB;

    // --- Constructors ---

    /**
     * Creates a ColorEntry with RGB values and no name.
     *
     * @param red Red component (0-255)
     * @param green Green component (0-255)
     * @param blue Blue component (0-255)
     * @throws IllegalArgumentException if any RGB value is invalid
     */
    public ColorEntry(int red, int green, int blue){
        this(red,green,blue,null,1.0);
    }


    /**
     * Creates a ColorEntry with RGB values and a name.
     *
     * @param red Red component (0-255)
     * @param green Green component (0-255)
     * @param blue Blue component (0-255)
     * @param name Optional name for the color
     * @throws IllegalArgumentException if any RGB value is invalid
     */
    public ColorEntry(int red, int green, int blue, String name){
        this(red,green,blue,name,1.0);
    }


    /**
     * Creates a ColorEntry with RGB values, a name, and an alpha value.
     *
     * @param red Red component (0-255)
     * @param green Green component (0-255)
     * @param blue Blue component (0-255)
     * @param name Optional name for the color
     * @param alpha Alpha value (0.0-1.0)
     * @throws IllegalArgumentException if RGB or alpha values are invalid
     */
    public ColorEntry(int red, int green, int blue, String name, double alpha){
        setRGB(red ,green, blue);
        setAlpha(alpha);
        this.name = name;
    }

    /**
     * Creates a ColorEntry from a HEX color string.
     *
     * @param hexColor HEX color string (e.g., "#FF0000", "FF0000", "#F00")
     * @throws IllegalArgumentException if HEX string is invalid
     */
    public ColorEntry(String hexColor) {
        this(hexColor, null);
    }

    /**
     * Creates a ColorEntry from a HEX color string with a name.
     *
     * @param hexColor HEX color string
     * @param name Optional name for the color
     * @throws IllegalArgumentException if HEX string is invalid
     */
    public ColorEntry(String hexColor, String name){
        int[] rgb = parseHexToRGB(hexColor);
        setRGB(rgb[0], rgb[1], rgb[2]);
        this.name = name;
    }

    // --- RGB Getters and Setters ---
    public int getRed() { return red; }
    public int getGreen() { return green; }
    public int getBlue() { return blue; }
    public double getAlpha() { return alpha; }

    // --- Name Management ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean hasName() { return name != null && !name.trim().isEmpty(); }

    /**
     * Sets RGB values with validation and invalidates cached representations.
     */
    public void setRGB(int red, int green, int blue){
        validateRGBValue(red,"Red");
        validateRGBValue(green,"Green");
        validateRGBValue(blue,"Blue");

        this.red = red;
        this.green = green;
        this.blue = blue;

        cachedHex = null;
        cachedHSL = null;
        cachedLAB = null;
    }

    /**
     * Sets alpha value (0.0 - 1.0)
     */
    public void setAlpha(double alpha){
        if(alpha < 0.0 || alpha > 1.0)
            throw new IllegalArgumentException("Alpha must be between 0.0 and 1.0");
        this.alpha = alpha;
    }

    // --- HEX Conversion ---

    /**
     * Returns HEX representation of this color.
     *
     * @return HEX string with # prefix
     */
    public String getHex() {
        if (cachedHex == null)
            cachedHex = String.format("#%02X%02X%02X", red, green, blue);
        return cachedHex;
    }

    /**
     * Updates color from HEX string.
     *
     * @param hexColor HEX color string
     * @throws IllegalArgumentException if HEX string is invalid
     */
    public void setHex(String hexColor) {
        int[] rgb = parseHexToRGB(hexColor);
        setRGB(rgb[0], rgb[1], rgb[2]);
    }

    // --- Helpers ---
    public void validateRGBValue(int value, String component){
        if(value < 0 || value > 255)
            throw  new IllegalArgumentException(component + " value must be between 0 and 255");
    }
    private int[] parseHexToRGB(String hexColor) {
        if (hexColor == null || hexColor.trim().isEmpty())
            throw new IllegalArgumentException("HEX color cannot be null or empty");

        String hex = hexColor.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);

        // Short HEX format
        if (hex.length() == 3)
            hex = "" + hex.charAt(0) + hex.charAt(0)
                    + hex.charAt(1) + hex.charAt(1)
                    + hex.charAt(2) + hex.charAt(2);

        if (hex.length() != 6)
            throw new IllegalArgumentException("Invalid HEX format: " + hexColor);

        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid HEX value: " + hexColor, e);
        }
    }

    // --- HSL Conversion ---

    /**
     * Returns HSL representation as [Hue(0-360), Saturation(0-100), Lightness(0-100)]
     */
    public double[] getHSL() {
        if (cachedHSL != null) return cachedHSL;

        double r = red / 255.0;
        double g = green / 255.0;
        double b = blue / 255.0;

        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double delta = max - min;

        double h = 0, s = 0;
        double l = (max + min) / 2.0;

        if (delta != 0) {
            s = l > 0.5 ? delta / (2.0 - max - min) : delta / (max + min);
            if (max == r) h = ((g - b) / delta + (g < b ? 6 : 0));
            else if (max == g) h = ((b - r) / delta + 2);
            else h = ((r - g) / delta + 4);
            h /= 6;
        }

        cachedHSL = new double[]{h * 360, s * 100, l * 100};
        return cachedHSL;
    }

    // --- LAB Conversion (accurate) ---

    /**
     * Returns LAB representation as [L(0-100), A(-127 to 127), B(-127 to 127)]
     */
    public double[] getLAB() {
        if (cachedLAB != null) return cachedLAB;

        // RGB -> XYZ
        double r = pivotRGB(red / 255.0);
        double g = pivotRGB(green / 255.0);
        double b = pivotRGB(blue / 255.0);

        double X = r * 0.4124 + g * 0.3576 + b * 0.1805;
        double Y = r * 0.2126 + g * 0.7152 + b * 0.0722;
        double Z = r * 0.0193 + g * 0.1192 + b * 0.9505;

        // Normalize for D65 white
        X /= 0.95047;
        Y /= 1.00000;
        Z /= 1.08883;

        // XYZ -> LAB
        double fx = pivotXYZ(X);
        double fy = pivotXYZ(Y);
        double fz = pivotXYZ(Z);

        double L = 116 * fy - 16;
        double A = 500 * (fx - fy);
        double B = 200 * (fy - fz);

        cachedLAB = new double[]{L, A, B};
        return cachedLAB;
    }

    private double pivotRGB(double c) {
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private double pivotXYZ(double t) {
        return t > 0.008856 ? Math.cbrt(t) : (7.787 * t + 16 / 116.0);
    }

    // Add this method to your ColorEntry class

    /**
     * Creates a ColorEntry from LAB color space values.
     * This is the reverse of getLAB() - converts LAB back to RGB.
     *
     * @param L Lightness (0-100)
     * @param A Green-Red axis (-127 to +127, negative=green, positive=red)
     * @param B Blue-Yellow axis (-127 to +127, negative=blue, positive=yellow)
     * @return ColorEntry with RGB values calculated from LAB
     * @throws IllegalArgumentException if LAB values are out of valid ranges
     */
    public static ColorEntry fromLAB(double L, double A, double B) {
        // Validate LAB ranges
        if (L < 0 || L > 100) {
            throw new IllegalArgumentException("L (Lightness) must be between 0 and 100, got: " + L);
        }
        if (A < -127 || A > 127) {
            throw new IllegalArgumentException("A (Green-Red) must be between -127 and 127, got: " + A);
        }
        if (B < -127 || B > 127) {
            throw new IllegalArgumentException("B (Blue-Yellow) must be between -127 and 127, got: " + B);
        }

        // LAB → XYZ conversion
        double fy = (L + 16) / 116.0;
        double fx = A / 500.0 + fy;
        double fz = fy - B / 200.0;

        // Reverse the pivot function used in getLAB()
        double X = reversePivotXYZ(fx) * 0.95047;  // D65 white point
        double Y = reversePivotXYZ(fy) * 1.00000;
        double Z = reversePivotXYZ(fz) * 1.08883;

        // XYZ → RGB conversion (reverse of the matrix used in getLAB())
        double r = X *  3.2406 + Y * -1.5372 + Z * -0.4986;
        double g = X * -0.9689 + Y *  1.8758 + Z *  0.0415;
        double b = X *  0.0557 + Y * -0.2040 + Z *  1.0570;

        // Reverse the gamma correction
        r = reversePivotRGB(r);
        g = reversePivotRGB(g);
        b = reversePivotRGB(b);

        // Convert to 0-255 range and clamp to valid RGB values
        int red   = clampToRGBRange(r * 255);
        int green = clampToRGBRange(g * 255);
        int blue  = clampToRGBRange(b * 255);

        return new ColorEntry(red, green, blue);
    }

    /**
     * Reverses the XYZ pivot function used in getLAB()
     */
    private static double reversePivotXYZ(double t) {
        double t3 = t * t * t;
        return t3 > 0.008856 ? t3 : (t - 16.0/116.0) / 7.787;
    }

    /**
     * Reverses the RGB pivot function used in getLAB()
     */
    private static double reversePivotRGB(double c) {
        return c <= 0.04045 ? c * 12.92 : Math.pow((c + 0.055) / 1.055, 1.0 / 2.4);
    }

    /**
     * Clamps a double value to valid RGB range (0-255)
     */
    private static int clampToRGBRange(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }

    // --- Object Methods ---
    @Override
    public boolean equals(Object obj){
        if (this == obj) return true;
        if ((!(obj instanceof ColorEntry that))) return false;
        return red == that.red && green == that.green && blue == that.blue
                && Double.compare(alpha, that.alpha) == 0 && Objects.equals(name,that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(red, green, blue, alpha, name);
    }


    /*
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ColorEntry that)) return false;
        // Compare only RGB, ignore alpha and name
        return red == that.red && green == that.green && blue == that.blue;
    }

    @Override
    public int hashCode() {
        // Only use RGB for hashing
        return Objects.hash(red, green, blue);
    }

    /*
    public boolean equalsStrict(ColorEntry other) {
        return this.equals(other) && this.alpha == other.alpha && Objects.equals(this.name, other.name);
    }
    */

    @Override
    public String toString() {
        if (hasName())
            return String.format("ColorEntry{%s: RGBA(%d,%d,%d,%.2f) %s}", name, red, green, blue, alpha, getHex());
        else
            return String.format("ColorEntry{RGBA(%d,%d,%d,%.2f) %s}", red, green, blue, alpha, getHex());
    }

}