package imagefilter.filters;

import imagefilter.core.Filter;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Grayscale} filter.
 * Verifies that RGB values are converted to equal grayscale values,
 * and that image dimensions remain unchanged.
 */
public class GrayscaleTest {

    /**
     * Tests that the grayscale filter converts red and green pixels to gray.
     * Verifies R == G == B in the output and that the input image remains unchanged.
     */
    @Test
    public void testGrayscaleFilter() {
        // Create a 2×1 image: red and green
        BufferedImage input = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        input.setRGB(0, 0, 0xFF0000); // Red
        input.setRGB(1, 0, 0x00FF00); // Green

        // Copy for input immutability check
        BufferedImage inputCopy = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        inputCopy.setRGB(0, 0, input.getRGB(0, 0));
        inputCopy.setRGB(1, 0, input.getRGB(1, 0));

        Filter filter = new Grayscale();
        BufferedImage result = filter.apply(input);

        // Assert output dimensions match input
        assertEquals(input.getWidth(), result.getWidth(), "Width should remain unchanged");
        assertEquals(input.getHeight(), result.getHeight(), "Height should remain unchanged");

        // Assert pixels are grayscale (R == G == B)
        for (int x = 0; x < 2; x++) {
            int rgb = result.getRGB(x, 0);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            assertEquals(r, g, "Red and Green channels should match");
            assertEquals(g, b, "Green and Blue channels should match");
        }

        // Assert input image was not modified
        for (int x = 0; x < 2; x++) {
            assertEquals(inputCopy.getRGB(x, 0), input.getRGB(x, 0), "Input image should not be modified");
        }
    }
}
