package com.cornelius.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String RESET = "\u001B[0m";
    private static final String GOLD = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String PURPLE = "\u001B[35m";

    public static void info(String tag, String message) {
        log("INFO", CYAN, tag, message);
    }

    public static void success(String tag, String message) {
        log("SUCCESS", GREEN, tag, message);
    }

    public static void warn(String tag, String message) {
        log("WARN", GOLD, tag, message);
    }

    public static void error(String tag, String message) {
        log("ERROR", RED, tag, message);
    }

    public static void butler(String message) {
        log("CORNELIUS", PURPLE, "Butler", message);
    }

    private static void log(String level, String color, String tag, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.out.printf("%s[%s] [%s%s%s] [%s]: %s%n",
                RESET, timestamp, color, level, RESET, tag, message);
    }
}

