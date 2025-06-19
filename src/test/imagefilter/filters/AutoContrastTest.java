package imagefilter.filters;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link AutoContrast} filter.
 * These tests verify that contrast is correctly stretched based on luminance,
 * and that edge cases are handled as expected.
 */
public class AutoContrastTest {

    /**
     * Verifies that the filter brightens a dark image with varying pixel values.
     * Ensures contrast stretching is applied when possible.
     */
    @Test
    public void testDarkImageBrightened() {
        // Create a 2x1 image with two different dark shades
        BufferedImage img = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, new Color(10, 10, 10).getRGB());
        img.setRGB(1, 0, new Color(50, 50, 50).getRGB());

        AutoContrast filter = new AutoContrast();
        BufferedImage result = filter.apply(img);

        // Extract transformed pixels
        Color out0 = new Color(result.getRGB(0, 0));
        Color out1 = new Color(result.getRGB(1, 0));

        // Check that at least one channel increased in brightness
        boolean brighter = out0.getRed() > 10 || out1.getRed() > 50;
        assertTrue(brighter, "Dark image should be brightened by contrast stretching");
    }

    /**
     * Verifies that after calling apply(), transformPixel() properly maps a known value.
     * For a pixel with value 10 in an image with luminance range 10–240, the result should be 0 (±1).
     */
    @Test
    public void testTransformPixelAfterApply() {
        // Create a 2x1 image with known luminance bounds
        BufferedImage img = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, new Color(10, 10, 10).getRGB());      // Dark pixel
        img.setRGB(1, 0, new Color(240, 240, 240).getRGB());   // Bright pixel

        AutoContrast filter = new AutoContrast();
        filter.apply(img); // Compute contrast bounds

        // Apply transformPixel() to a pixel with the same value as the minimum
        Color transformed = filter.transformPixel(new Color(10, 10, 10));

        // Verify that all channels are stretched to approximately 0
        int[] channels = {
                transformed.getRed(),
                transformed.getGreen(),
                transformed.getBlue()
        };

        for (int c : channels) {
            assertTrue(c <= 1, "Expected stretched channel to be 0 (±1), got: " + c);
        }
    }

    /**
     * Ensures that if the input image is flat (all pixels identical), no contrast stretching is applied.
     * The output image should remain unchanged.
     */
    @Test
    public void testFlatImageReturnsSame() {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Color originalColor = new Color(128, 128, 128);

        // Fill the image with a constant color
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                img.setRGB(x, y, originalColor.getRGB());
            }
        }

        AutoContrast filter = new AutoContrast();
        BufferedImage result = filter.apply(img);

        // Expect all output pixels to be identical to the input
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                assertEquals(originalColor.getRGB(), result.getRGB(x, y),
                        "Flat image should remain unchanged at (" + x + ", " + y + ")");
            }
        }
    }
}
