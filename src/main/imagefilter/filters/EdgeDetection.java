package imagefilter.filters;

import imagefilter.core.Filter;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * A filter that performs basic edge detection using the Sobel operator.
 *
 * <p>The filter computes gradient magnitudes in both horizontal and vertical
 * directions using Sobel kernels and combines them to produce edge intensity.
 * The output is a grayscale image where white represents strong edges.
 */
public class EdgeDetection implements Filter {

    // Sobel kernel for horizontal gradient
    private static final int[][] SOBEL_X = {
            { -1, 0, 1 },
            { -2, 0, 2 },
            { -1, 0, 1 }
    };

    // Sobel kernel for vertical gradient
    private static final int[][] SOBEL_Y = {
            { -1, -2, -1 },
            {  0,  0,  0 },
            {  1,  2,  1 }
    };

    /**
     * Returns the name of the filter.
     */
    @Override
    public String name() {
        return "edgedetection";
    }

    /**
     * Applies Sobel-based edge detection to the input image.
     *
     * @param input The source image.
     * @return A grayscale image with edge intensities.
     */
    @Override
    public BufferedImage apply(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();

        // Output image with same dimensions
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Apply Sobel operator to each non-border pixel
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {

                // Gradient components for each channel
                double gxRed = 0, gyRed = 0;
                double gxGreen = 0, gyGreen = 0;
                double gxBlue = 0, gyBlue = 0;

                // 3×3 neighborhood convolution
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color c = new Color(input.getRGB(x + kx, y + ky));

                        int weightX = SOBEL_X[ky + 1][kx + 1];
                        int weightY = SOBEL_Y[ky + 1][kx + 1];

                        gxRed   += c.getRed()   * weightX;
                        gyRed   += c.getRed()   * weightY;

                        gxGreen += c.getGreen() * weightX;
                        gyGreen += c.getGreen() * weightY;

                        gxBlue  += c.getBlue()  * weightX;
                        gyBlue  += c.getBlue()  * weightY;
                    }
                }

                // Compute magnitude using Euclidean norm
                int magRed = clamp((int) Math.hypot(gxRed, gyRed));
                int magGreen = clamp((int) Math.hypot(gxGreen, gyGreen));
                int magBlue = clamp((int) Math.hypot(gxBlue, gyBlue));

                // Convert to grayscale by averaging magnitudes
                int edgeIntensity = (magRed + magGreen + magBlue) / 3;

                // Set final grayscale edge value
                Color edgeColor = new Color(edgeIntensity, edgeIntensity, edgeIntensity);
                output.setRGB(x, y, edgeColor.getRGB());
            }
        }

        // Fill image borders with black to avoid artifacts
        for (int x = 0; x < width; x++) {
            output.setRGB(x, 0, Color.BLACK.getRGB());
            output.setRGB(x, height - 1, Color.BLACK.getRGB());
        }
        for (int y = 0; y < height; y++) {
            output.setRGB(0, y, Color.BLACK.getRGB());
            output.setRGB(width - 1, y, Color.BLACK.getRGB());
        }

        return output;
    }

    /**
     * This filter does not support single-pixel transform mode.
     *
     * @param inputColor Ignored.
     * @return Never returns; always throws UnsupportedOperationException.
     */
    @Override
    public Color transformPixel(Color inputColor) {
        throw new UnsupportedOperationException("Use apply() for EdgeDetection filter");
    }

    /**
     * This filter does not support contextual pixel transform mode.
     *
     * @param image Input image.
     * @param x     Pixel X-coordinate.
     * @param y     Pixel Y-coordinate.
     * @return Never returns; always throws UnsupportedOperationException.
     */
    @Override
    public Color transformPixel(BufferedImage image, int x, int y) {
        throw new UnsupportedOperationException("Use apply() for EdgeDetection filter");
    }

    /**
     * Clamps a value to the specified [min, max] range.
     *
     * @param val Value to clamp.
     * @return Clamped result.
     */
    private static int clamp(int val) {
        return Math.min(255, Math.max(0, val));
    }
}
