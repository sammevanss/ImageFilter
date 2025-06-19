package imagefilter.core;

import imagefilter.filters.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * A factory for creating filters by name with optional parameters,
 * and composing them into filter pipelines using '|' syntax.
 */
public class FilterFactory {

    /**
     * Holds construction logic and description for a filter.
     *
     * @param constructor Factory function from options to a Filter instance.
     * @param usage       Human-readable usage string for CLI help.
     */
    private record FilterInfo(
            Function<Map<String, String>, Filter> constructor, String usage)
    {}


    /**
     * Registry of all available filters mapped by lowercase name.
     * TreeMap is used to keep filters sorted alphabetically.
     */
    private static final Map<String, FilterInfo> registry = new TreeMap<>();

    // Static block to register all known filters.
    static {
        // Simple stateless filters
        register("autocontrast", _ -> new AutoContrast(), "Auto contrast adjustment");
        register("autobrightness", _ -> new AutoBrightness(), "Auto brightness adjustment");
        register("invert", _ -> new Invert(), "Inverts image colors");
        register("grayscale", _ -> new Grayscale(), "Converts to grayscale");
        register("edgedetection", _ -> new EdgeDetection(), "Detects edges");

        // Blur filter
        register("gaussianblur", _ -> new GaussianBlur(), "Applies Gaussian blur");

        // Filters with required parameters
        register("scale", Scale::fromOptions, "Scales the image. Usage: scale(factor=0.5)");
    }

    /**
     * Registers a new filter under a lowercase name.
     *
     * @param name        unique lowercase name
     * @param constructor factory function from options to Filter instance
     * @param usage       human-readable usage string
     */
    private static void register(
            String name, Function<Map<String, String>, Filter> constructor, String usage) {
        registry.put(name.toLowerCase(), new FilterInfo(constructor, usage));
    }

    /**
     * Constructs a filter from a registered name and optional parameters.
     *
     * @param filterName the filter name (case-insensitive)
     * @param options    optional key=value arguments
     * @return Filter instance
     * @throws IllegalArgumentException if the filter is unknown
     */
    public static Filter getFilter(String filterName, Map<String, String> options) {
        FilterInfo info = registry.get(filterName.toLowerCase());
        if (info == null) {
            // Inform user of available filters if an unknown one is requested
            throw new IllegalArgumentException(
                    "Unknown filter: " + filterName + ". Available: " + String.join(", ", registry.keySet()));
        }
        return info.constructor.apply(options);
    }

    /**
     * Creates a pipeline of filters from a pipe-delimited string.
     *
     * @param pipeFilterStr e.g., "grayscale|scale(factor=0.5)|invert"
     * @return composed FilterPipeline
     */
    public static Filter createPipeFilter(String pipeFilterStr) {
        List<Filter> filters = new ArrayList<>();
        for (String part : splitPipeline(pipeFilterStr)) {
            filters.add(parseFilterWithOptions(part.trim()));
        }
        return new FilterPipeline(filters);
    }

    /**
     * Parses a single filter specification with optional parameters.
     *
     * @param filterWithOpts e.g., "scale(factor=0.5)"
     * @return Filter instance
     */
    public static Filter parseFilterWithOptions(String filterWithOpts) {
        String name;
        Map<String, String> options = new HashMap<>();

        int startOpts = filterWithOpts.indexOf('(');
        int endOpts = filterWithOpts.lastIndexOf(')');

        if (startOpts != -1 && endOpts != -1 && endOpts > startOpts) {
            // Extract name and option key=value pairs
            name = filterWithOpts.substring(0, startOpts).trim().toLowerCase();
            String optsStr = filterWithOpts.substring(startOpts + 1, endOpts);
            String[] pairs = optsStr.split(",");
            for (String pair : pairs) {
                String[] kv = pair.trim().split("=", 2);
                if (kv.length == 2) {
                    options.put(kv[0].trim().toLowerCase(), kv[1].trim());
                }
            }
        } else {
            // No options present, assume plain filter name
            name = filterWithOpts.trim().toLowerCase();
        }

        return getFilter(name, options);
    }

    /**
     * Splits a pipeline string by '|' while ignoring splits inside parentheses.
     *
     * @param pipeStr the raw pipeline string
     * @return list of individual filter specs
     */
    private static List<String> splitPipeline(String pipeStr) {
        List<String> parts = new ArrayList<>();
        int depth = 0; // Tracks open parenthesis depth
        int start = 0; // Start index of current filter segment

        for (int i = 0; i < pipeStr.length(); i++) {
            char c = pipeStr.charAt(i);
            if (c == '(') {
                depth++; // Entering options
            } else if (c == ')') {
                depth = Math.max(0, depth - 1); // Leaving options
            } else if (c == '|' && depth == 0) {
                // Split only if we're not inside parentheses
                parts.add(pipeStr.substring(start, i));
                start = i + 1;
            }
        }

        // Add the final segment
        parts.add(pipeStr.substring(start));
        return parts;
    }

    /**
     * Returns a mapping of all available filters to their usage strings.
     *
     * @return map of filter name to usage description
     */
    public static Map<String, String> getFilterRegistry() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, FilterInfo> entry : registry.entrySet()) {
            map.put(entry.getKey(), entry.getValue().usage);
        }
        return map;
    }
}
