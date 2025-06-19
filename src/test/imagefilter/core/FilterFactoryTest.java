package imagefilter.core;

import imagefilter.filters.FilterPipeline;
import imagefilter.filters.Grayscale;
import imagefilter.filters.Invert;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FilterFactory}.
 * These tests verify correct filter creation, parsing, registry content,
 * option handling, and error conditions.
 */
public class FilterFactoryTest {

    /**
     * Verifies that known filters like "grayscale" and "invert" are created correctly.
     */
    @Test
    public void testCreateKnownFilter() {
        // Test grayscale creation
        assertInstanceOf(
                Grayscale.class,
                FilterFactory.getFilter("grayscale", Collections.emptyMap()),
                "Expected Grayscale filter");

        // Test invert creation
        assertInstanceOf(
                Invert.class,
                FilterFactory.getFilter("invert", Collections.emptyMap()),
                "Expected Invert filter");
    }

    /**
     * Ensures that an invalid filter name triggers an IllegalArgumentException.
     */
    @Test
    public void testCreateUnknownFilter() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FilterFactory.getFilter("nonexistent", Collections.emptyMap()),
                "Unknown filter should throw exception");
    }

    /**
     * Verifies that a filter accepting options (e.g., 'scale') is correctly constructed.
     */
    @Test
    public void testCreateFilterWithOptions() {
        Map<String, String> options = Collections.singletonMap("factor", "0.5");

        Filter filter = FilterFactory.getFilter("scale", options);

        assertNotNull(filter, "Expected non-null filter");
        assertEquals("scale", filter.name(), "Expected filter name to match");
    }

    /**
     * Ensures that a pipeline string produces a {@link FilterPipeline} instance.
     */
    @Test
    public void testCreatePipeFilter() {
        Filter pipeline = FilterFactory.createPipeFilter("grayscale|invert");

        assertInstanceOf(FilterPipeline.class, pipeline, "Expected FilterPipeline instance");
    }

    /**
     * Verifies that unknown options do not break filter instantiation.
     */
    @Test
    public void testUnknownOptionIgnored() {
        Map<String, String> options = Collections.singletonMap("unknown", "value");

        Filter filter = FilterFactory.getFilter("grayscale", options);

        assertNotNull(filter, "Filter should still be created with unknown options");
    }

    /**
     * Ensures that the filter registry contains common filter names.
     */
    @Test
    public void testFilterRegistryContainsKnownFilters() {
        Map<String, String> registry = FilterFactory.getFilterRegistry();

        assertTrue(registry.containsKey("invert"), "Registry should contain 'invert'");
        assertTrue(registry.containsKey("grayscale"), "Registry should contain 'grayscale'");
    }

    /**
     * Confirms that malformed pipeline strings throw exceptions.
     * This simulates a missing closing parenthesis.
     */
    @Test
    public void testPipeSyntaxWithMissingClosingParen() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FilterFactory.createPipeFilter("scale(factor=0.5"),
                "Malformed pipe input should throw exception");
    }

    /**
     * Ensures that filter name parsing is case-insensitive.
     */
    @Test
    public void testCaseInsensitiveFilterName() {
        Filter filter = FilterFactory.getFilter("GrAyScAlE", Collections.emptyMap());

        assertInstanceOf(Grayscale.class, filter, "Filter name matching should be case-insensitive");
    }

    /**
     * Verifies that multiple options in a pipeline string are parsed properly.
     */
    @Test
    public void testMultipleOptionsInPipe() {
        Filter pipeline =
                FilterFactory.createPipeFilter("scale(factor=0.7,method=fast)|invert");

        assertInstanceOf(FilterPipeline.class, pipeline, "Expected pipeline with multiple options");
    }
}
