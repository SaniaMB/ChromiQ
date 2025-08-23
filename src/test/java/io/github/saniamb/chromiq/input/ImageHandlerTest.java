package io.github.saniamb.chromiq.input;
import io.github.saniamb.chromiq.core.input.ImageInputHandler;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test class for ImageInputHandler
 * -------------------------------
 * Ensures that images can be loaded from files or streams,
 * resizing works correctly, and ARGB format is preserved.
 */
public class ImageHandlerTest {

   private final ImageInputHandler handler = new ImageInputHandler();

    /**
     * Test loading an image from a valid file path.
     */
    @Test
    void testLoadFromFile() throws IOException{
        // Create a temporary 100x100 PNG file
        File tempFile = File.createTempFile("test",".png");
        tempFile.deleteOnExit();

        BufferedImage dummy = new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(dummy,"png",tempFile);

        BufferedImage loaded = handler.loadFromFile(tempFile.getAbsolutePath());

        assertNotNull(loaded, "Loaded image should not be null");
        assertEquals(BufferedImage.TYPE_INT_ARGB, loaded.getType()," Image should be ARGB");
        assertEquals(100, loaded.getWidth());
        assertEquals(100,loaded.getHeight());
    }

    /**
     * Test loading an image from an InputStream.
     */
    @Test
    void testLoadFromStream() throws IOException{
        BufferedImage dummy = new BufferedImage(50, 50,BufferedImage.TYPE_INT_ARGB);

        // Convert image to a byte array
        File tempfile = File.createTempFile("testStream",".png");
        tempfile.deleteOnExit();
        ImageIO.write(dummy,"png",tempfile);

        byte[] bytes = Files.readAllBytes(tempfile.toPath());

        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        BufferedImage loaded = handler.loadFromStream(inputStream);

        assertNotNull(loaded);
        assertEquals(50, loaded.getWidth());
        assertEquals(50,loaded.getHeight());
    }


    /**
     * Test that resizing occurs when image exceeds MAX_WIDTH/MAX_HEIGHT.
     */
    @Test
    void testResizeLargeImage() throws  IOException{
        int largeWidth = 4000;
        int largeHeight = 3000;

        BufferedImage largeImage = new BufferedImage(largeWidth,largeHeight,BufferedImage.TYPE_INT_ARGB);

        BufferedImage processed = handler.loadFromFile(createTempPngFile(largeImage).getAbsolutePath());

        assertTrue(processed.getWidth() <= 1920, " Width should be resized to MAX_WIDTH");
        assertTrue(processed.getHeight() <= 1080, " Heigth should be resized to MAX_HEIGHT");
        assertEquals(BufferedImage.TYPE_INT_ARGB, processed.getType());
    }

    /**
     * Helper to create a temporary PNG file from a BufferedImage.
     */
    private File createTempPngFile(BufferedImage image) throws IOException {
        File tempFile = File.createTempFile("tempImage", ".png");
        tempFile.deleteOnExit();
        javax.imageio.ImageIO.write(image, "png", tempFile);
        return tempFile;
    }

    /**
     * Test that passing a null path throws IllegalArgumentException.
     */
    @Test
    void testLoadFromFile_nullPath() {
        assertThrows(IllegalArgumentException.class, () -> handler.loadFromFile(null));
        assertThrows(IllegalArgumentException.class, () -> handler.loadFromFile("   "));
    }

    /**
     * Test that passing a null path throws IllegalArgumentException.
     */
    @Test
    void testLoadFromStream_nullStream(){
        assertThrows(IllegalArgumentException.class, () -> handler.loadFromStream(null));
    }

    @Test
    void testGetImageInfo() throws IOException{
        BufferedImage dummy = new BufferedImage(20,30,BufferedImage.TYPE_INT_ARGB);

        String info = handler.getImageInfo(dummy);
        assertTrue(info.contains("20x30"));
        assertTrue(info.contains("ARGB"));
    }
}
