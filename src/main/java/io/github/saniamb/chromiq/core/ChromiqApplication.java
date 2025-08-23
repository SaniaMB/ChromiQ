package io.github.saniamb.chromiq.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot application class for ChromiQ
 *
 * This is the entry point of your web application that:
 * - Starts an embedded web server (Tomcat) on port 8080
 * - Scans all your packages for components (services, controllers, etc.)
 * - Makes your color processing available via REST APIs
 * - Serves web pages for image upload and camera capture
 */
@SpringBootApplication  // Combines @Configuration, @EnableAutoConfiguration, and @ComponentScan
@ComponentScan(basePackages = "io.github.saniamb.chromiq")  // Tells Spring to look for components in all your packages
public class ChromiqApplication {

    public static void main(String[] args) {
        System.out.println("🎨 Starting ChromiQ Color Processing Web Application...");
        System.out.println("📦 Java version: " + System.getProperty("java.version"));

        // Start Spring Boot application
        SpringApplication.run(ChromiqApplication.class, args);

        System.out.println("✅ ChromiQ is running!");
        System.out.println("🌐 Web interface: http://localhost:8080");
        System.out.println("📡 API endpoints: http://localhost:8080/api/");
    }
}