package imagefilter.filters;

import imagefilter.core.Filter;

import java.awt.Color;

/**
 * A filter that inverts the colors of an image.
 *
 * <p>Each RGB component is transformed as:
 * <pre>
 *     newComponent = 255 - originalComponent
 * </pre>
 * This results in a photographic negative of the input image.
 */
public class Invert implements Filter {

    /**
     * Returns the lowercase name of the filter.
     *
     * @return the filter name "invert"
     */
    @Override
    public String name() {
        return "invert";
    }

    /**
     * Inverts the color of a single pixel by subtracting each RGB component from 255.
     *
     * @param inputColor The original pixel color.
     * @return A new color where each channel has been inverted.
     */
    @Override
    public Color transformPixel(Color inputColor) {
        // Invert each RGB channel by subtracting from 255
        int r = 255 - inputColor.getRed();
        int g = 255 - inputColor.getGreen();
        int b = 255 - inputColor.getBlue();

        // Return the inverted color
        return new Color(r, g, b);
    }
}
