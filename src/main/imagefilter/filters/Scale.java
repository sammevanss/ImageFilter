package imagefilter.filters;

import imagefilter.core.Filter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * A basic nearest-neighbor image scaler.
 *
 * <p>Supports both downscaling and upscaling using a user-defined scale factor.
 * Uses integer rounding to map destination pixels to the nearest source pixels.
 */
public class Scale implements Filter {

    /** Scale factor used for resizing (e.g., 0.5 for downscaling, 2.0 for upscaling). */
    private final double scaleFactor;

    /**
     * Constructs a Scale filter with the given scale factor.
     *
     * @param scaleFactor a positive value greater than zero
     * @throws IllegalArgumentException if scaleFactor is not positive
     */
    public Scale(double scaleFactor) {
        if (scaleFactor <= 0) {
            throw new IllegalArgumentException("Scale factor must be positive");
        }
        this.scaleFactor = scaleFactor;
    }

    /**
     * Creates a Scale instance from user-provided options (e.g., from a CLI or config).
     *
     * @param opts map of option keys to values; expects a "factor" key
     * @return a new Scale filter instance
     * @throws IllegalArgumentException if the scale factor is invalid or too small
     */
    public static Scale fromOptions(Map<String, String> opts) {
        double factor = 1.0;
        if (opts.containsKey("factor")) {
            try {
                factor = Double.parseDouble(opts.get("factor"));
                if (factor < 0.0001) {
                    throw new IllegalArgumentException("Scale factor must be >= 0.0001");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid scale factor: " + opts.get("factor"));
            }
        }
        return new Scale(factor);
    }

    /**
     * Returns the name of the filter.
     *
     * @return the lowercase string "scale"
     */
    @Override
    public String name() {
        return "scale";
    }

    /**
     * Applies nearest-neighbor scaling to the given image.
     *
     * @param input the source image
     * @return a new BufferedImage with scaled dimensions
     */
    @Override
    public BufferedImage apply(BufferedImage input) {
        int srcWidth = input.getWidth();
        int srcHeight = input.getHeight();

        // Calculate output dimensions
        int dstWidth = (int) Math.round(srcWidth * scaleFactor);
        int dstHeight = (int) Math.round(srcHeight * scaleFactor);

        BufferedImage output = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_INT_RGB);

        // Map each destination pixel to the nearest source pixel
        for (int y = 0; y < dstHeight; y++) {
            int srcY = Math.min(srcHeight - 1, (int) (y / scaleFactor)); // Clamp to avoid overflow
            for (int x = 0; x < dstWidth; x++) {
                int srcX = Math.min(srcWidth - 1, (int) (x / scaleFactor)); // Clamp to image bounds
                Color color = new Color(input.getRGB(srcX, srcY));
                output.setRGB(x, y, color.getRGB());
            }
        }

        return output;
    }

    /**
     * This filter requires full image context and does not support single-pixel transformation.
     *
     * @param inputColor ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public Color transformPixel(Color inputColor) {
        throw new UnsupportedOperationException("Use apply() for Scale");
    }

    /**
     * This filter requires full image context and does not support coordinate-based transformation.
     *
     * @param image ignored
     * @param x     ignored
     * @param y     ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public Color transformPixel(BufferedImage image, int x, int y) {
        throw new UnsupportedOperationException("Use apply() for Scale");
    }
}
