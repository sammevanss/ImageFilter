package imagefilter.core;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/**
 * Executes a given {@link Filter} on an image using parallel processing.
 * Automatically uses the filter's {@code apply()} method if it has been overridden.
 * Otherwise, applies the filter using parallelized pixel-by-pixel transformation.
 */
public class ParallelFilterRunner {

    /**
     * Runs the given filter on the image using the default number of threads
     * (based on the number of available processor cores).
     *
     * @param filter the filter to apply
     * @param input the input image
     * @return the filtered image
     * @throws IllegalArgumentException if filter or input is null
     */
    public static BufferedImage run(Filter filter, BufferedImage input) {
        return run(filter, input, Runtime.getRuntime().availableProcessors(), true);
    }

    /**
     * Runs the filter on the image using a specified number of threads.
     *
     * @param filter the filter to apply
     * @param input the input image
     * @param threads the number of threads to use (1 = serial execution)
     * @return the filtered image
     * @throws IllegalArgumentException if filter or input is null
     */
    public static BufferedImage run(Filter filter, BufferedImage input, int threads) {
        return run(filter, input, threads, true);
    }

    /**
     * Runs the filter on the image using parallel processing.
     * If {@code useApplyIfOverridden} is true and the filter class overrides {@code apply()},
     * then that method is used instead of per-pixel filtering.
     *
     * @param filter the filter to apply
     * @param input the input image
     * @param threads number of threads to use for parallel execution
     * @param useApplyIfOverridden whether to allow the filter to use its custom {@code apply()} method
     * @return the filtered image
     * @throws IllegalArgumentException if filter or input is null
     */
    public static BufferedImage run(Filter filter, BufferedImage input, int threads, boolean useApplyIfOverridden) {
        if (filter == null || input == null) {
            throw new IllegalArgumentException("Filter and input image must not be null");
        }

        // If the filter overrides apply(), use that instead of transformPixel
        if (useApplyIfOverridden && isApplyMethodOverridden(filter)) {
            return filter.apply(input);
        }

        int width = input.getWidth();
        int height = input.getHeight();

        // Preserve the input image type if possible; fall back to RGB for undefined type (0)
        int imageType = input.getType() == 0 ? BufferedImage.TYPE_INT_RGB : input.getType();
        BufferedImage result = new BufferedImage(width, height, imageType);

        applyFilterInParallel(filter, input, result, threads);

        return result;
    }

    /**
     * Applies the filter in parallel across all rows in the image.
     *
     * @param filter the filter to apply
     * @param input the input image
     * @param output the output image to write transformed pixels into
     * @param threads the number of threads to use
     */
    private static void applyFilterInParallel(Filter filter, BufferedImage input, BufferedImage output, int threads) {
        int width = input.getWidth();
        int height = input.getHeight();

        // Use ForkJoinPool to parallelize across image rows
        try (ForkJoinPool pool = (threads <= 1) ? ForkJoinPool.commonPool() : new ForkJoinPool(threads)) {
            IntStream rowStream = IntStream.range(0, height);

            // Submit the parallel task and block until complete
            pool.submit(() ->
                    rowStream.forEach(y -> {
                        for (int x = 0; x < width; x++) {
                            // Transform each pixel safely (handle fallbacks internally)
                            Color outputColor = transformPixelSafe(filter, input, x, y);
                            output.setRGB(x, y, outputColor.getRGB());
                        }
                    })
            ).get();
        } catch (Exception e) {
            throw new RuntimeException("Parallel filter execution failed", e);
        }
    }

    /**
     * Attempts to transform a pixel using the filter's spatial method.
     * If the filter does not support spatial input, it falls back to the simpler color-only version.
     *
     * @param filter the filter to apply
     * @param image the input image
     * @param x the x-coordinate of the pixel
     * @param y the y-coordinate of the pixel
     * @return the transformed color
     */
    private static Color transformPixelSafe(Filter filter, BufferedImage image, int x, int y) {
        try {
            // Attempt to use spatial (x, y)-aware method
            return filter.transformPixel(image, x, y);
        } catch (UnsupportedOperationException e) {
            // If unsupported, fall back to basic color transform
            Color inputColor = new Color(image.getRGB(x, y));
            return filter.transformPixel(inputColor);
        }
    }

    /**
     * Checks if the filter class overrides the {@code apply(BufferedImage)} method.
     *
     * @param filter the filter to inspect
     * @return true if the filter class implements its own apply method
     */
    private static boolean isApplyMethodOverridden(Filter filter) {
        try {
            // Look up the apply() method in the actual filter class
            Method method = filter.getClass().getMethod("apply", BufferedImage.class);

            // If the method's declaring class is not the Filter interface, it has been overridden
            return !method.getDeclaringClass().equals(Filter.class);
        } catch (NoSuchMethodException e) {
            // Should not happen unless the Filter interface is broken
            return false;
        }
    }
}
