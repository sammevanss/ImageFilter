package imagefilter.filters;

import imagefilter.core.Filter;

import java.awt.Color;

/**
 * A simple filter that converts an image to grayscale using
 * perceived luminance based on Rec. 601 weights.
 */
public class Grayscale implements Filter {

    @Override
    public String name() {
        return "grayscale";
    }

    /**
     * Converts a color pixel to grayscale using the standard luminance formula:
     * <pre>
     *     gray = 0.3 * R + 0.59 * G + 0.11 * B
     * </pre>
     * This weighting reflects human sensitivity to different color channels.
     *
     * @param inputColor The original pixel color.
     * @return A new Color where R = G = B = perceived luminance.
     */
    @Override
    public Color transformPixel(Color inputColor) {
        int r = inputColor.getRed();
        int g = inputColor.getGreen();
        int b = inputColor.getBlue();

        // Compute perceived brightness using Rec. 601 weights
        int gray = (int) (0.3 * r + 0.59 * g + 0.11 * b);

        // Clamp the value to the valid 0–255 range
        gray = Math.min(255, Math.max(0, gray));

        // Return a new grayscale color (equal R, G, B)
        return new Color(gray, gray, gray);
    }
}
