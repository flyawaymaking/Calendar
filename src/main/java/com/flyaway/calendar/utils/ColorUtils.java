package com.flyaway.calendar.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    public static String formatColor(String text) {
        if (text == null) return "";

        // Сначала обрабатываем hex цвета формата &#RRGGBB
        Pattern hexPattern = Pattern.compile("&#([0-9a-fA-F]{6})");
        Matcher hexMatcher = hexPattern.matcher(text);
        StringBuilder hexResult = new StringBuilder();

        while (hexMatcher.find()) {
            String hexColor = hexMatcher.group(1);
            // Преобразуем #RRGGBB в §x§R§R§G§G§B§B
            String minecraftHex = "§x" +
                    "§" + hexColor.charAt(0) +
                    "§" + hexColor.charAt(1) +
                    "§" + hexColor.charAt(2) +
                    "§" + hexColor.charAt(3) +
                    "§" + hexColor.charAt(4) +
                    "§" + hexColor.charAt(5);
            hexMatcher.appendReplacement(hexResult, Matcher.quoteReplacement(minecraftHex));
        }
        hexMatcher.appendTail(hexResult);

        // Затем обычные цвета формата &a
        return hexResult.toString().replace('&', '§');
    }

    public static List<String> formatColor(List<String> texts) {
        if (texts == null) return new ArrayList<>();
        List<String> formatted = new ArrayList<>();
        for (String text : texts) {
            formatted.add(formatColor(text));
        }
        return formatted;
    }
}
