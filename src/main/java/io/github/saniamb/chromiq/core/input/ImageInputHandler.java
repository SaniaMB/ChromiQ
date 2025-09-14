package io.github.saniamb.chromiq.core.input;
import io.github.saniamb.chromiq.core.utils.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static java.awt.RenderingHints.*;
import static java.awt.RenderingHints.KEY_INTERPOLATION;

/**
 * ImageInputHandler
 * -----------------
 * Handles image input from various sources such as local files or uploaded streams.
 * Converts all images into a standardized BufferedImage format optimized for
 * ChromiQ's color processing pipeline.
 * <p>
 * Key Features:
 * 1. Preserves alpha transparency for images.
 * 2. Resizes large images to prevent memory issues.
 * 3. Converts images to ARGB color format for consistent downstream processing.
 * <p>
 * Usage:
 * BufferedImage img = new ImageInputHandler().loadFromFile("example.png");
 * BufferedImage webImg = new ImageInputHandler().loadFromStream(inputStream);
 */
public class ImageInputHandler {

    static {
        javax.imageio.spi.IIORegistry registry = javax.imageio.spi.IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(new com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReaderSpi());
        registry.registerServiceProvider(new com.twelvemonkeys.imageio.plugins.bmp.BMPImageReaderSpi());
        registry.registerServiceProvider(new com.twelvemonkeys.imageio.plugins.psd.PSDImageReaderSpi());
        registry.registerServiceProvider(new com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi());
        System.out.println("✅ TwelveMonkeys readers registered: JPEG, BMP, PSD, TIFF");
    }

    // Maximum allowed dimensions to prevent memory issues for very large images
    private final int MAX_WIDTH = 1920;
    private final int MAX_HEIGHT = 1080;


    /**
     * Loads an image from a file path and prepares it for color processing.
     *
     * @param filePath Path to the image file
     * @return Processed BufferedImage (ARGB, resized if necessary)
     * @throws IOException if a file does not exist or cannot be read
     */
    public BufferedImage loadFromFile(String filePath) throws IOException{
        if(filePath == null || filePath.trim().isEmpty()){
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        File imageFile = new File(filePath);
        if(!imageFile.exists()){
            throw new IOException("Image file does not exist: "+ filePath);
        }

        Logger.info("Available formats: " + String.join(", ", ImageIO.getReaderFormatNames()));

        // Load the image from disk
        BufferedImage image = ImageIO.read(imageFile);
        if(image == null){
            throw new IOException("Unsupported image format or corrupted image file: "+ filePath);
        }

        Logger.info("Loaded image from: " + filePath);

        // Process image: resize if too large, ensure ARGB
        return processImage(image);
    }


    /**
     * Loads an image from an InputStream (e.g., web uploads) and prepares it
     * for color processing.
     *
     * @param inputStream InputStream containing image data
     * @return Processed BufferedImage (ARGB, resized if necessary)
     * @throws IOException if stream is null, unsupported, or corrupted
     */
    public BufferedImage loadFromStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null or empty");
        }

        // Wrap in BufferedInputStream to support mark/reset
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        bis.mark(Integer.MAX_VALUE); // mark the beginning

        Logger.info("Available formats: " + String.join(", ", ImageIO.getReaderFormatNames()));

        // Try ImageIO.read directly first
        BufferedImage image = ImageIO.read(bis);
        if (image == null) {
            // Reset and try ImageInputStream (more reliable with TwelveMonkeys)
            bis.reset();
            try (javax.imageio.stream.ImageInputStream iis = ImageIO.createImageInputStream(bis)) {
                image = ImageIO.read(iis);
            }
        }

        if (image == null) {
            throw new IOException("Unsupported image format or corrupted input stream");
        }

        Logger.info("Loaded image from InputStream: " + image.getWidth() + "x" + image.getHeight());

        // Process image: resize if too large, ensure ARGB
        return processImage(image);
    }



    /**
     * Prepares a BufferedImage for downstream color analysis.
     * 1. Resizes large images to fit MAX_WIDTH/MAX_HEIGHT.
     * 2. Converts image to ARGB to preserve alpha/transparency.
     *
     * @param original Original BufferedImage
     * @return Processed BufferedImage ready for ChromiQ color pipeline
     */
    private BufferedImage processImage(BufferedImage original){
        BufferedImage processed = original;

        //Step 1: Resize if too large
        if(needsResizing(original)){
            processed = resizeImage(original);
            Logger.info("Resized image to " + processed.getWidth()+ "x" + processed.getHeight());
        }

        // Step 2: Convert to ARGB for consistent color extraction
        if(processed.getType() != BufferedImage.TYPE_INT_ARGB){
            processed = convertToARGB(processed);
            Logger.info("Converted image to ARGB format");
        }
        return processed;
    }


    /**
     * Determines if an image exceeds the maximum allowed dimensions.
     *
     * @param image BufferedImage to check
     * @return true if resizing is needed
    */
    private boolean needsResizing(BufferedImage image){
        return image.getWidth() > MAX_WIDTH || image.getHeight() > MAX_HEIGHT;
    }

    /**
     * Resizes a BufferedImage while maintaining its aspect ratio.
     * Uses high-quality rendering hints for smooth results.
     *
     * @param original Original BufferedImage
     * @return Resized BufferedImage
     */
    private BufferedImage resizeImage(BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // Compute a scaling factor to fit within max bounds
        double scaleX = (double) MAX_WIDTH / originalWidth;
        double scaleY = (double) MAX_HEIGHT / originalHeight;
        double scale = Math.min(scaleX, scaleY);

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // Create a new ARGB image for a resized result
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();

        // High-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the original image into resized canvas
        g2d.drawImage(original,0,0, newWidth, newHeight,null);
        g2d.dispose();

       return resized;
    }

    /**
     * Converts any BufferedImage to ARGB format to preserve transparency.
     *
     * @param original Original BufferedImage
     * @return ARGB BufferedImage
     */
    private BufferedImage convertToARGB(BufferedImage original){
        BufferedImage argbImage = new BufferedImage(original.getWidth(),original.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D gd2 = argbImage.createGraphics();
        gd2.drawImage(original,0,0,null);
        gd2.dispose();

        return argbImage;
    }

    /**
     * Returns basic image info for debugging or logging purposes.
     *
     * @param image BufferedImage to inspect
     * @return String describing image dimensions and type
     */
    public String getImageInfo(BufferedImage image){
        if(image == null) return "No image loaded";
        return String.format("Image: %dx%d pixels, Type: %s", image.getWidth(), image.getHeight(), getImageTypeString(image.getType()));
    }

    /**
     * Converts BufferedImage type constant to human-readable string.
     */
    private String getImageTypeString(int type){
        return switch (type){
            case BufferedImage.TYPE_INT_RGB -> "RGB";
            case BufferedImage.TYPE_INT_ARGB -> "ARGB";
            case BufferedImage.TYPE_BYTE_GRAY -> "Grayscale";
            default -> "Type" + type;
        };
    }
}
