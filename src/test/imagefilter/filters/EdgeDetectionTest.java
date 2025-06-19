package imagefilter.filters;

import imagefilter.core.Filter;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link EdgeDetection} filter.
 * Verifies that edges are correctly detected and the output image is in grayscale format.
 */
public class EdgeDetectionTest {

    /**
     * Creates a 5×5 test image with a white square centered on a black background.
     * This synthetic structure is used to trigger predictable edge detection.
     */
    private BufferedImage createSquareImage() {
        BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);

        // Fill entire image with black
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 5, 5);

        // Draw a centered 3×3 white square
        g.setColor(Color.WHITE);
        g.fillRect(1, 1, 3, 3);
        g.dispose();

        return img;
    }

    /**
     * Verifies that the filter highlights edges around the white square.
     * Interior pixels should remain dark, while edge-adjacent pixels should be bright.
     */
    @Test
    public void testDetectsEdgesAroundWhiteSquare() {
        Filter filter = new EdgeDetection();
        BufferedImage input = createSquareImage();
        BufferedImage result = filter.apply(input);

        // Center pixel (uniform area) should be dark
        int center = result.getRGB(2, 2) & 0xFF;
        assertTrue(center < 50, "Center of square should be dark (low edge response)");

        // Pixels around the center should be bright due to detected edges
        int edgeTop = result.getRGB(2, 1) & 0xFF;
        int edgeLeft = result.getRGB(1, 2) & 0xFF;
        int edgeRight = result.getRGB(3, 2) & 0xFF;
        int edgeBottom = result.getRGB(2, 3) & 0xFF;

        assertTrue(edgeTop > 100, "Top edge should be bright (strong edge)");
        assertTrue(edgeLeft > 100, "Left edge should be bright (strong edge)");
        assertTrue(edgeRight > 100, "Right edge should be bright (strong edge)");
        assertTrue(edgeBottom > 100, "Bottom edge should be bright (strong edge)");
    }

    /**
     * Verifies that the border pixels of the output image are black.
     * This is expected behavior in edge detection to avoid boundary artifacts.
     */
    @Test
    public void testBordersAreBlack() {
        Filter filter = new EdgeDetection();
        BufferedImage input = createSquareImage();
        BufferedImage result = filter.apply(input);

        int width = result.getWidth();
        int height = result.getHeight();

        // Check top and bottom rows
        for (int x = 0; x < width; x++) {
            assertEquals(Color.BLACK.getRGB(), result.getRGB(x, 0), "Top border should be black");
            assertEquals(Color.BLACK.getRGB(), result.getRGB(x, height - 1), "Bottom border should be black");
        }

        // Check left and right columns
        for (int y = 0; y < height; y++) {
            assertEquals(Color.BLACK.getRGB(), result.getRGB(0, y), "Left border should be black");
            assertEquals(Color.BLACK.getRGB(), result.getRGB(width - 1, y), "Right border should be black");
        }
    }

    /**
     * Verifies that the filter throws UnsupportedOperationException
     * when pixel-level transform methods are called directly.
     */
    @Test
    public void testTransformPixelThrowsUnsupported() {
        Filter filter = new EdgeDetection();

        assertThrows(UnsupportedOperationException.class,
                () -> filter.transformPixel(Color.WHITE),
                "Expected exception when calling transformPixel(Color)");

        assertThrows(UnsupportedOperationException.class, () -> {
            BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            filter.transformPixel(dummy, 0, 0);
        }, "Expected exception when calling transformPixel(image, x, y)");
    }
}
