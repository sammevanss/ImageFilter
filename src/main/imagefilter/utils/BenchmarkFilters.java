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
 * Utility class for benchmarking image filters using different thread counts.
 * <p>
 * Usage:
 *   java BenchmarkFilters <filter1> [filter2] ... <inputImage>
 *                         [threads=...] [useApply=true|false] [warmup=3] [measured=5]
 * <p>
 * Supports benchmarking filters using their custom `apply()` method or pixel-by-pixel fallback,
 * across multiple thread counts, with configurable warmup and measured run counts.
 */
public class BenchmarkFilters {

    // Default number of warm-up and measured runs
    private static int WARMUP_RUNS = 3;
    private static int MEASURED_RUNS = 5;

    private static boolean customWarmup = false;
    private static boolean customMeasured = false;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java BenchmarkFilters <filter1> [filter2] ... <inputImage> [threads=...] [useApply=true|false] [warmup=3] [measured=5]");
            return;
        }

        // Parse filter names from arguments
        List<String> filters = new ArrayList<>();
        int i = 0;
        while (i < args.length &&
                !args[i].toLowerCase().endsWith(".png") &&
                !args[i].toLowerCase().endsWith(".jpg") &&
                !args[i].toLowerCase().endsWith(".jpeg") &&
                !args[i].toLowerCase().startsWith("threads=") &&
                !args[i].toLowerCase().startsWith("useapply=") &&
                !args[i].toLowerCase().startsWith("warmup=") &&
                !args[i].toLowerCase().startsWith("measured=")) {
            filters.add(args[i].toLowerCase());
            i++;
        }

        // Ensure at least one filter and image path are provided
        if (filters.isEmpty() || i >= args.length) {
            System.out.println("Error: No filters or image path provided.");
            return;
        }

        // Load input image
        String imagePath = args[i++];
        File inputFile = new File(imagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);

        // Default thread counts and apply mode
        List<Integer> threadCounts = Arrays.asList(1, 2, 8, 16);
        boolean useApply = true;

        // Parse optional parameters
        while (i < args.length) {
            String arg = args[i++].toLowerCase();
            if (arg.startsWith("threads=")) {
                threadCounts = parseThreads(arg.substring(8));
            } else if (arg.startsWith("useapply=")) {
                useApply = Boolean.parseBoolean(arg.substring(9));
            } else if (arg.startsWith("warmup=")) {
                WARMUP_RUNS = Integer.parseInt(arg.substring(7));
                customWarmup = true;
            } else if (arg.startsWith("measured=")) {
                MEASURED_RUNS = Integer.parseInt(arg.substring(9));
                customMeasured = true;
            }
        }

        // Print benchmark configuration
        System.out.printf("Benchmarking %s on image '%s'\n", String.join(", ", filters), inputFile.getName());
        System.out.printf("Testing thread counts: %s\n", threadCounts);
        System.out.printf("Warm-up runs: %d %s\n", WARMUP_RUNS, customWarmup ? "" : "(default)");
        System.out.printf("Measured runs: %d %s\n\n", MEASURED_RUNS, customMeasured ? "" : "(default)");

        // Run benchmarks and collect results
        Map<String, Map<Integer, Long>> timings = new LinkedHashMap<>();
        for (String filter : filters) {
            benchmarkFilter(filter, inputImage, threadCounts, useApply, timings);
        }

        // Print final results summary table
        printSummaryTable(filters, threadCounts, timings);
    }

    /**
     * Benchmarks a single filter across multiple thread counts.
     */
    private static void benchmarkFilter(String filterName, BufferedImage inputImage, List<Integer> threadCounts, boolean useApply, Map<String, Map<Integer, Long>> results) throws InterruptedException {
        System.out.printf("-- %s --\n", filterName);

        // Check whether filter has a custom `apply()` method
        boolean hasCustomApply = overridesApplyMethod(FilterFactory.getFilter(filterName, new HashMap<>()));

        if (useApply && hasCustomApply) {
            System.out.printf("\uD83D\uDFE2 Using %s's custom apply() implementation\n", filterName);
        } else if (useApply) {
            System.out.println("\uD83D\uDFE1 No custom apply() found — using pixel-by-pixel transform");
        } else {
            System.out.println("\uD83D\uDD35 Forcing pixel-by-pixel transform");
        }

        long bestAvg = Long.MAX_VALUE;
        int bestThreads = -1;
        Map<Integer, Long> avgTimes = new HashMap<>();

        // Run benchmark for each thread count
        for (int threads : threadCounts) {
            Map<String, String> options = new HashMap<>();
            options.put("threads", String.valueOf(threads));
            Filter filter = FilterFactory.getFilter(filterName, options);

            // Warm-up runs
            for (int i = 0; i < WARMUP_RUNS; i++) {
                ParallelFilterRunner.run(filter, inputImage, threads, useApply);
            }

            // Run GC and let system settle
            System.gc();
            Thread.sleep(100);

            // Timed runs
            long[] durations = new long[MEASURED_RUNS];
            for (int i = 0; i < MEASURED_RUNS; i++) {
                long start = System.nanoTime();
                ParallelFilterRunner.run(filter, inputImage, threads, useApply);
                durations[i] = (System.nanoTime() - start) / 1_000_000;
            }

            // Calculate stats
            long sum = Arrays.stream(durations).sum();
            double avg = sum / (double) MEASURED_RUNS;
            long min = Arrays.stream(durations).min().orElse(-1);
            long max = Arrays.stream(durations).max().orElse(-1);
            double variance = Arrays.stream(durations)
                    .mapToDouble(d -> Math.pow(d - avg, 2))
                    .sum() / MEASURED_RUNS;
            double stddev = Math.sqrt(variance);

            // Output result
            System.out.printf("Threads: %3d → Avg: %5.1f ms  σ=%.1f  Min: %d ms  Max: %d ms\n", threads, avg, stddev, min, max);
            avgTimes.put(threads, Math.round(avg));

            // Track best performance
            if (avg < bestAvg) {
                bestAvg = Math.round(avg);
                bestThreads = threads;
            }
        }

        // Print best result for filter
        System.out.printf("✅ Best average: %d ms @ %d threads\n\n", bestAvg, bestThreads);
        results.put(filterName, avgTimes);
    }

    /**
     * Parses thread counts from a string: single value, comma-separated list, or range (e.g., "1-4").
     */
    private static List<Integer> parseThreads(String spec) {
        if (spec.contains(",")) {
            List<Integer> list = new ArrayList<>();
            for (String s : spec.split(",")) list.add(Integer.parseInt(s.trim()));
            return list;
        } else if (spec.contains("-")) {
            String[] parts = spec.split("-");
            int min = Integer.parseInt(parts[0]);
            int max = Integer.parseInt(parts[1]);
            List<Integer> list = new ArrayList<>();
            for (int i = min; i <= max; i++) list.add(i);
            return list;
        } else {
            return Collections.singletonList(Integer.parseInt(spec));
        }
    }

    /**
     * Checks whether a filter overrides the default `apply()` method.
     */
    private static boolean overridesApplyMethod(Filter filter) {
        try {
            Method m = filter.getClass().getMethod("apply", BufferedImage.class);
            return !m.getDeclaringClass().equals(Filter.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Prints a summary table of average run times for all filters and thread counts.
     */
    private static void printSummaryTable(List<String> filters, List<Integer> threadCounts, Map<String, Map<Integer, Long>> timings) {
        System.out.println("\n\033[1mBenchmark Summary (Average Times in ms)\033[0m");

        // Determine column widths
        int filterColWidth = Math.max("Filter".length(), filters.stream().mapToInt(String::length).max().orElse(6)) + 2;

        List<String> threadLabels = new ArrayList<>();
        int valueColWidth = 0;
        for (int t : threadCounts) {
            String label = t + " thread" + (t == 1 ? "" : "s");
            threadLabels.add(label);
            valueColWidth = Math.max(valueColWidth, label.length());
        }
        valueColWidth = Math.max(valueColWidth, 9);
        int paddedColWidth = valueColWidth + 1;

        // Header
        System.out.printf("%-" + filterColWidth + "s", "Filter");
        for (String label : threadLabels) {
            System.out.printf("%" + paddedColWidth + "s", label);
        }
        System.out.println();

        // Divider
        System.out.println("=".repeat(filterColWidth + paddedColWidth * threadCounts.size()));

        // Filter rows
        for (String filter : filters) {
            System.out.printf("%-" + filterColWidth + "s", filter);

            Map<Integer, Long> timeMap = timings.get(filter);
            long min = timeMap.values().stream().min(Long::compare).orElse(Long.MAX_VALUE);

            for (int t : threadCounts) {
                Long val = timeMap.get(t);
                String cell;
                if (val == null) {
                    cell = "—";
                } else {
                    cell = val + " ms";
                    if (val == min) {
                        cell += "*";
                    }
                }

                // Highlight best time
                if (cell.endsWith("*")) {
                    System.out.printf("\033[1m\033[32m%" + paddedColWidth + "s\033[0m", cell);
                } else {
                    System.out.printf("%" + paddedColWidth + "s", cell);
                }
            }
            System.out.println();
        }

        // Footer note
        System.out.println("\n\033[2m* = fastest config for that filter\033[0m");

        // Best overall configuration
        String bestFilter = null;
        int bestThreads = -1;
        long bestTime = Long.MAX_VALUE;

        for (String filter : filters) {
            Map<Integer, Long> times = timings.get(filter);
            for (Map.Entry<Integer, Long> entry : times.entrySet()) {
                if (entry.getValue() < bestTime) {
                    bestTime = entry.getValue();
                    bestFilter = filter;
                    bestThreads = entry.getKey();
                }
            }
        }

        if (bestFilter != null) {
            System.out.printf("\n\033[1m🏁 Fastest overall:\033[0m %s @ %d thread%s → %d ms\n",
                    bestFilter,
                    bestThreads,
                    (bestThreads == 1 ? "" : "s"),
                    bestTime);
        }
    }
}
