package imagefilter.core;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Base interface for image filters in the ImageFilter framework.
 *
 * <p>Filters can operate in two ways:
 * <ul>
 *   <li>Per-pixel color transformations (e.g., grayscale, invert)</li>
 *   <li>Contextual transformations that use the full image (e.g., blur, edge detection)</li>
 * </ul>
 */
public interface Filter {

    /**
     * Returns the name of the filter.
     *
     * @return the lowercase name of this filter (e.g., "grayscale", "blur")
     */
    String name();

    /**
     * Transforms a single pixel color without access to surrounding context.
     *
     * <p>This method should be overridden for filters like invert or grayscale that only
     * require the input pixel's color value.
     *
     * @param inputColor the color of the pixel
     * @return the transformed color
     * @throws UnsupportedOperationException if this method is not implemented by the filter
     */
    default Color transformPixel(Color inputColor) {
        throw new UnsupportedOperationException(
                "transformPixel(Color) not implemented for " + name());
    }

    /**
     * Transforms a pixel with access to the entire image context.
     *
     * <p>This method should be overridden for filters like blur or edge detection
     * that depend on neighboring pixel values.
     *
     * @param image the source image
     * @param x the x-coordinate of the pixel
     * @param y the y-coordinate of the pixel
     * @return the transformed color
     * @throws UnsupportedOperationException if this method is not implemented by the filter
     */
    default Color transformPixel(BufferedImage image, int x, int y) {
        throw new UnsupportedOperationException(
                "transformPixel(BufferedImage, int, int) not implemented for " + name());
    }

    /**
     * Applies the filter to the entire image using single-threaded iteration.
     *
     * <p>This method first attempts to use the image-aware {@link #transformPixel(BufferedImage, int, int)}.
     * If not supported, it falls back to the simpler {@link #transformPixel(Color)}.
     *
     * @param input the source image
     * @return the filtered result image
     * @throws IllegalStateException if neither transform method is implemented
     */
    default BufferedImage apply(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();

        // Create a new image to store the result
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Loop over every pixel in the input image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color outputColor;

                try {
                    // Prefer image-aware transformation if available
                    outputColor = transformPixel(input, x, y);
                } catch (UnsupportedOperationException e1) {
                    try {
                        // Fall back to single-pixel transformation
                        Color inputColor = new Color(input.getRGB(x, y));
                        outputColor = transformPixel(inputColor);
                    } catch (UnsupportedOperationException e2) {
                        // If neither method is supported, fail with a clear error
                        throw new IllegalStateException(
                                "No valid transformPixel method implemented for filter: " + name(), e2);
                    }
                }

                // Set the transformed color in the result image
                result.setRGB(x, y, outputColor.getRGB());
            }
        }

        return result;
    }
}
