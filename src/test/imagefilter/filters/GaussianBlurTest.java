package imagefilter.filters;

import imagefilter.core.Filter;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GaussianBlur}.
 * Validates that the blur:
 * - smooths edges,
 * - preserves uniform regions,
 * - maintains image dimensions,
 * - uses a normalized kernel.
 */
public class GaussianBlurTest {

    /**
     * Creates a 3×3 image with only the center pixel set to white.
     * Verifies that blur spreads brightness from the center.
     */
    @Test
    public void testBlurSpreadsCenterPixel() {
        BufferedImage input = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        input.setRGB(1, 1, Color.WHITE.getRGB());

        Filter blur = new GaussianBlur();
        BufferedImage result = blur.apply(input);

        int centerRed = new Color(result.getRGB(1, 1)).getRed();
        int cornerRed = new Color(result.getRGB(0, 0)).getRed();

        assertTrue(centerRed < 255, "Center pixel should be softened (less than full white)");
        assertTrue(cornerRed > 0, "Corner pixel should receive some brightness from blur");
    }

    /**
     * Verifies that a solid color image remains unchanged after blur.
     */
    @Test
    public void testUniformColorRemainsSame() {
        BufferedImage input = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                input.setRGB(x, y, Color.WHITE.getRGB());
            }
        }

        Filter blur = new GaussianBlur();
        BufferedImage result = blur.apply(input);

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                assertEquals(Color.WHITE.getRGB(), result.getRGB(x, y),
                        "Pixel at (" + x + "," + y + ") should remain white");
            }
        }
    }

    /**
     * Confirms that image dimensions are unchanged after applying blur.
     */
    @Test
    public void testImageSizeUnchanged() {
        BufferedImage input = new BufferedImage(10, 8, BufferedImage.TYPE_INT_RGB);
        Filter blur = new GaussianBlur();
        BufferedImage result = blur.apply(input);

        assertEquals(10, result.getWidth(), "Image width should remain unchanged");
        assertEquals(8, result.getHeight(), "Image height should remain unchanged");
    }

    /**
     * Applies blur to an image with a sharp black-white-black transition.
     * Verifies that center pixel becomes a blend.
     */
    @Test
    public void testSharpEdgeIsBlurred() {
        BufferedImage input = new BufferedImage(3, 1, BufferedImage.TYPE_INT_RGB);
        input.setRGB(0, 0, Color.BLACK.getRGB());
        input.setRGB(1, 0, Color.WHITE.getRGB());
        input.setRGB(2, 0, Color.BLACK.getRGB());

        Filter blur = new GaussianBlur();
        BufferedImage result = blur.apply(input);

        int red = new Color(result.getRGB(1, 0)).getRed();
        assertTrue(red > 0 && red < 255, "Center pixel should be partially blurred (not black or white)");
    }

    /**
     * Verifies that the Gaussian kernel used by the filter is normalized.
     * Uses reflection to access the private kernel field.
     */
    @Test
    public void testKernelNormalization() throws Exception {
        GaussianBlur blur = new GaussianBlur();

        // Access private 'kernel' field via reflection
        var field = GaussianBlur.class.getDeclaredField("kernel");
        field.setAccessible(true);
        double[][] kernel = (double[][]) field.get(blur);

        double sum = 0.0;
        for (double[] row : kernel) {
            for (double value : row) {
                sum += value;
            }
        }

        assertEquals(1.0, sum, 0.0001, "Kernel values should sum approximately to 1.0");
    }
}
