package imagefilter.filters;

import imagefilter.core.Filter;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Invert} filter.
 * Validates color inversion, dimension preservation, and input immutability.
 */
public class InvertTest {

    /**
     * Verifies that black is inverted to white.
     */
    @Test
    public void testBlackBecomesWhite() {
        BufferedImage input = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        input.setRGB(0, 0, 0x000000); // Black

        Filter filter = new Invert();
        BufferedImage result = filter.apply(input);

        assertEquals(255, (result.getRGB(0, 0) >> 16) & 0xFF, "Red should be 255");
        assertEquals(255, (result.getRGB(0, 0) >> 8) & 0xFF, "Green should be 255");
        assertEquals(255, result.getRGB(0, 0) & 0xFF, "Blue should be 255");

        assertEquals(input.getWidth(), result.getWidth(), "Width should match");
        assertEquals(input.getHeight(), result.getHeight(), "Height should match");
    }

    /**
     * Verifies that white is inverted to black.
     */
    @Test
    public void testWhiteBecomesBlack() {
        BufferedImage input = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        input.setRGB(0, 0, 0xFFFFFF); // White

        Filter filter = new Invert();
        BufferedImage result = filter.apply(input);

        assertEquals(0, (result.getRGB(0, 0) >> 16) & 0xFF, "Red should be 0");
        assertEquals(0, (result.getRGB(0, 0) >> 8) & 0xFF, "Green should be 0");
        assertEquals(0, result.getRGB(0, 0) & 0xFF, "Blue should be 0");
    }

    /**
     * Ensures that the input image is not modified.
     */
    @Test
    public void testInputImageUnchanged() {
        BufferedImage input = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        int originalRGB = 0x123456;
        input.setRGB(0, 0, originalRGB);

        BufferedImage inputCopy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        inputCopy.setRGB(0, 0, originalRGB);

        Filter filter = new Invert();
        filter.apply(input);

        assertEquals(inputCopy.getRGB(0, 0), input.getRGB(0, 0), "Input image should not be modified");
    }
}
