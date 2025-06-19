package imagefilter;

import imagefilter.core.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * CLI entry point for applying filters to images.
 *
 * <p>Usage:
 * <pre>
 *   java Main list
 *   java Main &lt;filter|pipeline&gt; &lt;input&gt; [output] [key=value ...]
 * </pre>
 *
 * <p>Examples:
 * <pre>
 *   java Main grayscale input.jpg output.jpg
 *   java Main grayscale|invert input.jpg
 *   java Main scale input.jpg factor=0.5 threads=4
 * </pre>
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String filterName = args[0].toLowerCase();

        // Handle 'list' command to show available filters
        if (filterName.equals("list")) {
            printAvailableFilters();
            return;
        }

        // Require at least input file for filtering
        if (args.length < 2) {
            printUsage();
            return;
        }

        File inputFile = new File(args[1]);
        if (!inputFile.exists()) {
            System.err.println("Input file not found: " + inputFile.getAbsolutePath());
            return;
        }

        File outputFile;
        if (args.length >= 3 && !args[2].contains("=")) {
            outputFile = new File(args[2]);
        } else {
            outputFile = generateOutputFileName(inputFile, filterName);
        }

        Map<String, String> options = parseOptions(args);
        BufferedImage inputImage = ImageIO.read(inputFile);

        // Create filter or pipeline
        Filter filter = filterName.contains("|")
                ? FilterFactory.createPipeFilter(filterName)
                : FilterFactory.getFilter(filterName, options);

        System.out.println("Using filter: " + filter.name());

        // Determine number of threads to use
        int threads = Runtime.getRuntime().availableProcessors();
        if (options.containsKey("threads")) {
            try {
                int parsed = Integer.parseInt(options.get("threads"));
                if (parsed > 0) {
                    threads = parsed;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid thread count: " + options.get("threads"));
            }
        }

        // Apply filter and time the operation
        long startTime = System.nanoTime();
        BufferedImage outputImage = ParallelFilterRunner.run(filter, inputImage, threads);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        ImageIO.write(outputImage, "jpg", outputFile);

        System.out.println("Saved to: " + outputFile.getAbsolutePath());
        System.out.printf("Filter '%s' completed in %d ms using %d threads%n",
                filter.name(), durationMs, threads);
    }

    /**
     * Parses key=value arguments from the command line into a map.
     *
     * @param args CLI arguments array
     * @return a map of options (e.g., "factor" → "0.5")
     */
    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        int start = (args.length >= 3 && !args[2].contains("=")) ? 3 : 2;
        for (int i = start; i < args.length; i++) {
            if (args[i].contains("=")) {
                String[] parts = args[i].split("=", 2);
                if (parts.length == 2) {
                    options.put(parts[0].toLowerCase(), parts[1]);
                }
            }
        }
        return options;
    }

    /**
     * Generates an output file path based on the input file and filter name.
     * Example: input.jpg + grayscale → input_grayscale.jpg
     *
     * @param inputFile  the input image file
     * @param filterName the applied filter name
     * @return a new File pointing to the suggested output path
     */
    private static File generateOutputFileName(File inputFile, String filterName) {
        String inputName = inputFile.getName();
        int dotIndex = inputName.lastIndexOf('.');
        String base = (dotIndex != -1) ? inputName.substring(0, dotIndex) : inputName;
        String ext = (dotIndex != -1) ? inputName.substring(dotIndex) : ".jpg";
        return new File(inputFile.getParent(), base + "_" + filterName + ext);
    }

    /**
     * Prints the available filters and their descriptions to the console.
     */
    private static void printAvailableFilters() {
        System.out.println("Available filters:");
        Map<String, String> filters = FilterFactory.getFilterRegistry();
        int maxLength = filters.keySet().stream()
                .mapToInt(String::length)
                .max().orElse(20);

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            System.out.printf("  %-" + maxLength + "s - %s%n",
                    entry.getKey(), entry.getValue());
        }
    }

    /**
     * Prints usage instructions for the command-line interface.
     */
    private static void printUsage() {
        System.out.println("Usage: java Main <filter|list> <input> [output] [key=value ...]");
        System.out.println("Examples:");
        System.out.println("  java Main grayscale input.jpg output.jpg");
        System.out.println("  java Main grayscale|invert input.jpg");
        System.out.println("  java Main scale input.jpg factor=0.5 threads=4");
        System.out.println("  java Main list");
    }
}
