package imagefilter.filters;

import imagefilter.core.Filter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A composite filter that applies a sequence of individual filters in order.
 *
 * <p>This allows you to build filter pipelines like:
 * {@code grayscale | gaussianblur | edge-detect}.
 */
public class FilterPipeline implements Filter {

    /** Ordered list of filters to apply. */
    private final List<Filter> filters;

    /**
     * Constructs a new pipeline of filters.
     *
     * @param filters A non-empty list of filters to apply in sequence.
     * @throws IllegalArgumentException if the filter list is null or empty.
     */
    public FilterPipeline(List<Filter> filters) {
        if (filters == null || filters.isEmpty()) {
            throw new IllegalArgumentException("FilterPipeline requires at least one filter");
        }
        this.filters = flatten(filters);
    }

    /**
     * Flattens nested pipelines into a single list of filters.
     *
     * @param filters A possibly nested list of filters
     * @return A flat list with no nested pipelines
     */
    private List<Filter> flatten(List<Filter> filters) {
        List<Filter> result = new ArrayList<>();
        for (Filter f : filters) {
            if (f instanceof FilterPipeline pipeline) {
                // Recursively add filters from inner pipelines
                result.addAll(pipeline.filters);
            } else {
                result.add(f);
            }
        }
        return result;
    }

    /**
     * Returns the combined name of the filters in this pipeline.
     *
     * <p>For example: {@code "grayscale|gaussianblur|invert"}
     */
    @Override
    public String name() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filters.size(); i++) {
            if (i > 0) {
                sb.append("|");
            }
            sb.append(filters.get(i).name());
        }
        return sb.toString();
    }

    /**
     * Applies each filter in the pipeline in sequence.
     *
     * <p>The output of one filter becomes the input to the next.
     *
     * @param input The original input image.
     * @return The result after all filters have been applied.
     */
    @Override
    public BufferedImage apply(BufferedImage input) {
        BufferedImage current = input;
        for (Filter f : filters) {
            current = f.apply(current);
        }
        return current;
    }

    /**
     * Per-pixel transform is unsupported for pipelines.
     *
     * <p>This is because the logic of a pipeline depends on the image context
     * and cannot be expressed as a single color transformation.
     *
     * @param inputColor Ignored.
     * @throws UnsupportedOperationException always.
     */
    @Override
    public Color transformPixel(Color inputColor) {
        throw new UnsupportedOperationException(
                "FilterPipeline filter does not support transformPixel(Color) directly");
    }

    /**
     * Coordinate-aware transform is unsupported for pipelines.
     *
     * <p>Each filter may support this, but the pipeline applies full-image
     * transformations in sequence.
     *
     * @param image The input image.
     * @param x     The x-coordinate of the pixel.
     * @param y     The y-coordinate of the pixel.
     * @throws UnsupportedOperationException always.
     */
    @Override
    public Color transformPixel(BufferedImage image, int x, int y) {
        throw new UnsupportedOperationException(
                "FilterPipeline filter does not support transformPixel(BufferedImage,int,int) directly");
    }
}
