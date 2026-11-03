package com.learningdashboard.backend.folder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A folder's colour: one of the fixed palette keys ("teal"), or a custom
 * {@code #rrggbb} the user picked. Stored and sent as that string. Palette
 * keys stay keys, never hex, so the frontend decides how each one is drawn
 * (and a future dark theme can map a key to a different shade); a custom
 * colour is drawn as given, with its tints worked out on the client.
 *
 * <p>{@link #values()} is the palette only, in the order {@link
 * LeastUsedFolderColorAssigner} hands colours out (matching the V3
 * migration's backfill) - custom colours are never assigned automatically.
 */
public final class FolderColor {

    public static final FolderColor CORAL = new FolderColor("coral");
    public static final FolderColor YELLOW = new FolderColor("yellow");
    public static final FolderColor TEAL = new FolderColor("teal");
    public static final FolderColor SKY = new FolderColor("sky");
    public static final FolderColor VIOLET = new FolderColor("violet");
    public static final FolderColor ROSE = new FolderColor("rose");
    public static final FolderColor GREEN = new FolderColor("green");
    public static final FolderColor SLATE = new FolderColor("slate");

    private static final List<FolderColor> PALETTE = List.of(CORAL, YELLOW, TEAL, SKY, VIOLET, ROSE, GREEN, SLATE);
    private static final Pattern CUSTOM = Pattern.compile("#[0-9a-f]{6}");

    private final String key;

    private FolderColor(String key) {
        this.key = key;
    }

    /** The palette, in assignment order. */
    public static List<FolderColor> values() {
        return PALETTE;
    }

    @JsonValue
    public String key() {
        return key;
    }

    public boolean isCustom() {
        return !PALETTE.contains(this);
    }

    /**
     * Parses a palette key or a {@code #rrggbb} colour (any case; stored
     * lowercase). Anything else is rejected, which the API turns into a 400.
     */
    @JsonCreator
    public static FolderColor fromJson(String value) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (FolderColor color : PALETTE) {
                if (color.key.equals(normalized)) {
                    return color;
                }
            }
            if (CUSTOM.matcher(normalized).matches()) {
                return new FolderColor(normalized);
            }
        }
        throw new IllegalArgumentException("Unknown folder colour: " + value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FolderColor color && color.key.equals(key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key;
    }
}
