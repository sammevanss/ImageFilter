package imagefilter.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the BenchmarkThreadCount utility.
 * This test ensures the benchmark tool runs safely and handles input edge cases.
 */
public class BenchmarkThreadCountTest {

    // Temporary image file used for benchmarking
    private static File tempImage;

    /**
     * Creates a temporary image file before any tests run.
     * This image is used to simulate an input image for benchmarking.
     */
    @BeforeAll
    public static void setup() throws Exception {
        // Create a simple 100x100 dark image
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, 100, 100);
        g.dispose();

        // Save it as a temporary file on disk
        tempImage = File.createTempFile("benchmarkthread", ".jpg");
        ImageIO.write(img, "jpg", tempImage);
    }

    /**
     * Deletes the temporary image file after all tests are complete.
     */
    @AfterAll
    public static void cleanup() throws Exception {
        Files.deleteIfExists(tempImage.toPath());
    }

    /**
     * Verifies that the benchmark runs without throwing errors for a valid case.
     */
    @Test
    public void testBenchmarkRunsWithoutError() {
        String[] args = {
                "invert",                      // Known filter from FilterFactory
                tempImage.getAbsolutePath(),   // Use generated temp image
                "threads=1,2,4",               // Thread count options
                "useApply=true"                // Use custom apply if available
        };

        assertDoesNotThrow(() -> BenchmarkThreadCount.main(args));
    }

    /**
     * Ensures that invalid thread specifications are handled gracefully.
     */
    @Test
    public void testInvalidThreadSpecIsHandled() {
        String[] args = {
                "invert",
                tempImage.getAbsolutePath(),
                "threads=badinput"             // Invalid format
        };

        assertDoesNotThrow(() -> BenchmarkThreadCount.main(args),
                "Invalid thread spec should not throw uncaught exception");
    }

    /**
     * Ensures that the tool does not throw when required arguments are missing.
     */
    @Test
    public void testMissingFilterArgument() {
        String[] args = {}; // No arguments
        assertDoesNotThrow(() -> BenchmarkThreadCount.main(args));
    }

    /**
     * Verifies that using an invalid filter name causes an exception.
     */
    @Test
    public void testInvalidFilterThrows() {
        String[] args = {
                "nonexistentfilter",          // Filter name that doesn't exist
                tempImage.getAbsolutePath()
        };

        Exception ex = assertThrows(Exception.class, () -> BenchmarkThreadCount.main(args));
        assertTrue(ex.getMessage().toLowerCase().contains("nonexistent"));
    }
}
