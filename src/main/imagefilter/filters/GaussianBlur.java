package imagefilter.filters;

import imagefilter.core.Filter;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * A filter that applies Gaussian blur to an image.
 *
 * <p>This implementation performs 2D convolution using a fixed-size
 * Gaussian kernel and supports context-aware transforms via
 * {@link #transformPixel(BufferedImage, int, int)}.
 *
 * <p>Gaussian blur is often used for denoising, smoothing,
 * or softening image features.
 */
public class GaussianBlur implements Filter {

    /** The precomputed 2D Gaussian kernel matrix. */
    private final double[][] kernel;

    /** The kernel radius, derived from kernel size. */
    private final int radius;

    /**
     * Constructs a GaussianBlur filter with a 5×5 kernel and σ=1.0.
     */
    public GaussianBlur() {
        this.kernel = createGaussianKernel();
        this.radius = kernel.length / 2;
    }

    @Override
    public String name() {
        return "gaussianblur";
    }

    /**
     * Applies Gaussian blur to a single pixel using a convolution
     * over its surrounding neighborhood and the Gaussian kernel.
     *
     * @param image The input image to sample from.
     * @param x     The x-coordinate of the pixel to blur.
     * @param y     The y-coordinate of the pixel to blur.
     * @return A new {@link Color} representing the blurred pixel.
     */
    @Override
    public Color transformPixel(BufferedImage image, int x, int y) {
        int width = image.getWidth();
        int height = image.getHeight();

        double red = 0;
        double green = 0;
        double blue = 0;

        // Iterate over the kernel window centered at (x, y)
        for (int dy = -radius; dy <= radius; dy++) {
            int py = clamp(y + dy, height - 1);  // Clamp vertical coordinate

            for (int dx = -radius; dx <= radius; dx++) {
                int px = clamp(x + dx, width - 1);  // Clamp horizontal coordinate

                Color c = new Color(image.getRGB(px, py));
                double weight = kernel[dy + radius][dx + radius];  // Lookup kernel weight

                // Accumulate weighted RGB values
                red += c.getRed() * weight;
                green += c.getGreen() * weight;
                blue += c.getBlue() * weight;
            }
        }

        // Compose blurred pixel with clamped RGB values
        return new Color(
                clamp((int) Math.round(red), 255),
                clamp((int) Math.round(green), 255),
                clamp((int) Math.round(blue), 255)
        );
    }

    /**
     * Clamps a numeric value into a fixed [min, max] range.
     *
     * @param val The value to clamp.
     * @param max Upper bound.
     * @return The clamped result.
     */
    private static int clamp(int val, int max) {
        return Math.min(max, Math.max(0, val));
    }

    /**
     * Generates a square 2D Gaussian kernel.
     *
     * @return A normalized 2D array of weights summing to 1.
     */
    private static double[][] createGaussianKernel() {
        double[][] kernel = new double[5][5];
        int half = 5 / 2;
        double sum = 0.0;
        double sigma22 = 2 * 1.0 * 1.0;

        // Compute unnormalized weights for the Gaussian distribution
        for (int y = -half; y <= half; y++) {
            for (int x = -half; x <= half; x++) {
                double r2 = x * x + y * y;
                kernel[y + half][x + half] = Math.exp(-r2 / sigma22) / (Math.PI * sigma22);
                sum += kernel[y + half][x + half];
            }
        }

        // Normalize the kernel so all weights sum to 1
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                kernel[y][x] /= sum;
            }
        }

        return kernel;
    }
}
