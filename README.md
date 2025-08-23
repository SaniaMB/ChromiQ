# 🎨 ChromiQ - Color Detection & Palette Generation

A Spring Boot application that extracts colors from images and generates color palettes. Built as a learning project to explore image processing and color analysis.

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## What It Does

- Extract colors from uploaded images
- Convert between RGB, HEX, HSL, and LAB color formats  
- Analyze color distribution and statistics
- Generate color palettes (planned)
- Web interface for easy use (planned)

## Current Status

The basic color extraction pipeline is working:

### What's Complete
- **Image loading**: Handles file uploads and resizing
- **Color extraction**: Finds all unique colors in an image
- **Color model**: Converts between RGB, HEX, HSL, LAB formats
- **Basic testing**: Core components are tested

### How It Works
The basic process is simple:
1. Load an image file
2. Look at each pixel to find its color
3. Count how often each unique color appears
4. Convert colors between different formats (RGB, HEX, etc.)

Currently this works through Java code, but I'm planning to add a web interface.

### Project Structure

| Component | Status | Description |
|-----------|--------|-------------|
| **ImageInputHandler** | ✅ Complete | Memory-safe image loading with auto-resize |
| **ColorExtractor** | ✅ Complete | High-performance pixel analysis engine |
| **ColorEntry** | ✅ Complete | Professional color data model with conversions |
| **Spring Boot Core** | ✅ Complete | Web application foundation |
| **Test Suite** | ✅ Complete | Comprehensive JUnit testing |

---

## Technical Details

### What I Learned Building This
- **Image Processing**: Reading pixels and handling different image formats
- **Color Theory**: Converting between different color spaces (RGB, HSL, LAB)
- **Performance**: Processing large images efficiently without running out of memory
- **Testing**: Writing comprehensive tests for image processing code

### Current Capabilities
- Handles images up to 1920×1080 (auto-resized to prevent memory issues)
- Processes transparency in PNG images
- Finds thousands of unique colors in typical photos
- Fast processing using bulk pixel operations

---

## Getting Started

### Requirements
- Java 17 or higher
- Maven for building

### Setup
```bash
git clone https://github.com/SaniaMB/ChromiQ.git
cd ChromiQ
mvn test  # Run tests to make sure everything works
```

### Try It Out
The project includes automated tests that create sample images and test the color extraction on them. These tests demonstrate how the color detection works with different types of images (solid colors, gradients, multi-colored images).

---

## 🗂️ Project Structure

```
src/main/java/io/github/saniamb/chromiq/
├── core/                    # 🔧 Spring Boot application core
│   ├── ChromiQApplication.java
│   └── config/
├── input/                   # ✅ Image input pipeline  
│   └── ImageInputHandler.java
├── color/                   # ✅ Color processing engine
│   ├── ColorExtractor.java
│   ├── models/
│   │   └── ColorEntry.java
│   └── [ColorQuantizer.java]      # 🔄 Next up
├── palette/                 # 🔄 Palette management
├── api/                     # 🔄 REST API controllers  
├── service/                 # 🔄 Business logic layer
└── utils/                   # 🔧 Helper utilities
```

**Legend:** ✅ Complete | 🔄 In Progress | 🔧 Planned

---

## What's Next

### Immediate Goals
Right now, my color extraction finds *every* unique color in an image - which can be thousands! My next step is making this more useful by:

- **Smart Color Selection**: Instead of showing 50,000 colors, find the 5-10 most important ones
- **Similar Color Grouping**: Merge colors that are almost identical (like #FF0000 and #FF0001)

### Planned Features
Once the core logic is solid, I want to add:

- **Simple Web Interface**: Upload an image, see the main colors
- **Color Palettes**: Generate 3-5 color schemes from any photo
- **Better Formats**: Export colors for CSS, design tools, etc.

### Ideas I'm Considering  
These might happen eventually, depending on how much I learn:

- **Live Camera**: Point your phone camera at something to get its colors
- **Color Names**: Instead of just "#FF0000", show "Bright Red"  
- **Design Tips**: Suggest which colors work well together

### Learning Goals
This project helps me practice:
- Image processing and computer graphics
- Building user-friendly interfaces  
- Working with color theory and design
- Spring Boot web development

---

## Testing

Run the test suite to see what's working:
```bash
mvn test
```

The tests cover:
- Creating and analyzing solid color images
- Multi-colored test images with known color distributions  
- Gradient images with many color variations
- Color format conversions (RGB/HEX/HSL/LAB)
- Edge cases like transparent images

---

## Contributing

This is a learning project, but I'm open to suggestions and contributions! Feel free to:
- Report bugs or issues
- Suggest improvements
- Submit pull requests
- Share ideas for new features

Just keep in mind this is still early in development.

---

## Performance Notes

Some basic benchmarks on my machine:

| Image Size | Unique Colors | Processing Time |
|------------|---------------|----------------|
| 800×600    | ~15,000       | ~50ms         |
| 1920×1080  | ~50,000       | ~120ms        |

Large images are automatically resized to save memory.

---

## 📄 License

This project is licensed under the MIT License - see the (LICENSE) file for details.

---

## 👨‍💻 Author

**Sania M.B.**
- GitHub: [@SaniaMB](https://github.com/SaniaMB)
- Email: saniabhandari05@gmail.com

---

## Acknowledgments

Thanks to various online resources for color theory and Java image processing tutorials that helped me learn these concepts.
