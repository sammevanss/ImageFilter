package imagefilter.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BenchmarkFilters}. These tests validate expected
 * behavior for valid and invalid argument configurations.
 */
public class BenchmarkFiltersTest {

    // Temporary file to hold the generated test image
    private static File tempImageFile;

    /**
     * Creates a temporary blue image before all tests.
     *
     * @throws Exception if image creation or saving fails
     */
    @BeforeAll
    static void setup() throws Exception {
        // Create a 100x100 solid blue image
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 100, 100);
        g.dispose();

        // Write the image to a temporary file
        tempImageFile = File.createTempFile("benchmark_test", ".jpg");
        ImageIO.write(img, "jpg", tempImageFile);
    }

    /**
     * Deletes the temporary image file after all tests complete.
     *
     * @throws Exception if deletion fails
     */
    @AfterAll
    static void cleanup() throws Exception {
        Files.deleteIfExists(tempImageFile.toPath());
    }

    /**
     * Tests that BenchmarkFilters runs without throwing when provided
     * valid filter and arguments.
     */
    @Test
    public void testBenchmarkFiltersRunsWithoutError() {
        String[] args = {
                "invert",                         // Assumes 'invert' is a valid filter
                tempImageFile.getAbsolutePath(),  // Use temp image file
                "threads=1,2",                    // Use 1 and 2 threads
                "useApply=true",                  // Use filter's apply() method
                "warmup=1",                       // One warmup iteration
                "measured=2"                      // Two measurement iterations
        };

        assertDoesNotThrow(() -> BenchmarkFilters.main(args),
                "BenchmarkFilters.main() should not throw exceptions");
    }

    /**
     * Ensures the program exits cleanly when called without arguments.
     */
    @Test
    public void testMissingArguments() {
        String[] args = {};  // No arguments provided

        assertDoesNotThrow(() -> BenchmarkFilters.main(args),
                "Should handle missing arguments gracefully");
    }

    /**
     * Verifies that an invalid filter name triggers an appropriate exception.
     */
    @Test
    public void testInvalidFilterNameHandled() {
        String[] args = {
                "nonexistentfilter",              // This filter doesn't exist
                tempImageFile.getAbsolutePath()   // Valid image
        };

        Exception ex = assertThrows(Exception.class,
                () -> BenchmarkFilters.main(args),
                "Expected exception when unknown filter is passed");

        assertTrue(ex.getMessage().toLowerCase().contains("nonexistent"),
                "Exception message should mention the nonexistent filter");
    }
}
