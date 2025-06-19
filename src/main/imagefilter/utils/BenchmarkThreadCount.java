package imagefilter.utils;

import imagefilter.core.Filter;
import imagefilter.core.FilterFactory;
import imagefilter.core.ParallelFilterRunner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Benchmarks the performance of a filter across different thread counts.
 * Accepts command-line arguments for filter name, input image path,
 * optional thread counts, and whether to use the filter's custom apply() method.
 */
public class BenchmarkThreadCount {

    public static void main(String[] args) throws Exception {
        // Validate minimum required arguments
        if (args.length < 2) {
            System.out.println("Usage: java ThreadBenchmark <filter> <inputImage> [threads=<n> | threads=<min>-<max> | threads=<list>] [useApply=true|false]");
            return;
        }

        String filterName = args[0].toLowerCase();
        File inputFile = new File(args[1]);
        List<Integer> threadCounts = Arrays.asList(1, 2, 8, 16); // Default thread counts

        // Parse optional thread count argument
        if (args.length >= 3) {
            String threadsArg = args[2].toLowerCase();
            if (threadsArg.startsWith("threads=")) {
                String spec = threadsArg.substring("threads=".length()).trim();
                try {
                    if (spec.contains(",")) {
                        threadCounts = parseThreadList(spec); // Custom list e.g. 1,2,4
                    } else if (spec.contains("-")) {
                        threadCounts = parseThreadRange(spec); // Range e.g. 2-8
                    } else {
                        // Threads=n → benchmark from 1 to n
                        int maxThreads = Integer.parseInt(spec);
                        if (maxThreads < 1) throw new IllegalArgumentException();
                        threadCounts = new ArrayList<>();
                        for (int i = 1; i <= maxThreads; i++) {
                            threadCounts.add(i);
                        }
                    }
                } catch (Exception e) {
                    // Invalid thread specification format
                    System.err.println("Invalid threads specification. Use one of:");
                    System.err.println("  threads=<n>");
                    System.err.println("  threads=<min>-<max>");
                    System.err.println("  threads=<num1>,<num2>,<num3>,...");
                    return;
                }
            } else {
                System.err.println("Invalid third argument. Expected threads=...");
                return;
            }
        }

        // Parse optional useApply flag
        boolean useApplyIfOverridden = true;
        if (args.length >= 4) {
            String applyFlag = args[3].toLowerCase();
            if (applyFlag.startsWith("useapply=")) {
                useApplyIfOverridden = Boolean.parseBoolean(applyFlag.substring("useapply=".length()));
            }
        }

        // Load input image
        BufferedImage inputImage = ImageIO.read(inputFile);

        System.out.printf("Benchmarking filter '%s' on image '%s'%n", filterName, inputFile.getName());
        System.out.printf("Testing thread counts: %s%n", threadCounts);

        // Check if the filter class overrides apply(BufferedImage)
        boolean hasCustomApply = overridesApplyMethod(FilterFactory.getFilter(filterName, new HashMap<>()));

        // Print summary of apply() usage decision
        if (useApplyIfOverridden && hasCustomApply) {
            System.out.printf("🟢 Using %s's custom apply() implementation", filterName);
        } else if (useApplyIfOverridden) {
            System.out.println("🟡 No custom apply() found using pixel-by-pixel transform");
        } else {
            System.out.println("🔵 Ignoring apply() — forcing pixel-by-pixel transform");
        }
        System.out.println();

        // Benchmark loop
        long bestTime = Long.MAX_VALUE;
        int bestThreadCount = -1;

        for (int threads : threadCounts) {
            Map<String, String> options = new HashMap<>();
            options.put("threads", String.valueOf(threads));

            Filter filter;
            try {
                filter = FilterFactory.getFilter(filterName, options);
            } catch (IllegalArgumentException e) {
                System.err.printf("❌ Filter '%s' does not support threading options.%n", filterName);
                return;
            }

            // Warm-up run to stabilize JIT behavior
            ParallelFilterRunner.run(filter, inputImage, threads, useApplyIfOverridden);

            // Timed run
            long start = System.nanoTime();
            ParallelFilterRunner.run(filter, inputImage, threads, useApplyIfOverridden);
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("Threads: %3d → %5d ms%n", threads, durationMs);

            // Track best performance
            if (durationMs < bestTime) {
                bestTime = durationMs;
                bestThreadCount = threads;
            }
        }

        System.out.printf("%n✅ Best performance: %d ms using %d thread(s)%n", bestTime, bestThreadCount);
    }

    /**
     * Parses a comma-separated list of integers into a sorted list.
     * Example: "1,2,8" → [1, 2, 8]
     */
    private static List<Integer> parseThreadList(String spec) {
        String[] parts = spec.split(",");
        List<Integer> list = new ArrayList<>();
        for (String part : parts) {
            int n = Integer.parseInt(part.trim());
            if (n < 1) throw new IllegalArgumentException("Thread counts must be positive");
            list.add(n);
        }
        Collections.sort(list);
        return list;
    }

    /**
     * Parses a thread range string like "2-5" into a list [2, 3, 4, 5].
     */
    private static List<Integer> parseThreadRange(String spec) {
        String[] parts = spec.split("-");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid range format");
        int min = Integer.parseInt(parts[0].trim());
        int max = Integer.parseInt(parts[1].trim());
        if (min < 1 || max < min) throw new IllegalArgumentException("Invalid range values");
        List<Integer> list = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            list.add(i);
        }
        return list;
    }

    /**
     * Checks whether the filter class overrides apply(BufferedImage) method.
     * This helps decide whether to use the custom filter logic or fall back to default.
     */
    private static boolean overridesApplyMethod(Filter filter) {
        try {
            Method m = filter.getClass().getMethod("apply", BufferedImage.class);
            return !m.getDeclaringClass().equals(Filter.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
