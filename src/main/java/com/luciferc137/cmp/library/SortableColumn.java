package com.luciferc137.cmp.library;

/**
 * Enum representing sortable columns in the music table.
 */
public enum SortableColumn {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("⏲");

    private final String displayName;

    SortableColumn(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

