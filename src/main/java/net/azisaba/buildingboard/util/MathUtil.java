package net.azisaba.buildingboard.util;

import java.util.Arrays;
import java.util.List;

public class MathUtil {
    public static String toFullPriceString(long l) {
        return formatPrice(l) + " (" + toFriendlyString(l) + ")";
    }

    public static String formatPrice(long l) {
        String preFormatted = String.format("%,d", l);
        if (l >= 1_000_000_000_000_000_000L) {
            return "§5" + preFormatted;
        } else if (l >= 1_000_000_000_000_000L) {
            return "§6" + preFormatted;
        } else if (l >= 1_000_000_000_000L) {
            return "§d" + preFormatted;
        } else if (l >= 1_000_000_000L) {
            return "§b" + preFormatted;
        } else if (l >= 1_000_000L) {
            return "§a" + preFormatted;
        } else {
            return "§f" + preFormatted;
        }
    }

    public static String toFriendlyString(long number) {
        List<String> suffixes = Arrays.asList("", "", "万", "億", "兆", "京");
        double suffixNum = Math.ceil(("" + number).length() / 4.0);
        double shortValue = Math.floor(number / Math.pow(10000.0, suffixNum - 1) * 100) / 100;
        String suffix = suffixes.get((int) suffixNum);
        if (((long) shortValue) == shortValue) {
            return String.format("%,.0f", shortValue) + suffix;
        }
        return shortValue + suffix;
    }

    public static long fromFriendlyString(String s) {
        if (s == null || s.isEmpty()) return 0;
        s = s.replace(" ", "");
        long multiplier = 1;
        if (s.endsWith("万")) {
            multiplier = 10000;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("億")) {
            multiplier = 100000000;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("兆")) {
            multiplier = 1000000000000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("京")) {
            multiplier = 10000000000000000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("k") || s.endsWith("K")) {
            multiplier = 1000;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("m") || s.endsWith("M")) {
            multiplier = 1000000;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("b") || s.endsWith("B")) {
            multiplier = 1000000000;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("t") || s.endsWith("T")) {
            multiplier = 1000000000000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("q")) {
            multiplier = 1000000000000000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("Q")) {
            multiplier = 1000000000000000000L;
            s = s.substring(0, s.length() - 1);
        }
        return (long) (Double.parseDouble(s) * multiplier);
    }
}
