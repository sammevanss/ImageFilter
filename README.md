# ImageFilter

A multithreaded image processing framework in Java that supports filter pipelines, CLI usage, and high-performance parallelism.

## Features

* Features
* Modular filter design via a common Filter interface
* Centralized FilterFactory for dynamic filter instantiation and registration
* Support for filter pipelines through the FilterPipeline class, enabling composition like ```autocontrast|autobrightness|gaussianblur|edgedetection```
* Parallel image processing using ParallelFilterRunner with customizable thread count
* CLI tool with flexible argument parsing, output naming, and runtime filter construction

## Usage

```
java -cp out imagefilter.Main <filter|list> <input> [output] [key=value ...]
```

## Filters

| Name           | Description                          |
| -------------- | ------------------------------------ |
| grayscale      | Converts to grayscale                |
| invert         | Inverts image colors                 |
| autocontrast   | Adjusts contrast to full range       |
| autobrightness | Normalizes average brightness to 128 |
| edgedetection  | Detects edges using Sobel filters    |
| gaussianblur   | Blurs the image with Gaussian kernel |
| scale          | Resizes image with factor param      |

### Usage Examples

```
# Apply grayscale filter
java -cp out imagefilter.Main grayscale input.jpg output.jpg

# Invert image colors
java -cp out imagefilter.Main invert input.jpg output.jpg

# Auto-adjust contrast
java -cp out imagefilter.Main autocontrast input.jpg output.jpg

# Auto-adjust brightness
java -cp out imagefilter.Main autobrightness input.jpg output.jpg

# Edge detection
java -cp out imagefilter.Main edgedetection input.jpg output.jpg

# Apply Gaussian blur
java -cp out imagefilter.Main gaussianblur input.jpg output.jpg

# Scale image to 50% size
java -cp out imagefilter.Main scale input.jpg output.jpg factor=0.5

# Use 4 threads for parallel processing
java -cp out imagefilter.Main scale input.jpg output.jpg factor=0.5 threads=4

# Compose filters in a pipeline
java -cp out imagefilter.Main grayscale|invert input.jpg output.jpg

# List available filters
java -cp out imagefilter.Main list
```

## Output

The output image is saved as a JPEG. If no output filename is given, it appends the filter name to the input filename.

---

Feel free to extend the framework with your own filters by implementing the `Filter` interface.
