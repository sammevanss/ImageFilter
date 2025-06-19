package imagefilter.core;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Filter} interface's default methods.
 * These tests verify fallback behavior between the two transformPixel methods
 * and proper exception handling.
 */
public class FilterInterfaceTest {

    /**
     * Filter that only overrides transformPixel(Color).
     * Used to test fallback when the image-aware transform is not implemented.
     */
    static class ColorOnlyFilter implements Filter {
        @Override
        public Color transformPixel(Color inputColor) {
            return Color.WHITE; // Always return white
        }

        @Override
        public String name() {
            return "coloronly";
        }
    }

    /**
     * Filter that only overrides transformPixel(BufferedImage, x, y).
     * Used to test context-aware behavior.
     */
    static class ImageContextOnlyFilter implements Filter {
        @Override
        public Color transformPixel(BufferedImage image, int x, int y) {
            return new Color(0, 255, 0); // Always return green
        }

        @Override
        public String name() {
            return "imagecontext";
        }
    }

    /**
     * Filter that overrides neither transformPixel method.
     * Used to test that an exception is thrown when apply() is called.
     */
    static class InvalidFilter implements Filter {
        @Override
        public String name() {
            return "invalid";
        }
    }

    /**
     * Filter that returns the input color unmodified.
     * Used to validate color preservation.
     */
    static class IdentityFilter implements Filter {
        @Override
        public Color transformPixel(Color inputColor) {
            return inputColor; // No-op transformation
        }

        @Override
        public String name() {
            return "identity";
        }
    }

    /**
     * Tests that apply() uses transformPixel(Color) when only that is implemented.
     */
    @Test
    public void testApplyWithColorOnlyTransform() {
        BufferedImage input = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        input.setRGB(0, 0, Color.BLUE.getRGB()); // Original pixel

        Filter filter = new ColorOnlyFilter();
        BufferedImage result = filter.apply(input); // Should fall back to transformPixel(Color)

        assertEquals(Color.WHITE.getRGB(), result.getRGB(0, 0),
                "Expected white from color-only fallback");
    }

    /**
     * Tests that apply() uses transformPixel(image, x, y) when implemented.
     */
    @Test
    public void testApplyWithImageContextTransform() {
        BufferedImage input = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        input.setRGB(0, 0, Color.RED.getRGB()); // Input pixel color should be ignored

        Filter filter = new ImageContextOnlyFilter();
        BufferedImage result = filter.apply(input);

        assertEquals(new Color(0, 255, 0).getRGB(), result.getRGB(0, 0),
                "Expected green from context-aware transform");
    }

    /**
     * Tests that apply() throws an exception when no transformPixel method is implemented.
     */
    @Test
    public void testApplyWithNoTransformImplementedThrows() {
        BufferedImage input = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        input.setRGB(0, 0, Color.CYAN.getRGB());

        Filter filter = new InvalidFilter();

        assertThrows(IllegalStateException.class, () -> filter.apply(input),
                "Expected IllegalStateException when no transform methods are implemented");
    }

    /**
     * Verifies that transformPixel(Color) preserves the original pixel if it returns the same value.
     */
    @Test
    public void testApplyPreservesColorWhenTransformIsIdentity() {
        BufferedImage input = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Color original = new Color(123, 200, 50);
        input.setRGB(0, 0, original.getRGB());

        Filter filter = new IdentityFilter();
        BufferedImage result = filter.apply(input);

        assertEquals(original.getRGB(), result.getRGB(0, 0),
                "Identity filter should preserve original color");
    }

    /**
     * Ensures apply() does not modify the original input image in-place.
     */
    @Test
    public void testApplyDoesNotModifyInputImage() {
        BufferedImage input = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Color initial = new Color(100, 150, 200);
        input.setRGB(0, 0, initial.getRGB());

        // Make a copy of the input pixel value
        BufferedImage copy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        copy.setRGB(0, 0, input.getRGB(0, 0));

        Filter filter = new ColorOnlyFilter();
        filter.apply(input); // We're not using the result—just checking for input mutation

        assertEquals(copy.getRGB(0, 0), input.getRGB(0, 0),
                "Input image should remain unchanged after apply()");
    }
}
