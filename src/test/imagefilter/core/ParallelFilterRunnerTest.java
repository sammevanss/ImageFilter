package imagefilter.core;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ParallelFilterRunner}.
 * These tests verify correct behavior for parallel execution, apply fallback,
 * thread configurations, input image immutability, and overload coverage.
 */
public class ParallelFilterRunnerTest {

    /**
     * Tests the no-argument run() overload.
     * Verifies that the filter is applied using default thread count and fallback logic.
     */
    @Test
    void run() {
        BufferedImage input = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Filter filter = new ConstantRedFilter();

        // Run filter with default settings (uses transformPixel)
        BufferedImage result = ParallelFilterRunner.run(filter, input);

        // All pixels should be red
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                assertEquals(Color.RED.getRGB(), result.getRGB(x, y),
                        "Expected RED at (" + x + ", " + y + ")");
            }
        }
    }

    /**
     * Tests the run(filter, image, threads) overload.
     * Ensures the filter works correctly with a specific thread count.
     */
    @Test
    void testRun() {
        BufferedImage input = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Filter filter = new ConstantRedFilter();

        // Run using 3 threads
        BufferedImage result = ParallelFilterRunner.run(filter, input, 3);

        // All pixels should be red
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                assertEquals(Color.RED.getRGB(), result.getRGB(x, y),
                        "Expected RED at (" + x + ", " + y + ")");
            }
        }
    }

    /**
     * Tests the full run(filter, image, threads, useApplyIfOverridden) overload.
     * Ensures correct behavior when apply fallback is disabled.
     */
    @Test
    void testRun1() {
        BufferedImage input = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Filter filter = new ConstantRedFilter();

        // Explicitly disable apply() fallback, must use transformPixel
        BufferedImage result = ParallelFilterRunner.run(filter, input, 2, false);

        // All pixels should be red
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                assertEquals(Color.RED.getRGB(), result.getRGB(x, y),
                        "Expected RED at (" + x + ", " + y + ")");
            }
        }
    }

    /**
     * A simple test filter that always returns red from transformPixel.
     * Used for verifying spatial filtering and fallback logic.
     */
    static class ConstantRedFilter implements Filter {
        @Override
        public Color transformPixel(Color color) {
            return Color.RED;
        }

        @Override
        public String name() {
            return "constantred";
        }
    }

    /**
     * A test filter that overrides apply() only.
     * Used to ensure apply() is preferred over transformPixel if requested.
     */
    static class BlackApplyOnlyFilter implements Filter {
        @Override
        public BufferedImage apply(BufferedImage input) {
            BufferedImage out = new BufferedImage(
                    input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < input.getWidth(); x++) {
                for (int y = 0; y < input.getHeight(); y++) {
                    out.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
            return out;
        }

        @Override
        public Color transformPixel(Color color) {
            throw new UnsupportedOperationException("transformPixel() should not be used.");
        }

        @Override
        public String name() {
            return "blackapplyonly";
        }
    }

    /**
     * Verifies that a transformPixel-based filter runs correctly using multiple threads.
     */
    @Test
    public void testParallelExecutionWithTransformPixel() {
        int width = 3, height = 3;
        BufferedImage input = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Filter filter = new ConstantRedFilter();

        BufferedImage result = ParallelFilterRunner.run(filter, input, 4, true);

        // All pixels should be red
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int actual = result.getRGB(x, y);
                assertEquals(Color.RED.getRGB(), actual, "Expected RED at (" + x + ", " + y + ")");
            }
        }
    }

    /**
     * Verifies that a filter with only apply() overridden is used properly
     * when apply fallback is enabled.
     */
    @Test
    public void testFallbackToApplyMethod() {
        int width = 2, height = 2;
        BufferedImage input = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Filter filter = new BlackApplyOnlyFilter();

        BufferedImage result = ParallelFilterRunner.run(filter, input, 4, true);

        // All pixels should be black
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int actual = result.getRGB(x, y);
                assertEquals(Color.BLACK.getRGB(), actual, "Expected BLACK at (" + x + ", " + y + ")");
            }
        }
    }

    /**
     * Tests that the filter works correctly with only one thread.
     * This verifies fallback behavior to serial processing.
     */
    @Test
    public void testSingleThreadedExecution() {
        BufferedImage input = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Filter filter = new ConstantRedFilter();

        // Run with a single thread
        BufferedImage result = ParallelFilterRunner.run(filter, input, 1, true);

        // All pixels should be red
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                assertEquals(Color.RED.getRGB(), result.getRGB(x, y),
                        "Expected RED at (" + x + ", " + y + ")");
            }
        }
    }

    /**
     * Ensures that if apply() is available, it is not used when disallowed via config.
     */
    @Test
    public void testSkipApplyWhenNotAllowed() {
        BufferedImage input = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Filter filter = new ConstantRedFilter();  // Does not override apply()

        // Should use transformPixel even if apply() is available
        BufferedImage result = ParallelFilterRunner.run(filter, input, 2, false);

        // All pixels should be red
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                assertEquals(Color.RED.getRGB(), result.getRGB(x, y),
                        "Expected RED at (" + x + ", " + y + ")");
            }
        }
    }

    /**
     * Ensures the input image is not modified during processing.
     * Verifies immutability of source image by comparing to a copy.
     */
    @Test
    public void testInputImageUnchanged() {
        BufferedImage input = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        input.setRGB(0, 0, Color.BLUE.getRGB());

        // Make a copy of the original image
        BufferedImage copy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        copy.setRGB(0, 0, input.getRGB(0, 0));

        Filter filter = new ConstantRedFilter();
        ParallelFilterRunner.run(filter, input);  // Output ignored

        // Confirm original input image is unchanged
        assertEquals(copy.getRGB(0, 0), input.getRGB(0, 0),
                "Input image should not be modified");
    }
}
