<div align="center">

# ChromiQ

**Instantly generate beautiful color palettes from any image.**
<br />
A smart, web-based tool for designers, developers, and artists.

<br />

**[View Live Demo](https://chromiq-app.onrender.com/)**

![Java](https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg?style=for-the-badge&logo=spring)
![Docker](https://img.shields.io/badge/Docker-gray.svg?style=for-the-badge&logo=docker)
![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)

</div>

---

### ► Live Demo: [https://chromiq-app.onrender.com/](https://chromiq-app.onrender.com/)

*Note: The application is deployed on Render's free tier and may "spin down" after a period of inactivity. The first visit might have a delay of up to a minute while the service wakes up.*

---

## 🌟 Key Features

| Feature | Description |
| :--- | :--- |
| **🎨 Dominant Color Extraction** | Intelligently identifies the most prominent colors using a k-means clustering algorithm. |
| **🖌️ Interactive Palette** | Add, remove, or replace colors in your palette simply by clicking on the image. |
| **🔄 Multiple Color Formats** | Get color values in HEX, RGB, HSL, and LAB to use in any design tool or codebase. |
| **🌙 Dark Mode** | A sleek, eye-friendly dark mode for a comfortable experience during late-night creative sessions. |
| **📤 Export Options** | Easily export your final palette as CSS variables, a list of HEX codes, or a PNG image. |

---

## ⚙️ How It Works

ChromiQ uses a sophisticated backend pipeline to transform an image into a beautiful and useful color palette:

1.  **Image Processing**: When an image is uploaded, the backend (built with Spring Boot) processes it, ensuring it's in a standard format and resized for optimal performance without sacrificing quality.
2.  **Color Extraction**: The application analyzes the image pixel by pixel, identifying every unique color and its frequency.
3.  **K-Means Clustering**: To make sense of potentially thousands of colors, ChromiQ uses a k-means clustering algorithm. This groups similar colors together, identifying the most statistically significant "dominant" colors.
4.  **Interactive API**: A RESTful API allows the frontend to communicate with the backend to generate the initial palette, and then add, remove, or replace colors based on user interactions.

---

## 🛠️ Tech Stack

* **Backend**: Java 21, Spring Boot
* **Frontend**: HTML5, CSS3, Vanilla JavaScript
* **Deployment**: Docker & Render

---

## 🚀 Getting Started

To run this project locally, you will need:

* Java 21 or higher
* Maven
* Docker (optional, for containerized deployment)

Once you have the prerequisites, follow these steps:

```bash
# 1. Clone the repository
git clone [https://github.com/SaniaMB/ChromiQ.git](https://github.com/SaniaMB/ChromiQ.git)

# 2. Navigate to the project directory
cd ChromiQ

# 3. Build and run the application using Maven
mvn spring-boot:run
```
The application will then be available at http://localhost:8080.

**📄 License**
This project is licensed under the MIT License - see the LICENSE file for details.
