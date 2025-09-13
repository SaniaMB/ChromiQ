package io.github.saniamb.chromiq.core.api;

import io.github.saniamb.chromiq.core.color.DominantColorExtractor;
import io.github.saniamb.chromiq.core.color.RegionalColorExtractor;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.input.ImageInputHandler;
import io.github.saniamb.chromiq.core.palette.PaletteManager;
import io.github.saniamb.chromiq.core.utils.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/palette")
@CrossOrigin(origins = "*")
public class PaletteWorkflowController {

    private final ImageInputHandler imageInputHandler;
    private final DominantColorExtractor dominantColorExtractor;
    private final RegionalColorExtractor regionalColorExtractor;

    @Autowired
    private UserSessionManager userSessionManager;

    public PaletteWorkflowController() {
        this.imageInputHandler = new ImageInputHandler();
        this.dominantColorExtractor = new DominantColorExtractor();
        this.regionalColorExtractor = new RegionalColorExtractor();
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAndGeneratePalette(@RequestParam("image") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select an image file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            Logger.info("Starting palette workflow for: " + file.getOriginalFilename());
            BufferedImage currentImage = imageInputHandler.loadFromStream(file.getInputStream());
            userSessionManager.setCurrentImage(currentImage);
            String currentImageName = file.getOriginalFilename();
            Logger.info("Image processed: " + imageInputHandler.getImageInfo(currentImage));

            List<DominantColorExtractor.DominantColor> dominantColors =
                    dominantColorExtractor.extractDominantColors(currentImage, 10);

            PaletteManager currentPalette = new PaletteManager(dominantColors);
            currentPalette.setImageName(currentImageName);
            userSessionManager.setPaletteManager(currentPalette);

            response.put("success", true);
            response.put("message", "Image uploaded and palette generated successfully");
            response.put("imageWidth", currentImage.getWidth());
            response.put("imageHeight", currentImage.getHeight());
            response.put("palette", buildPaletteResponse());

            Logger.info("Initial palette generated: " + currentPalette.getPaletteSummary());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Logger.info("Error in palette workflow: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to process image and generate palette: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/colors/{index}")
    public ResponseEntity<Map<String, Object>> removeColor(@PathVariable int index) {
        Map<String, Object> response = new HashMap<>();
        try {
            PaletteManager currentPalette = userSessionManager.getPaletteManager();
            if (currentPalette == null) {
                response.put("success", false);
                response.put("message", "No palette loaded. Upload an image first.");
                return ResponseEntity.badRequest().body(response);
            }

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

    @PostMapping("/add-from-image")
    public ResponseEntity<Map<String, Object>> addColorFromImage(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            PaletteManager currentPalette = userSessionManager.getPaletteManager();
            BufferedImage currentImage = userSessionManager.getCurrentImage();
            if (currentPalette == null || currentImage == null) {
                response.put("success", false);
                response.put("message", "No image/palette loaded. Upload an image first.");
                return ResponseEntity.badRequest().body(response);
            }

            Integer x = (Integer) request.get("x");
            Integer y = (Integer) request.get("y");
            Boolean exactMode = request.get("exactMode") != null ? (Boolean) request.get("exactMode") : false;

            if (x == null || y == null) {
                response.put("success", false);
                response.put("message", "Missing x or y coordinates");
                return ResponseEntity.badRequest().body(response);
            }

            Logger.info(String.format("Adding color from click: (%d,%d) in %s mode", x, y, exactMode ? "exact" : "region"));
            ColorEntry pickedColor = exactMode ? regionalColorExtractor.extractExactPixel(currentImage, x, y) : regionalColorExtractor.extractFromRegion(currentImage, x, y);
            PaletteManager.AddColorResult result = currentPalette.addPickedColor(pickedColor, exactMode);

            if (result.isSuccess()) {
                response.put("success", true);
                response.put("message", result.getMessage());
                response.put("addedColor", colorEntryToJson(pickedColor));
                response.put("palette", buildPaletteResponse());
                Logger.info("Color added to palette: " + result.getMessage());
            } else {
                response.put("success", false);
                response.put("message", result.getMessage());
                response.put("rejectedColor", colorEntryToJson(pickedColor));
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Logger.info("Error adding color from image: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to add color: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/colors/{index}")
    public ResponseEntity<Map<String, Object>> replaceColor(@PathVariable int index, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            PaletteManager currentPalette = userSessionManager.getPaletteManager();
            BufferedImage currentImage = userSessionManager.getCurrentImage();
            if (currentPalette == null || currentImage == null) {
                response.put("success", false);
                response.put("message", "No image/palette loaded. Upload an image first.");
                return ResponseEntity.badRequest().body(response);
            }

            Integer x = (Integer) request.get("x");
            Integer y = (Integer) request.get("y");
            Boolean exactMode = request.get("exactMode") != null ? (Boolean) request.get("exactMode") : false;

            if (x == null || y == null) {
                response.put("success", false);
                response.put("message", "Missing x or y coordinates");
                return ResponseEntity.badRequest().body(response);
            }

            ColorEntry pickedColor = exactMode ? regionalColorExtractor.extractExactPixel(currentImage, x, y) : regionalColorExtractor.extractFromRegion(currentImage, x, y);
            PaletteManager.ReplaceColorResult result = currentPalette.replaceColor(index, pickedColor, exactMode);

            if (result.isSuccess()) {
                response.put("success", true);
                response.put("message", result.getMessage());
                response.put("palette", buildPaletteResponse());
            } else {
                response.put("success", false);
                response.put("message", result.getMessage());
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Logger.info("Error replacing color: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to replace color: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentPalette() {
        Map<String, Object> response = new HashMap<>();
        PaletteManager currentPalette = userSessionManager.getPaletteManager();
        if (currentPalette == null) {
            response.put("success", false);
            response.put("message", "No palette loaded");
            return ResponseEntity.ok(response);
        }
        response.put("success", true);
        response.put("palette", buildPaletteResponse());
        BufferedImage currentImage = userSessionManager.getCurrentImage();
        if (currentImage != null) {
            response.put("image", Map.of("name", currentPalette.getImageName(), "width", currentImage.getWidth(), "height", currentImage.getHeight()));
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetSession() {
        userSessionManager.setCurrentImage(null);
        userSessionManager.setPaletteManager(null);
        Logger.info("Palette session reset");
        return ResponseEntity.ok(Map.of("success", true, "message", "Session reset successfully"));
    }

    // --- HELPER METHODS ---

    private Map<String, Object> buildPaletteResponse() {
        PaletteManager currentPalette = userSessionManager.getPaletteManager();
        if (currentPalette == null) return new HashMap<>();
        Map<String, Object> paletteData = new HashMap<>();
        paletteData.put("size", currentPalette.getPaletteSize());
        paletteData.put("maxSize", currentPalette.getMaxPaletteSize());
        paletteData.put("isFull", currentPalette.isPaletteFull());
        paletteData.put("summary", currentPalette.getPaletteSummary());
        paletteData.put("imageName", currentPalette.getImageName());
        List<Map<String, Object>> colors = currentPalette.getPaletteColors().stream().map(this::paletteColorToJson).collect(Collectors.toList());
        paletteData.put("colors", colors);
        return paletteData;
    }

    private Map<String, Object> paletteColorToJson(PaletteManager.PaletteColor paletteColor) {
        Map<String, Object> colorJson = new HashMap<>(colorEntryToJson(paletteColor.getColor()));
        colorJson.put("source", paletteColor.getSource().toString());
        colorJson.put("sourceDescription", paletteColor.getSourceDescription());
        colorJson.put("isUserPicked", paletteColor.isUserPicked());
        if (paletteColor.getOriginalPercentage() > 0) {
            colorJson.put("originalPercentage", Math.round(paletteColor.getOriginalPercentage() * 10) / 10.0);
            colorJson.put("originalPixelCount", paletteColor.getOriginalPixelCount());
        }
        return colorJson;
    }

    private Map<String, Object> colorEntryToJson(ColorEntry color) {
        Map<String, Object> colorJson = new HashMap<>();
        colorJson.put("hex", color.getHex());
        colorJson.put("rgb", Map.of("red", color.getRed(), "green", color.getGreen(), "blue", color.getBlue()));
        double[] hsl = color.getHSL();
        colorJson.put("hsl", Map.of("hue", Math.round(hsl[0]), "saturation", Math.round(hsl[1]), "lightness", Math.round(hsl[2])));
        double[] lab = color.getLAB();
        colorJson.put("lab", Map.of("l", Math.round(lab[0] * 10) / 10.0, "a", Math.round(lab[1] * 10) / 10.0, "b", Math.round(lab[2] * 10) / 10.0));
        return colorJson;
    }
}