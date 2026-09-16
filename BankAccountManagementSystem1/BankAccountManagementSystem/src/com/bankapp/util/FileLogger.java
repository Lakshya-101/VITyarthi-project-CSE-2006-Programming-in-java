package com.bankapp.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Wraps java.util.logging to write every significant event
 * (logins, transactions, failures, fraud alerts) to a rotating log file.
 * NON-FUNCTIONAL REQUIREMENT: Logging & Monitoring — gives an auditable
 * trail of everything that happened in the system, independent of the
 * console session.
 */
public final class FileLogger {

    private static final Logger LOGGER = Logger.getLogger("BankApplication");
    private static boolean initialized = false;

    private FileLogger() {
        // utility class, no instances
    }

    public static synchronized void init(String logFilePath) {
        if (initialized) {
            return;
        }
        try {
            FileHandler handler = new FileHandler(logFilePath, true); // append mode
            handler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(handler);
            LOGGER.setUseParentHandlers(false); // keep console clean for the menu UI
            initialized = true;
        } catch (IOException e) {
            System.err.println("Warning: could not initialize file logger: " + e.getMessage());
        }
    }

    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
    }

    public static void warn(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, message, t);
    }
}
