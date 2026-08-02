package com.tsotne.fastarenas.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;

public final class Color {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    private Color() {
    }

    public static String translateColors(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        if (message.indexOf('#') >= 0) {
            Matcher matcher = HEX_PATTERN.matcher(message);
            StringBuilder out = new StringBuilder(message.length() + 32);
            while (matcher.find()) {
                String hex = matcher.group();
                StringBuilder replacement = new StringBuilder(14);
                replacement.append("&x");
                for (int i = 1; i < hex.length(); i++) {
                    replacement.append('&').append(hex.charAt(i));
                }
                matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.toString()));
            }
            matcher.appendTail(out);
            message = out.toString();
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
