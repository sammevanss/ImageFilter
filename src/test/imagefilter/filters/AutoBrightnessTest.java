package imagefilter.filters;

import imagefilter.core.Filter;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link AutoBrightness} filter.
 * These tests verify brightness correction behavior and image integrity.
 */
public class AutoBrightnessTest {

    /**
     * Creates a solid color image for testing.
     *
     * @param color  the color to fill the image with
     * @param width  image width
     * @param height image height
     * @return a new BufferedImage of the given color
     */
    private BufferedImage createSolidImage(Color color, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                img.setRGB(x, y, color.getRGB());
            }
        }
        return img;
    }

    /**
     * Verifies that the output image is not null and has the same dimensions as the input.
     */
    @Test
    public void testOutputImageIsNotNullAndSameSize() {
        BufferedImage input = createSolidImage(Color.DARK_GRAY, 10, 10);
        Filter filter = new AutoBrightness();
        BufferedImage output = filter.apply(input);

        assertNotNull(output, "Output image should not be null");
        assertEquals(input.getWidth(), output.getWidth(), "Output width should match input");
        assertEquals(input.getHeight(), output.getHeight(), "Output height should match input");
    }

    /**
     * Verifies that bright images are darkened by the filter.
     */
    @Test
    public void testBrightImageIsDarkened() {
        BufferedImage input = createSolidImage(Color.WHITE, 5, 5);
        Filter filter = new AutoBrightness();
        BufferedImage output = filter.apply(input);

        Color resultColor = new Color(output.getRGB(0, 0));
        assertTrue(resultColor.getRed() < 255, "Red should be reduced from 255");
        assertTrue(resultColor.getGreen() < 255, "Green should be reduced from 255");
        assertTrue(resultColor.getBlue() < 255, "Blue should be reduced from 255");
    }

    /**
     * Verifies that dark images are brightened by the filter.
     */
    @Test
    public void testDarkImageIsBrightened() {
        AutoBrightness filter = new AutoBrightness();

        // Create a 2×2 very dark gray image (not pure black)
        BufferedImage input = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                input.setRGB(x, y, new Color(10, 10, 10).getRGB());
            }
        }

        BufferedImage output = filter.apply(input);

        // Extract RGB channels
        int rgb = output.getRGB(0, 0);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        // Expect at least one channel to increase from the original value (10)
        boolean brightened = r > 10 || g > 10 || b > 10;
        assertTrue(brightened, "Expected at least one channel to increase above 10");
    }

    /**
     * Verifies that the filter works correctly on a single pixel image.
     */
    @Test
    public void testSinglePixelImage() {
        BufferedImage input = createSolidImage(new Color(100, 150, 200), 1, 1);
        Filter filter = new AutoBrightness();
        BufferedImage output = filter.apply(input);

        assertEquals(1, output.getWidth(), "Width should be 1");
        assertEquals(1, output.getHeight(), "Height should be 1");
    }

    /**
     * Ensures that the input image remains unchanged after applying the filter.
     */
    @Test
    public void testInputImageUnchanged() {
        BufferedImage input = createSolidImage(new Color(50, 100, 150), 2, 2);

        // Make a copy of the original image for later comparison
        BufferedImage copy = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                copy.setRGB(x, y, input.getRGB(x, y));
            }
        }

        Filter filter = new AutoBrightness();
        filter.apply(input);  // Discard output; only interested in input image

        // Verify that original pixels remain unchanged
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                assertEquals(copy.getRGB(x, y), input.getRGB(x, y),
                        "Input image should remain unchanged at (" + x + ", " + y + ")");
            }
        }
    }
}
