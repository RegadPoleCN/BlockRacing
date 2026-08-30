package top.lqsnow.blockracing.core;

import java.util.Locale;

public enum TeamColor {
    RED, BLUE;

    public TeamColor opponent() {
        return this == RED ? BLUE : RED;
    }

    /** Parses the legacy "red"/"blue" team strings; null for anything else. */
    public static TeamColor fromName(String name) {
        if (name == null) {
            return null;
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "red" -> RED;
            case "blue" -> BLUE;
            default -> null;
        };
    }
}
