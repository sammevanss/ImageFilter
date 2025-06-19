package imagefilter.filters;

import imagefilter.core.Filter;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FilterPipeline}.
 * Verifies correct filter chaining, name composition, flattening behavior, and error handling.
 */
public class FilterPipelineTest {

    /**
     * Simple filter that fills the image with red.
     */
    static class RedFilter implements Filter {
        @Override public String name() { return "red"; }

        @Override
        public BufferedImage apply(BufferedImage input) {
            BufferedImage out = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setColor(Color.RED);
            g.fillRect(0, 0, input.getWidth(), input.getHeight());
            g.dispose();
            return out;
        }
    }

    /**
     * Simple filter that fills the image with green.
     */
    static class GreenFilter implements Filter {
        @Override public String name() { return "green"; }

        @Override
        public BufferedImage apply(BufferedImage input) {
            BufferedImage out = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setColor(Color.GREEN);
            g.fillRect(0, 0, input.getWidth(), input.getHeight());
            g.dispose();
            return out;
        }
    }

    /**
     * Creates a 1×1 white test image.
     */
    private BufferedImage makeWhiteImage() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, Color.WHITE.getRGB());
        return img;
    }

    /**
     * Verifies that filters are applied in order from left to right.
     */
    @Test
    public void testPipelineAppliesAllFiltersInOrder() {
        FilterPipeline pipeline = new FilterPipeline(Arrays.asList(
                new RedFilter(),  // white → red
                new GreenFilter() // red → green
        ));

        BufferedImage input = makeWhiteImage();
        BufferedImage result = pipeline.apply(input);

        assertEquals(Color.GREEN.getRGB(), result.getRGB(0, 0), "Expected final color to be green");
    }

    /**
     * Verifies that pipeline name is a pipe-separated list of subfilter names.
     */
    @Test
    public void testPipelineNameIsJoined() {
        FilterPipeline pipeline = new FilterPipeline(List.of(new RedFilter(), new GreenFilter()));
        assertEquals("red|green", pipeline.name(), "Pipeline name should concatenate subfilter names");
    }

    /**
     * Verifies that constructing an empty pipeline throws an exception.
     */
    @Test
    public void testPipelineThrowsOnEmptyList() {
        assertThrows(IllegalArgumentException.class,
                () -> new FilterPipeline(Collections.emptyList()),
                "Pipeline should not allow an empty filter list");
    }

    /**
     * Verifies that passing null to the constructor throws an exception.
     */
    @Test
    public void testPipelineThrowsOnNullList() {
        assertThrows(IllegalArgumentException.class,
                () -> new FilterPipeline(null),
                "Pipeline should not accept null filter list");
    }

    /**
     * Verifies that nested pipelines are flattened into a single pipeline.
     */
    @Test
    public void testNestedPipelineIsFlattened() {
        Filter f1 = new RedFilter();
        Filter f2 = new GreenFilter();
        FilterPipeline nested = new FilterPipeline(List.of(f1, f2));
        FilterPipeline top = new FilterPipeline(List.of(nested, f1));

        assertEquals("red|green|red", top.name(), "Pipeline should flatten nested pipelines");
    }

    /**
     * Verifies that calling transformPixel(Color) throws as unsupported.
     */
    @Test
    public void testTransformPixelThrows() {
        FilterPipeline pipeline = new FilterPipeline(List.of(new RedFilter()));
        assertThrows(UnsupportedOperationException.class,
                () -> pipeline.transformPixel(Color.RED),
                "transformPixel(Color) should be unsupported in pipelines");
    }

    /**
     * Verifies that calling transformPixel(image, x, y) throws as unsupported.
     */
    @Test
    public void testTransformPixelWithCoordsThrows() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        FilterPipeline pipeline = new FilterPipeline(List.of(new RedFilter()));

        assertThrows(UnsupportedOperationException.class,
                () -> pipeline.transformPixel(img, 0, 0),
                "transformPixel(image, x, y) should be unsupported in pipelines");
    }
}
