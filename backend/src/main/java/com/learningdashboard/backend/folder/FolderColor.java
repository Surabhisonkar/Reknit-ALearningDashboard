package com.learningdashboard.backend.folder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The fixed folder palette. Stored and sent as its lowercase key ("teal"),
 * never as a hex value, so the frontend decides how each key is drawn (and a
 * future dark theme can map a key to a different shade). The order here is
 * the order {@link LeastUsedFolderColorAssigner} hands colours out in, and
 * matches the V3 migration's backfill.
 */
public enum FolderColor {
    CORAL("coral"),
    YELLOW("yellow"),
    TEAL("teal"),
    SKY("sky"),
    VIOLET("violet"),
    ROSE("rose"),
    GREEN("green"),
    SLATE("slate");

    private final String key;

    FolderColor(String key) {
        this.key = key;
    }

    @JsonValue
    public String key() {
        return key;
    }

    /** Parses a palette key; an unknown value (e.g. a hex colour) is rejected, which the API turns into a 400. */
    @JsonCreator
    public static FolderColor fromJson(String value) {
        for (FolderColor color : values()) {
            if (color.key.equals(value)) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown folder colour: " + value);
    }
}
