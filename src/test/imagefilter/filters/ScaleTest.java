package imagefilter.filters;

import imagefilter.core.Filter;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Scale} filter.
 * Tests correctness of upscaling, downscaling, constructor behavior,
 * option parsing, and exception safety.
 */
public class ScaleTest {

    /**
     * Creates a 2x2 test image with distinct colors in each pixel.
     * <p>
     * Layout:
     * [RED,   GREEN]
     * [BLUE,  YELLOW]
     * <p>
     * @return BufferedImage test image with distinct corners
     */
    private BufferedImage createTestImage() {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, Color.RED.getRGB());     // Top-left
        img.setRGB(1, 0, Color.GREEN.getRGB());   // Top-right
        img.setRGB(0, 1, Color.BLUE.getRGB());    // Bottom-left
        img.setRGB(1, 1, Color.YELLOW.getRGB());  // Bottom-right
        return img;
    }

    /**
     * Verifies that scaling by a factor of 2 results in a 4x4 image
     * and that color replication behaves correctly for top-left pixel.
     */
    @Test
    public void testScaleUp2x() {
        BufferedImage input = createTestImage();
        Filter scale = new Scale(2.0);
        BufferedImage result = scale.apply(input);

        // Dimensions should double
        assertEquals(4, result.getWidth(), "Expected width to double");
        assertEquals(4, result.getHeight(), "Expected height to double");

        // Top-left 2x2 block should replicate RED
        assertEquals(Color.RED.getRGB(), result.getRGB(0, 0));
        assertEquals(Color.RED.getRGB(), result.getRGB(1, 0));
        assertEquals(Color.RED.getRGB(), result.getRGB(0, 1));
        assertEquals(Color.RED.getRGB(), result.getRGB(1, 1));
    }

    /**
     * Verifies that downscaling to 1x1 results in a single pixel
     * and that the result matches the top-left of the input.
     */
    @Test
    public void testScaleDownTo1x1() {
        BufferedImage input = createTestImage();
        Filter scale = new Scale(0.4); // 2x2 scaled by 0.4 becomes 1x1
        BufferedImage result = scale.apply(input);

        assertEquals(1, result.getWidth());
        assertEquals(1, result.getHeight());

        // Should match top-left pixel (RED)
        assertEquals(Color.RED.getRGB(), result.getRGB(0, 0));
    }

    /**
     * Ensures that a filter can be instantiated via options map
     * with a valid string scale factor.
     */
    @Test
    public void testFromOptionsValid() {
        Map<String, String> opts = Collections.singletonMap("factor", "1.5");
        Filter scale = Scale.fromOptions(opts);
        assertEquals("scale", scale.name());
    }

    /**
     * Ensures that non-numeric values for 'factor' option throw an exception.
     */
    @Test
    public void testFromOptionsInvalidStringThrows() {
        Map<String, String> opts = Collections.singletonMap("factor", "not-a-number");
        assertThrows(IllegalArgumentException.class, () -> Scale.fromOptions(opts));
    }

    /**
     * Ensures that very small scale factors are rejected during option parsing.
     */
    @Test
    public void testFromOptionsTooSmallThrows() {
        Map<String, String> opts = Collections.singletonMap("factor", "0.00001");
        assertThrows(IllegalArgumentException.class, () -> Scale.fromOptions(opts));
    }

    /**
     * Verifies that the constructor throws when given a zero scale.
     */
    @Test
    public void testConstructorRejectsZeroScale() {
        assertThrows(IllegalArgumentException.class, () -> new Scale(0.0));
    }

    /**
     * Verifies that the constructor throws when given a negative scale.
     */
    @Test
    public void testConstructorRejectsNegativeScale() {
        assertThrows(IllegalArgumentException.class, () -> new Scale(-1.0));
    }

    /**
     * Ensures that transformPixel methods are unsupported and throw as expected.
     */
    @Test
    public void testTransformPixelMethodsThrow() {
        Filter scale = new Scale(1.0);
        assertThrows(UnsupportedOperationException.class, () -> scale.transformPixel(Color.BLACK));

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        assertThrows(UnsupportedOperationException.class, () -> scale.transformPixel(img, 0, 0));
    }

    /**
     * Ensures that scaling does not throw IndexOutOfBoundsException for larger outputs.
     */
    @Test
    public void testNoOutOfBoundsScaling() {
        BufferedImage input = createTestImage();
        Filter scale = new Scale(3.0);
        BufferedImage result = scale.apply(input);

        // Expect successful scale to 6x6 image
        assertNotNull(result);
        assertEquals(6, result.getWidth());
        assertEquals(6, result.getHeight());
    }
}
