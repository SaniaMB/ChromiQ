package io.github.saniamb.chromiq.integration;

import io.github.saniamb.chromiq.core.color.ColorClusterer;
import io.github.saniamb.chromiq.core.color.ColorExtractor;
import io.github.saniamb.chromiq.core.color.models.ColorEntry;
import io.github.saniamb.chromiq.core.input.ImageInputHandler;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class ColorClustererIT {

    private static final int MAX_CLUSTERS = 10; // Desired number of clusters

    public static void main(String[] args) {
        System.out.println("🎨 ColorClusterer 10-Cluster Test\n");

        String[] imagePaths = {
                "src/test/resources/test-images/gradient.jpeg",
                "src/test/resources/test-images/photo.jpeg",
                "src/test/resources/test-images/solid-blue.png",
                "src/test/resources/test-images/transparent.jpg"
        };

        ImageInputHandler imageHandler = new ImageInputHandler();
        ColorExtractor colorExtractor = new ColorExtractor();
        ColorClusterer colorClusterer = new ColorClusterer();

        for (String imagePath : imagePaths) {
            System.out.println("🖼️ Processing: " + new File(imagePath).getName());
            System.out.println("─".repeat(50));

            processImage(imagePath, imageHandler, colorExtractor, colorClusterer);
            System.out.println();
        }
    }

    private static void processImage(String imagePath,
                                     ImageInputHandler imageHandler,
                                     ColorExtractor colorExtractor,
                                     ColorClusterer colorClusterer) {
        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                System.err.println("❌ File not found: " + imagePath);
                return;
            }

            BufferedImage image = imageHandler.loadFromFile(imagePath);
            List<ColorExtractor.ColorCount> extractedColors = colorExtractor.extractColors(image);

            if (extractedColors.isEmpty()) {
                System.err.println("❌ No colors found in image");
                return;
            }

            // Determine k safely
            int k = Math.min(MAX_CLUSTERS, extractedColors.size());

            List<ColorClusterer.ColorCluster> clusters = colorClusterer.clusterColors(extractedColors, k);

            // Print clusters 1–10, fill missing with "-"
            System.out.println("📊 Cluster centroids (hex):");
            for (int i = 0; i < MAX_CLUSTERS; i++) {
                if (i < clusters.size()) {
                    ColorEntry center = clusters.get(i).getCenterColor();
                    System.out.println(String.format("%2d: %s", i + 1, center.getHex()));
                } else {
                    System.out.println(String.format("%2d: –", i + 1));
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing image: " + e.getMessage());
        }
    }
}
