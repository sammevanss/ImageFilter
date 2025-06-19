package imagefilter.filters;

import imagefilter.core.Filter;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * A filter that automatically adjusts image contrast by stretching luminance
 * values to span the full 0–255 range.
 *
 * <p>Contrast stretching improves image visibility by mapping the darkest and
 * brightest values in the image to black and white, respectively.
 */
public class AutoContrast implements Filter {

    // Stores the minimum and maximum luminance values found in the image
    private int minLum = 0;
    private int maxLum = 255;

    /**
     * Returns the filter's identifier.
     */
    @Override
    public String name() {
        return "autocontrast";
    }

    /**
     * Applies contrast stretching to the input image.
     *
     * @param input The source image.
     * @return A new image with stretched contrast.
     */
    @Override
    public BufferedImage apply(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();

        // Read the entire image into a pixel array
        int[] pixels = input.getRGB(0, 0, width, height, null, 0, width);

        // Track global min and max luminance values in parallel
        AtomicInteger min = new AtomicInteger(255);
        AtomicInteger max = new AtomicInteger(0);

        // First pass: scan for min and max luminance
        IntStream.range(0, height).parallel().forEach(y -> {
            for (int x = 0; x < width; x++) {
                int rgb = pixels[y * width + x];

                // Extract RGB components
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Calculate perceived luminance using standard weights
                int lum = (int) (0.3 * r + 0.59 * g + 0.11 * b);

                // Atomically update global min/max
                min.getAndUpdate(current -> Math.min(current, lum));
                max.getAndUpdate(current -> Math.max(current, lum));
            }
        });

        // Store computed bounds
        minLum = min.get();
        maxLum = max.get();

        // No contrast to stretch — image is flat
        if (minLum == maxLum) {
            return input;
        }

        // Second pass: stretch each pixel’s RGB channels based on luminance bounds
        int[] outPixels = new int[width * height];
        IntStream.range(0, height).parallel().forEach(y -> {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                int rgb = pixels[idx];

                int r = stretch((rgb >> 16) & 0xFF, minLum, maxLum);
                int g = stretch((rgb >> 8) & 0xFF, minLum, maxLum);
                int b = stretch(rgb & 0xFF, minLum, maxLum);

                // Pack the adjusted RGB values into the output pixel
                outPixels[idx] = (r << 16) | (g << 8) | b;
            }
        });

        // Construct output image from modified pixels
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        output.setRGB(0, 0, width, height, outPixels, 0, width);
        return output;
    }

    /**
     * Transforms a single pixel color using the current min/max luminance stretch.
     *
     * @param inputColor The input color.
     * @return The transformed color with adjusted contrast.
     */
    @Override
    public java.awt.Color transformPixel(java.awt.Color inputColor) {
        int r = stretch(inputColor.getRed(), minLum, maxLum);
        int g = stretch(inputColor.getGreen(), minLum, maxLum);
        int b = stretch(inputColor.getBlue(), minLum, maxLum);
        return new java.awt.Color(r, g, b);
    }

    /**
     * Applies linear contrast stretching to a single channel value.
     * Values are mapped from [min, max] to [0, 255].
     *
     * @param value The original channel value (0–255).
     * @param min   The image’s minimum luminance.
     * @param max   The image’s maximum luminance.
     * @return The stretched and clamped channel value.
     */
    private int stretch(int value, int min, int max) {
        if (min == max) {
            return value;  // Avoid division by zero for uniform images
        }

        // Linearly map to the 0–255 range with rounding
        int stretched = (int) Math.round((value - min) * 255.0 / (max - min));

        // Clamp result to valid range
        return Math.min(255, Math.max(0, stretched));
    }
}
