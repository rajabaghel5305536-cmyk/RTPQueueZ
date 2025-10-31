package com.rtpqueuez.util;

import org.bukkit.ChatColor;

public class ChatUtil {

    /**
     * Applies color codes to a string.
     * @param message The message to color.
     * @return The colored message.
     */
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Removes color codes from a string.
     * @param message The message to strip.
     * @return The message without color codes.
     */
    public static String stripColor(String message) {
        return ChatColor.stripColor(color(message));
    }

    /**
     * Simple placeholder replacement for messages without PlaceholderAPI.
     */
    public static String replacePlaceholders(String message, String placeholder, String replacement) {
        return message.replace(placeholder, replacement);
    }
}
