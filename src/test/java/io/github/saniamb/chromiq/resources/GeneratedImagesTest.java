package io.github.saniamb.chromiq.resources;

import io.github.saniamb.chromiq.core.input.ImageInputHandler;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratedImagesTest {
    @Test
    void testLoadFromFile() throws IOException {
        // Load an existing PNG from test resources
        String imagePath = getClass().getResource("/images/test-image.jpg").getPath();

        ImageInputHandler handler = new ImageInputHandler();
        BufferedImage loaded = handler.loadFromFile(imagePath);

        assertNotNull(loaded, "Loaded image should not be null");
        assertEquals(BufferedImage.TYPE_INT_ARGB, loaded.getType(), "Image should be ARGB");
       // assertEquals(200, loaded.getWidth(), "Image width should be 100");
       // assertEquals(200, loaded.getHeight(), "Image height should be 100");
        assertTrue(loaded.getWidth() <= 1920, " Width should be resized to MAX_WIDTH");
        assertTrue(loaded.getHeight() <= 1080, " Heigth should be resized to MAX_HEIGHT");
    }

}
