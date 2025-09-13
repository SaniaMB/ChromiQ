package io.github.saniamb.chromiq.core.api;

import io.github.saniamb.chromiq.core.color.DominantColorExtractor;
import io.github.saniamb.chromiq.core.color.RegionalColorExtractor;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.input.ImageInputHandler;
import io.github.saniamb.chromiq.core.palette.PaletteManager;
import io.github.saniamb.chromiq.core.utils.Logger;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PaletteWorkflowController - Complete Palette Creation Workflow
 * ============================================================
 *
 * Manages the full user journey:
 * 1. Upload image → Generate 10 dominant colors automatically
 * 2. Remove unwanted colors from the palette
 * 3. Click image to add specific colors you want
 * 4. Get current palette state at any time
 *
 * This creates a guided, intuitive palette creation experience.
 */
@RestController
@RequestMapping("/api/palette")
@CrossOrigin(origins = "*") // Allow requests from any origin for development
public class PaletteWorkflowController {

    // Backend components
    private final ImageInputHandler imageInputHandler;
    private final DominantColorExtractor dominantColorExtractor;
    private final RegionalColorExtractor regionalColorExtractor;

    // Current session state (in a real app, this would be session/user-based)
    private BufferedImage currentImage;
    private String currentImageName;
    private PaletteManager currentPalette;

    public PaletteWorkflowController() {
        this.imageInputHandler = new ImageInputHandler();
        this.dominantColorExtractor = new DominantColorExtractor();
        this.regionalColorExtractor = new RegionalColorExtractor();
    }

    /**
     * STEP 1: Upload image and generate initial dominant color palette
     * POST /api/palette/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAndGeneratePalette(@RequestParam("image") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate upload
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select an image file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            Logger.info("Starting palette workflow for: " + file.getOriginalFilename());

            // Step 1: Process the image
            currentImage = imageInputHandler.loadFromStream(file.getInputStream());
            currentImageName = file.getOriginalFilename();

            Logger.info("Image processed: " + imageInputHandler.getImageInfo(currentImage));

            // Step 2: Extract 10 dominant colors automatically
            List<DominantColorExtractor.DominantColor> dominantColors =
                    dominantColorExtractor.extractDominantColors(currentImage, 10);

            // Step 3: Initialize palette manager with these dominant colors
            currentPalette = new PaletteManager(dominantColors);
            currentPalette.setImageName(currentImageName);

            // Step 4: Convert palette to JSON format for frontend
            Map<String, Object> paletteData = buildPaletteResponse();

            // Success response with image info + initial palette
            response.put("success", true);
            response.put("message", "Image uploaded and palette generated successfully");
            response.put("image", Map.of(
                    "name", currentImageName,
                    "width", currentImage.getWidth(),
                    "height", currentImage.getHeight(),
                    "info", imageInputHandler.getImageInfo(currentImage)
            ));
            response.put("palette", paletteData);

            Logger.info("Initial palette generated: " + currentPalette.getPaletteSummary());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Logger.info("Error in palette workflow: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to process image and generate palette: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * STEP 2: Remove a color from current palette
     * DELETE /api/palette/colors/{index}
     */
    @DeleteMapping("/colors/{index}")
    public ResponseEntity<Map<String, Object>> removeColor(@PathVariable int index) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (currentPalette == null) {
                response.put("success", false);
                response.put("message", "No palette loaded. Upload an image first.");
                return ResponseEntity.badRequest().body(response);
            }

            // Use your PaletteManager to remove the color
            PaletteManager.RemoveColorResult result = currentPalette.removeColor(index);

            if (result.isSuccess()) {
                response.put("success", true);
                response.put("message", result.getMessage());
                response.put("removedColor", colorEntryToJson(result.getRemovedColor().getColor()));
                response.put("palette", buildPaletteResponse());

                Logger.info("Color removed: " + result.getMessage());
            } else {
                response.put("success", false);
                response.put("message", result.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Logger.info("Error removing color: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to remove color: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * STEP 3: Click on image to add a specific color to palette
     * POST /api/palette/add-from-image
     */
    @PostMapping("/add-from-image")
    public ResponseEntity<Map<String, Object>> addColorFromImage(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (currentPalette == null || currentImage == null) {
                response.put("success", false);
                response.put("message", "No image/palette loaded. Upload an image first.");
                return ResponseEntity.badRequest().body(response);
            }

            // Get coordinates and mode from request
            Integer x = (Integer) request.get("x");
            Integer y = (Integer) request.get("y");
            Boolean exactMode = request.get("exactMode") != null ?
                    (Boolean) request.get("exactMode") : false; // Default to region mode

            if (x == null || y == null) {
                response.put("success", false);
                response.put("message", "Missing x or y coordinates");
                return ResponseEntity.badRequest().body(response);
            }

            Logger.info(String.format("Adding color from click: (%d,%d) in %s mode",
                    x, y, exactMode ? "exact" : "region"));

            // Use RegionalColorExtractor to get the color at that point
            ColorEntry pickedColor;
            if (exactMode) {
                pickedColor = regionalColorExtractor.extractExactPixel(currentImage, x, y);
            } else {
                pickedColor = regionalColorExtractor.extractFromRegion(currentImage, x, y);
            }

            // Use PaletteManager to add this color to the palette
            PaletteManager.AddColorResult result = currentPalette.addPickedColor(pickedColor, exactMode);

            if (result.isSuccess()) {
                response.put("success", true);
                response.put("message", result.getMessage());
                response.put("addedColor", colorEntryToJson(pickedColor));
                response.put("coordinates", Map.of("x", x, "y", y));
                response.put("mode", exactMode ? "exact" : "region");
                response.put("palette", buildPaletteResponse());

                Logger.info("Color added to palette: " + result.getMessage());
            } else {
                // Color was rejected (similar color exists, palette full, etc.)
                response.put("success", false);
                response.put("message", result.getMessage());
                response.put("rejectedColor", colorEntryToJson(pickedColor));
                response.put("coordinates", Map.of("x", x, "y", y));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Logger.info("Error adding color from image: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to add color: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * STEP 4: Get current palette state
     * GET /api/palette/current
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentPalette() {
        Map<String, Object> response = new HashMap<>();

        if (currentPalette == null) {
            response.put("success", false);
            response.put("message", "No palette loaded");
            return ResponseEntity.ok(response);
        }

        response.put("success", true);
        response.put("palette", buildPaletteResponse());

        if (currentImage != null) {
            response.put("image", Map.of(
                    "name", currentImageName,
                    "width", currentImage.getWidth(),
                    "height", currentImage.getHeight()
            ));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * HELPER: Convert current palette to JSON format for frontend
     */
    private Map<String, Object> buildPaletteResponse() {
        if (currentPalette == null) return new HashMap<>();

        Map<String, Object> paletteData = new HashMap<>();

        // Basic palette info
        paletteData.put("size", currentPalette.getPaletteSize());
        paletteData.put("maxSize", currentPalette.getMaxPaletteSize());
        paletteData.put("isFull", currentPalette.isPaletteFull());
        paletteData.put("summary", currentPalette.getPaletteSummary());
        paletteData.put("imageName", currentPalette.getImageName());

        // Convert each palette color to JSON
        List<Map<String, Object>> colors = currentPalette.getPaletteColors().stream()
                .map(this::paletteColorToJson)
                .collect(Collectors.toList());

        paletteData.put("colors", colors);

        return paletteData;
    }

    /**
     * HELPER: Convert PaletteColor to JSON
     */
    private Map<String, Object> paletteColorToJson(PaletteManager.PaletteColor paletteColor) {
        Map<String, Object> colorJson = new HashMap<>();

        // Basic color info
        colorJson.putAll(colorEntryToJson(paletteColor.getColor()));

        // Palette-specific info
        colorJson.put("source", paletteColor.getSource().toString());
        colorJson.put("sourceDescription", paletteColor.getSourceDescription());
        colorJson.put("isUserPicked", paletteColor.isUserPicked());

        // Original dominant extraction data (if available)
        if (paletteColor.getOriginalPercentage() > 0) {
            colorJson.put("originalPercentage", Math.round(paletteColor.getOriginalPercentage() * 10) / 10.0);
            colorJson.put("originalPixelCount", paletteColor.getOriginalPixelCount());
        }

        return colorJson;
    }

    /**
     * HELPER: Convert ColorEntry to JSON
     */
    private Map<String, Object> colorEntryToJson(ColorEntry color) {
        Map<String, Object> colorJson = new HashMap<>();

        // Basic formats
        colorJson.put("hex", color.getHex());
        colorJson.put("rgb", Map.of(
                "red", color.getRed(),
                "green", color.getGreen(),
                "blue", color.getBlue()
        ));

        // HSL
        double[] hsl = color.getHSL();
        colorJson.put("hsl", Map.of(
                "hue", Math.round(hsl[0]),
                "saturation", Math.round(hsl[1]),
                "lightness", Math.round(hsl[2])
        ));

        // LAB (rounded for readability)
        double[] lab = color.getLAB();
        colorJson.put("lab", Map.of(
                "l", Math.round(lab[0] * 10) / 10.0,
                "a", Math.round(lab[1] * 10) / 10.0,
                "b", Math.round(lab[2] * 10) / 10.0
        ));

        return colorJson;
    }

    /**
     * UTILITY: Reset current session (useful for testing)
     * POST /api/palette/reset
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetSession() {
        currentImage = null;
        currentImageName = null;
        currentPalette = null;

        Logger.info("Palette session reset");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session reset successfully"
        ));
    }
}