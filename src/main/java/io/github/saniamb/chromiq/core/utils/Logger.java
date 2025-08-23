package io.github.saniamb.chromiq.core.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger
 * ------
 * Simple utility class for logging messages with timestamp and level.
 * Can be expanded later for file logging, debug levels, etc.
 */
public final class Logger {

    // Private constructor to prevent instantiation
    private Logger() {}

    // Default date-time format for log messages
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Logs an informational message.
     *
     * @param message The message to log
     */
    public static void info(String message) {
        System.out.println("[INFO] " + timestamp() + " - " + message);
    }

    /**
     * Logs a warning message.
     * @param message The message to log
     */
    public static void warn(String message) {
        System.out.println("[WARN] " + timestamp() + " - " + message);
    }

    /**
     * Logs an error message.
     * @param message The message to log
     */
    public static void error(String message) {
        System.err.println("[ERROR] " + timestamp() + " - " + message);
    }

    /**
     * Generates a timestamp string for logging.
     * @return Current timestamp in "yyyy-MM-dd HH:mm:ss" format
     */
    private static String timestamp() {
        return LocalDateTime.now().format(formatter);
    }
}

