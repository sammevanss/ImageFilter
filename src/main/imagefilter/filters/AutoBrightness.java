package imagefilter.filters;

import imagefilter.core.Filter;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * A filter that automatically adjusts the brightness of an image
 * so that the average perceived luminance is 128 (mid-gray).
 *
 * <p>This implementation uses parallel streams to compute and apply
 * the brightness adjustment efficiently using multiple threads.
 *
 * <p>It uses the Rec. 601 luminance approximation:
 * luminance ≈ (299 * R + 587 * G + 114 * B) / 1000
 */
public class AutoBrightness implements Filter {

    /** Desired average brightness value (on a scale of 0–255). */
    private static final int TARGET_BRIGHTNESS = 128;

    /**
     * Returns the name of the filter.
     *
     * @return A string identifier for this filter.
     */
    @Override
    public String name() {
        return "autobrightness";
    }

    /**
     * Applies automatic brightness correction to the given image.
     * The result will have an average perceived brightness of approximately 128.
     *
     * @param input The input image to process.
     * @return A new image with adjusted brightness.
     */
    @Override
    public BufferedImage apply(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();
        int pixelCount = width * height;

        // Extract the pixel data from the image into a 1D array
        int[] pixels = getPixelArray(input, width, height);

        // Compute the average perceived brightness using parallel processing
        double averageBrightness = computeAverageBrightness(pixels, pixelCount);

        // Avoid divide-by-zero if the image is completely black
        if (averageBrightness == 0) {
            return input;
        }

        // Calculate a brightness scale factor to bring average brightness to 128
        double scale = TARGET_BRIGHTNESS / averageBrightness;

        // Adjust the brightness of each pixel
        int[] adjustedPixels = adjustBrightness(pixels, scale);

        // Build a new BufferedImage from the adjusted pixels
        return createImageFromPixels(adjustedPixels, width, height);
    }

    /**
     * Extracts pixel data from a BufferedImage into a 1D ARGB array.
     *
     * @param image  The source image.
     * @param width  Image width.
     * @param height Image height.
     * @return A 1D array of packed ARGB pixel values.
     */
    private int[] getPixelArray(BufferedImage image, int width, int height) {
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        return pixels;
    }

    /**
     * Computes the average perceived brightness of an array of pixels,
     * using the Rec. 601 luminance approximation scaled by 1000.
     *
     * @param pixels     The input pixel array (ARGB).
     * @param pixelCount The number of pixels in the array.
     * @return The average perceived brightness on a 0–255 scale.
     */
    private double computeAverageBrightness(int[] pixels, int pixelCount) {
        long totalBrightness = Arrays.stream(pixels)
                .parallel()
                .mapToLong(this::getPerceivedBrightness)
                .sum();

        // Convert the sum to average and adjust for integer scaling
        return totalBrightness / (double) pixelCount / 1000.0;
    }

    /**
     * Approximates perceived brightness (luminance) for a single pixel
     * using integer math with Rec. 601 weights.
     *
     * @param rgb The ARGB-packed pixel value.
     * @return Luminance scaled by 1000 (to preserve precision).
     */
    private long getPerceivedBrightness(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 299L * r + 587L * g + 114L * b;
    }

    /**
     * Applies a brightness scaling factor to each pixel in the array.
     *
     * @param pixels The input pixel array (ARGB).
     * @param scale  The brightness adjustment factor.
     * @return A new array of RGB-packed adjusted pixel values.
     */
    private int[] adjustBrightness(int[] pixels, double scale) {
        return Arrays.stream(pixels)
                .parallel()
                .map(rgb -> {
                    int r = clamp((int) (((rgb >> 16) & 0xFF) * scale));
                    int g = clamp((int) (((rgb >> 8) & 0xFF) * scale));
                    int b = clamp((int) ((rgb & 0xFF) * scale));

                    // Pack the RGB components back into a single integer
                    return (r << 16) | (g << 8) | b;
                })
                .toArray();
    }

    /**
     * Constructs a new BufferedImage from a 1D array of RGB pixel values.
     *
     * @param pixels The RGB pixel array.
     * @param width  The image width.
     * @param height The image height.
     * @return A BufferedImage containing the adjusted pixels.
     */
    private BufferedImage createImageFromPixels(int[] pixels, int width, int height) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        output.setRGB(0, 0, width, height, pixels, 0, width);
        return output;
    }

    /**
     * This filter does not support pixel-by-pixel transform mode.
     *
     * @param inputColor Ignored.
     * @return Never returns; always throws UnsupportedOperationException.
     */
    @Override
    public java.awt.Color transformPixel(java.awt.Color inputColor) {
        throw new UnsupportedOperationException("Use apply() for AutoBrightness");
    }

    /**
     * Clamps a color component value to the 0–255 range.
     *
     * @param val The value to clamp.
     * @return The clamped value, always between 0 and 255.
     */
    private static int clamp(int val) {
        return val < 0 ? 0 : Math.min(val, 255);
    }
}
